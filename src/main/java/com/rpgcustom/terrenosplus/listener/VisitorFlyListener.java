package com.rpgcustom.terrenosplus.listener;

import com.rpgcustom.terrenosplus.model.Terreno;
import com.rpgcustom.terrenosplus.service.TerrenoManager;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VisitorFlyListener implements Listener {

    private final TerrenoManager manager;
    private final Map<UUID, FlyState> suppressed = new ConcurrentHashMap<>();

    public VisitorFlyListener(TerrenoManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location to = event.getTo();
        if (to == null) return;

        Location from = event.getFrom();
        if (sameBlock(from, to)) return;

        apply(event.getPlayer(), to);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getTo() != null) {
            apply(event.getPlayer(), event.getTo());
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        apply(event.getPlayer(), event.getPlayer().getLocation());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        suppressed.remove(event.getPlayer().getUniqueId());
    }

    public void refreshTerrain(Terreno terrain) {
        if (terrain == null) return;
        for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            if (!terrain.contains(player.getLocation())) continue;
            apply(player, player.getLocation());
        }
    }

    private void apply(Player player, Location location) {
        Optional<Terreno> current = manager.find(location);
        if (current.isEmpty()) {
            restore(player);
            return;
        }

        Terreno terrain = current.get();
        if (terrain.visitorFlyEnabled()
                || terrain.ownerId().equals(player.getUniqueId())
                || isStaff(player)
                || player.getGameMode() == GameMode.CREATIVE
                || player.getGameMode() == GameMode.SPECTATOR) {
            restore(player);
            return;
        }

        if (!player.getAllowFlight() && !player.isFlying()) {
            return;
        }

        boolean firstSuppression = !suppressed.containsKey(player.getUniqueId());
        suppressed.computeIfAbsent(
                player.getUniqueId(),
                ignored -> new FlyState(player.getAllowFlight(), player.isFlying())
        );

        if (player.isFlying()) {
            player.setFlying(false);
        }
        player.setAllowFlight(false);

        if (firstSuppression) {
            player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes(
                    '&',
                    "&c[Voo] Modo de voo desativado nesse terreno."
            ));
        }
    }

    private void restore(Player player) {
        FlyState state = suppressed.remove(player.getUniqueId());
        if (state == null) return;

        player.setAllowFlight(state.allowFlight());
        if (state.flying() && state.allowFlight()) {
            player.setFlying(true);
        }
    }

    private boolean isStaff(Player player) {
        return player.hasPermission("terrenosplus.admin")
                || player.hasPermission("terrenosplus.bypass");
    }

    private boolean sameBlock(Location a, Location b) {
        return a.getWorld() == b.getWorld()
                && a.getBlockX() == b.getBlockX()
                && a.getBlockY() == b.getBlockY()
                && a.getBlockZ() == b.getBlockZ();
    }

    private record FlyState(boolean allowFlight, boolean flying) {
    }
}
