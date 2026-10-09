package com.rpgcustom.terrenosplus.listener;

import com.rpgcustom.terrenosplus.TerrenosPlus;
import com.rpgcustom.terrenosplus.model.Terreno;
import com.rpgcustom.terrenosplus.service.TerrenoManager;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ClaimToolListener implements Listener {

    private final TerrenosPlus plugin;
    private final TerrenoManager manager;
    private final Map<UUID, Location> firstCorners = new HashMap<>();
    private final Map<UUID, BlockData> firstCornerOriginal = new HashMap<>();
    private final Map<UUID, org.bukkit.scheduler.BukkitTask> selectionTasks = new HashMap<>();

    public ClaimToolListener(TerrenosPlus plugin, TerrenoManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onShovelUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        if (event.getItem() == null || event.getItem().getType() != toolMaterial()) return;

        Player player = event.getPlayer();
        if (!player.hasPermission("terrenosplus.use")) return;

        event.setCancelled(true);
        Block clicked = event.getClickedBlock();
        Location location = clicked.getLocation();

        if (!isEnabledWorld(clicked.getWorld().getName())) {
            plugin.send(player, "messages.disabled-world");
            return;
        }

        UUID playerId = player.getUniqueId();
        Location first = firstCorners.remove(playerId);
        if (first == null) {
            firstCorners.put(playerId, location);
            firstCornerOriginal.put(playerId, clicked.getBlockData().clone());
            plugin.send(player, "messages.first-corner",
                    "{x}", String.valueOf(location.getBlockX()),
                    "{z}", String.valueOf(location.getBlockZ()));
            startSelectionPreview(player, location);
            return;
        }

        BlockData firstOriginal = firstCornerOriginal.remove(playerId);
        BlockData secondOriginal = clicked.getBlockData().clone();
        stopSelectionPreview(playerId);
        showSelectionCorner(player, first);
        showSelectionCorner(player, location);

        if (!first.getWorld().equals(location.getWorld())) {
            restoreVirtualBlock(player, first, firstOriginal);
            restoreVirtualBlock(player, location, secondOriginal);
            plugin.send(player, "messages.different-world");
            return;
        }

        TerrenoManager.CreateResult result = manager.create(player, first, location);
        switch (result.type()) {
            case SUCCESS -> {
                Terreno terreno = result.terreno();
                plugin.send(player, "messages.created", "{area}", String.valueOf(terreno.area()));
                showCreatedSubtitle(player);

                // Durante os primeiros 5 segundos, os dois pontos escolhidos
                // permanecem em esmeralda e o contorno usa partículas verdes.
                for (int i = 0; i < 10; i++) {
                    plugin.getServer().getScheduler().runTaskLater(
                            plugin,
                            () -> {
                                if (!player.isOnline()) return;
                                showSelectionCorner(player, first);
                                showSelectionCorner(player, location);
                                showGreenBoundary(player, terreno);
                            },
                            i * 10L
                    );
                }

                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (!player.isOnline()) return;

                    restoreVirtualBlock(player, first, firstOriginal);
                    restoreVirtualBlock(player, location, secondOriginal);

                    showGoldClaimMarkers(player, terreno);
                    showYellowBoundary(player, terreno);
                    plugin.send(player, "messages.configure-after-create");

                    // Depois de mais 15 segundos, restaura os blocos reais.
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        if (!player.isOnline()) return;
                        restoreTerrainCorners(player, terreno);
                    }, 15L * 20L);
                }, 5L * 20L);
            }
            case DIFFERENT_WORLD -> {
                restoreVirtualBlock(player, first, firstOriginal);
                restoreVirtualBlock(player, location, secondOriginal);
                plugin.send(player, "messages.different-world");
            }
            case TOO_SMALL -> {
                restoreVirtualBlock(player, first, firstOriginal);
                restoreVirtualBlock(player, location, secondOriginal);
                plugin.send(player, "messages.too-small",
                        "{min}", String.valueOf(result.value()));
            }
            case TOO_LARGE -> {
                restoreVirtualBlock(player, first, firstOriginal);
                restoreVirtualBlock(player, location, secondOriginal);
                plugin.send(player, "messages.too-large",
                        "{max}", String.valueOf(result.value()));
            }
            case OVERLAP -> {
                restoreVirtualBlock(player, first, firstOriginal);
                restoreVirtualBlock(player, location, secondOriginal);
                plugin.send(player, "messages.overlap",
                        "{owner}", result.overlap().ownerName());
            }
            case LIMIT -> {
                restoreVirtualBlock(player, first, firstOriginal);
                restoreVirtualBlock(player, location, secondOriginal);
                plugin.send(player, "messages.claim-limit",
                        "{max}", String.valueOf(result.value()));
            }
        }
    }

    private void showCreatedSubtitle(Player player) {
        String subtitle = plugin.getConfig().getString(
                "messages.created-subtitle",
                "&aTerreno criado com sucesso!"
        );
        player.sendTitle(
                "",
                org.bukkit.ChatColor.translateAlternateColorCodes('&', subtitle == null ? "" : subtitle),
                10,
                50,
                10
        );
    }

    private Material toolMaterial() {
        Material configured = Material.matchMaterial(plugin.getConfig().getString("tool.material", "GOLDEN_SHOVEL"));
        return configured == null ? Material.GOLDEN_SHOVEL : configured;
    }

    private boolean isEnabledWorld(String worldName) {
        return plugin.getConfig().getStringList("claims.enabled-worlds").contains(worldName);
    }

    private void startSelectionPreview(Player player, Location location) {
        stopSelectionPreview(player.getUniqueId());

        // Envia no tick seguinte e repete enquanto o segundo canto ainda não foi
        // escolhido. Isso evita que um update normal do chunk sobrescreva o
        // bloco virtual imediatamente após o clique.
        org.bukkit.scheduler.BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(
                plugin,
                () -> {
                    if (!player.isOnline() || !firstCorners.containsKey(player.getUniqueId())) {
                        stopSelectionPreview(player.getUniqueId());
                        return;
                    }
                    showSelectionCorner(player, location);
                },
                1L,
                10L
        );
        selectionTasks.put(player.getUniqueId(), task);
    }

    private void stopSelectionPreview(UUID playerId) {
        org.bukkit.scheduler.BukkitTask task = selectionTasks.remove(playerId);
        if (task != null) task.cancel();
    }

    private void showSelectionCorner(Player player, Location location) {
        if (location == null || location.getWorld() == null) return;
        player.sendBlockChange(location, Material.EMERALD_BLOCK.createBlockData());
        Location center = location.clone().add(0.5, 1.1, 0.5);
        player.spawnParticle(Particle.HAPPY_VILLAGER, center, 20, 0.35, 0.25, 0.35, 0.0);
    }

    private void restoreVirtualBlock(Player player, Location location, BlockData original) {
        if (location == null || location.getWorld() == null) return;
        BlockData data = original != null ? original : location.getBlock().getBlockData();
        player.sendBlockChange(location, data);
    }

    private void showGreenBoundary(Player player, Terreno terreno) {
        showBoundary(player, terreno, Particle.HAPPY_VILLAGER);
    }

    private void showYellowBoundary(Player player, Terreno terreno) {
        // Redesenha por alguns segundos para que o efeito amarelo permaneça visível.
        for (int i = 0; i < 15; i++) {
            plugin.getServer().getScheduler().runTaskLater(
                    plugin,
                    () -> {
                        if (player.isOnline()) showYellowDustBoundary(player, terreno);
                    },
                    i * 20L
            );
        }
    }

    private void showYellowDustBoundary(Player player, Terreno terreno) {
        var world = plugin.getServer().getWorld(terreno.world());
        if (world == null) return;

        int y = player.getLocation().getBlockY() + 1;
        int step = Math.max(1, Math.min(4, Math.max(terreno.width(), terreno.depth()) / 20));
        Particle.DustOptions yellow = new Particle.DustOptions(Color.YELLOW, 1.15f);

        for (int x = terreno.minX(); x <= terreno.maxX(); x += step) {
            yellowParticle(player, new Location(world, x + 0.5, y, terreno.minZ() + 0.5), yellow);
            yellowParticle(player, new Location(world, x + 0.5, y, terreno.maxZ() + 0.5), yellow);
        }
        for (int z = terreno.minZ(); z <= terreno.maxZ(); z += step) {
            yellowParticle(player, new Location(world, terreno.minX() + 0.5, y, z + 0.5), yellow);
            yellowParticle(player, new Location(world, terreno.maxX() + 0.5, y, z + 0.5), yellow);
        }
    }

    private void yellowParticle(Player player, Location location, Particle.DustOptions yellow) {
        player.spawnParticle(
                Particle.DUST,
                location,
                2,
                0.05, 0.05, 0.05,
                0.0,
                yellow
        );
    }

    private void showGoldClaimMarkers(Player player, Terreno terreno) {
        for (Location corner : terrainCorners(terreno)) {
            player.sendBlockChange(corner, Material.GOLD_BLOCK.createBlockData());
        }
    }

    private void restoreTerrainCorners(Player player, Terreno terreno) {
        for (Location corner : terrainCorners(terreno)) {
            player.sendBlockChange(corner, corner.getBlock().getBlockData());
        }
    }

    private List<Location> terrainCorners(Terreno terreno) {
        var world = plugin.getServer().getWorld(terreno.world());
        if (world == null) return List.of();

        return List.of(
                groundCorner(world, terreno.minX(), terreno.minZ()),
                groundCorner(world, terreno.minX(), terreno.maxZ()),
                groundCorner(world, terreno.maxX(), terreno.minZ()),
                groundCorner(world, terreno.maxX(), terreno.maxZ())
        );
    }

    private Location groundCorner(org.bukkit.World world, int x, int z) {
        int y = world.getHighestBlockYAt(x, z);
        return new Location(world, x, y, z);
    }

    private void showBoundary(Player player, Terreno terreno, Particle particleType) {
        var world = plugin.getServer().getWorld(terreno.world());
        if (world == null) return;

        int y = player.getLocation().getBlockY() + 1;
        int step = Math.max(1, Math.min(4, Math.max(terreno.width(), terreno.depth()) / 20));

        for (int x = terreno.minX(); x <= terreno.maxX(); x += step) {
            particle(player, world.getName(), x, y, terreno.minZ(), particleType);
            particle(player, world.getName(), x, y, terreno.maxZ(), particleType);
        }
        for (int z = terreno.minZ(); z <= terreno.maxZ(); z += step) {
            particle(player, world.getName(), terreno.minX(), y, z, particleType);
            particle(player, world.getName(), terreno.maxX(), y, z, particleType);
        }
    }

    private void particle(Player player, String worldName, int x, int y, int z, Particle particleType) {
        var world = plugin.getServer().getWorld(worldName);
        if (world == null) return;
        player.spawnParticle(particleType,
                new Location(world, x + 0.5, y, z + 0.5),
                2, 0.05, 0.05, 0.05, 0.0);
    }
}
