package com.rpgcustom.terrenosplus.listener;

import com.rpgcustom.terrenosplus.model.Terreno;
import com.rpgcustom.terrenosplus.service.TerrenoManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Optional;
import java.util.UUID;

public final class TerrainEnterListener implements Listener {

    private final TerrenoManager manager;

    public TerrainEnterListener(TerrenoManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null || sameBlock(from, to)) return;

        Optional<Terreno> previous = manager.find(from);
        Optional<Terreno> current = manager.find(to);
        if (current.isEmpty()) return;

        UUID previousId = previous.map(Terreno::id).orElse(null);
        Terreno terrain = current.get();

        // Só mostra ao realmente entrar em outro terreno; caminhar dentro dele
        // não repete o title.
        if (terrain.id().equals(previousId)) return;

        showTitle(event.getPlayer(), terrain.ownerName());
    }

    private void showTitle(Player player, String owner) {
        player.sendTitle(
                color("&eTerreno de"),
                color("&f" + owner),
                10,
                50,
                10
        );
    }

    private boolean sameBlock(Location a, Location b) {
        return a.getWorld() == b.getWorld()
                && a.getBlockX() == b.getBlockX()
                && a.getBlockY() == b.getBlockY()
                && a.getBlockZ() == b.getBlockZ();
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }
}
