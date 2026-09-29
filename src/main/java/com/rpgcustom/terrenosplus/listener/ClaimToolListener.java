package com.rpgcustom.terrenosplus.listener;

import com.rpgcustom.terrenosplus.TerrenosPlus;
import com.rpgcustom.terrenosplus.model.Terreno;
import com.rpgcustom.terrenosplus.service.TerrenoManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ClaimToolListener implements Listener {

    private final TerrenosPlus plugin;
    private final TerrenoManager manager;
    private final Map<UUID, Location> firstCorners = new HashMap<>();

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

        Location first = firstCorners.remove(player.getUniqueId());
        if (first == null) {
            firstCorners.put(player.getUniqueId(), location);
            plugin.send(player, "messages.first-corner",
                    "{x}", String.valueOf(location.getBlockX()),
                    "{z}", String.valueOf(location.getBlockZ()));
            showCorner(player, location);
            return;
        }

        if (!first.getWorld().equals(location.getWorld())) {
            plugin.send(player, "messages.different-world");
            return;
        }

        TerrenoManager.CreateResult result = manager.create(player, first, location);
        switch (result.type()) {
            case SUCCESS -> {
                Terreno terreno = result.terreno();
                plugin.send(player, "messages.created", "{area}", String.valueOf(terreno.area()));
                showBoundary(player, terreno);
            }
            case DIFFERENT_WORLD -> plugin.send(player, "messages.different-world");
            case TOO_SMALL -> plugin.send(player, "messages.too-small",
                    "{min}", String.valueOf(result.value()));
            case TOO_LARGE -> plugin.send(player, "messages.too-large",
                    "{max}", String.valueOf(result.value()));
            case OVERLAP -> plugin.send(player, "messages.overlap",
                    "{owner}", result.overlap().ownerName());
            case LIMIT -> plugin.send(player, "messages.claim-limit",
                    "{max}", String.valueOf(result.value()));
        }
    }

    private Material toolMaterial() {
        Material configured = Material.matchMaterial(plugin.getConfig().getString("tool.material", "GOLDEN_SHOVEL"));
        return configured == null ? Material.GOLDEN_SHOVEL : configured;
    }

    private boolean isEnabledWorld(String worldName) {
        return plugin.getConfig().getStringList("claims.enabled-worlds").contains(worldName);
    }

    private void showCorner(Player player, Location location) {
        Location center = location.clone().add(0.5, 1.1, 0.5);
        player.spawnParticle(Particle.HAPPY_VILLAGER, center, 20, 0.35, 0.25, 0.35, 0.0);
    }

    private void showBoundary(Player player, Terreno terreno) {
        var world = plugin.getServer().getWorld(terreno.world());
        if (world == null) return;

        int y = player.getLocation().getBlockY() + 1;
        int step = Math.max(1, Math.min(4, Math.max(terreno.width(), terreno.depth()) / 20));

        for (int x = terreno.minX(); x <= terreno.maxX(); x += step) {
            particle(player, world.getName(), x, y, terreno.minZ());
            particle(player, world.getName(), x, y, terreno.maxZ());
        }
        for (int z = terreno.minZ(); z <= terreno.maxZ(); z += step) {
            particle(player, world.getName(), terreno.minX(), y, z);
            particle(player, world.getName(), terreno.maxX(), y, z);
        }
    }

    private void particle(Player player, String worldName, int x, int y, int z) {
        var world = plugin.getServer().getWorld(worldName);
        if (world == null) return;
        player.spawnParticle(Particle.HAPPY_VILLAGER,
                new Location(world, x + 0.5, y, z + 0.5),
                2, 0.05, 0.05, 0.05, 0.0);
    }
}
