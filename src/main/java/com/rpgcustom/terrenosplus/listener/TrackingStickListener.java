package com.rpgcustom.terrenosplus.listener;

import com.rpgcustom.terrenosplus.TerrenosPlus;
import com.rpgcustom.terrenosplus.model.Terreno;
import com.rpgcustom.terrenosplus.service.TerrenoManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TrackingStickListener implements Listener {

    private static final NamespacedKey TERRAIN_TOOL_KEY = new NamespacedKey("terrenosplus", "tool");

    private final TerrenosPlus plugin;
    private final TerrenoManager manager;
    private final Map<UUID, MarkerState> activeMarkers = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> trackedTerrains = new ConcurrentHashMap<>();

    public TrackingStickListener(TerrenosPlus plugin, TerrenoManager manager) {
        this.plugin = plugin;
        this.manager = manager;

        // Enquanto o jogador estiver segurando o palito, redesenha os limites
        // periodicamente. Ao trocar/largar o item, nenhum novo efeito é enviado.
        plugin.getServer().getScheduler().runTaskTimer(
                plugin,
                this::showHeldTrackerBoundaries,
                1L,
                10L
        );
    }

    private void showHeldTrackerBoundaries() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!isHoldingTrackingStick(player)) {
                trackedTerrains.remove(player.getUniqueId());
                restoreCornerMarkers(player);
                continue;
            }

            UUID trackedId = trackedTerrains.get(player.getUniqueId());
            if (trackedId == null) {
                manager.find(player.getLocation()).ifPresent(terrain -> {
                    if (terrain.ownerId().equals(player.getUniqueId())
                            || player.hasPermission("terrenosplus.admin")) {
                        trackedTerrains.put(player.getUniqueId(), terrain.id());
                    }
                });
                trackedId = trackedTerrains.get(player.getUniqueId());
            }

            if (trackedId == null) {
                restoreCornerMarkers(player);
                continue;
            }

            var terrain = manager.getById(trackedId);
            if (terrain.isEmpty()) {
                trackedTerrains.remove(player.getUniqueId());
                restoreCornerMarkers(player);
                continue;
            }

            Terreno current = terrain.get();
            showBoundary(player, current);
            showCornerMarkers(player, current);
        }
    }

    private boolean isHoldingTrackingStick(Player player) {
        return isTrackingStick(player.getInventory().getItemInMainHand())
                || isTrackingStick(player.getInventory().getItemInOffHand());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        trackedTerrains.remove(uuid);
        activeMarkers.remove(uuid);
    }

    @EventHandler(ignoreCancelled = true)
    public void onUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        ItemStack item = event.getItem();
        if (!isTrackingStick(item)) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        var terrain = manager.find(event.getClickedBlock().getLocation());

        if (terrain.isEmpty()) {
            trackedTerrains.remove(player.getUniqueId());
            restoreCornerMarkers(player);
            plugin.send(player, "messages.tracker-unprotected");
            return;
        }

        Terreno t = terrain.get();
        trackedTerrains.put(player.getUniqueId(), t.id());
        plugin.send(player, "messages.tracker-info",
                "{owner}", t.ownerName(),
                "{area}", String.valueOf(t.area()),
                "{width}", String.valueOf(t.width()),
                "{depth}", String.valueOf(t.depth()));

        if (t.ownerId().equals(player.getUniqueId()) || player.hasPermission("terrenosplus.admin")) {
            showBoundary(player, t);
            showCornerMarkers(player, t);
        }
    }

    private void showCornerMarkers(Player player, Terreno terrain) {
        var world = plugin.getServer().getWorld(terrain.world());
        if (world == null) {
            restoreCornerMarkers(player);
            return;
        }

        List<Location> corners = List.of(
                groundCorner(world, terrain.minX(), terrain.minZ()),
                groundCorner(world, terrain.minX(), terrain.maxZ()),
                groundCorner(world, terrain.maxX(), terrain.minZ()),
                groundCorner(world, terrain.maxX(), terrain.maxZ())
        );

        MarkerState previous = activeMarkers.get(player.getUniqueId());
        if (previous != null && !previous.terrainId().equals(terrain.id())) {
            restoreCornerMarkers(player);
        }

        var gold = Material.GOLD_BLOCK.createBlockData();
        for (Location corner : corners) {
            player.sendBlockChange(corner, gold);
        }

        activeMarkers.put(player.getUniqueId(), new MarkerState(terrain.id(), corners));
    }

    private Location groundCorner(org.bukkit.World world, int x, int z) {
        int y = world.getHighestBlockYAt(x, z);
        return new Location(world, x, y, z);
    }

    private void restoreCornerMarkers(Player player) {
        MarkerState state = activeMarkers.remove(player.getUniqueId());
        if (state == null) return;

        for (Location location : state.locations()) {
            if (location.getWorld() == null) continue;
            player.sendBlockChange(location, location.getBlock().getBlockData());
        }
    }

    private boolean isTrackingStick(ItemStack item) {
        if (item == null || item.getType() != Material.STICK || !item.hasItemMeta()) return false;
        String type = item.getItemMeta().getPersistentDataContainer()
                .get(TERRAIN_TOOL_KEY, PersistentDataType.STRING);
        return "inspect".equals(type);
    }

    private void showBoundary(Player player, Terreno terrain) {
        var world = plugin.getServer().getWorld(terrain.world());
        if (world == null) return;

        int y = player.getLocation().getBlockY() + 1;
        int step = Math.max(1, Math.min(4, Math.max(terrain.width(), terrain.depth()) / 20));

        for (int x = terrain.minX(); x <= terrain.maxX(); x += step) {
            particle(player, new Location(world, x + 0.5, y, terrain.minZ() + 0.5));
            particle(player, new Location(world, x + 0.5, y, terrain.maxZ() + 0.5));
        }
        for (int z = terrain.minZ(); z <= terrain.maxZ(); z += step) {
            particle(player, new Location(world, terrain.minX() + 0.5, y, z + 0.5));
            particle(player, new Location(world, terrain.maxX() + 0.5, y, z + 0.5));
        }
    }

    private void particle(Player player, Location location) {
        player.spawnParticle(Particle.WAX_ON, location, 2, 0.05, 0.05, 0.05, 0.0);
    }

    private record MarkerState(UUID terrainId, List<Location> locations) {
    }
}
