package com.rpgcustom.terrenosplus.command;

import com.rpgcustom.terrenosplus.TerrenosPlus;
import com.rpgcustom.terrenosplus.model.Terreno;
import com.rpgcustom.terrenosplus.gui.TerrenosGUI;
import com.rpgcustom.terrenosplus.gui.TerrenosListGUI;
import com.rpgcustom.terrenosplus.listener.TrackingStickListener;
import com.rpgcustom.terrenosplus.service.TerrenoManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public final class TerrenoCommand implements CommandExecutor {

    private static final DateTimeFormatter CREATION_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.systemDefault());

    private final TerrenosPlus plugin;
    private final TerrenoManager manager;
    private final TrackingStickListener trackingStickListener;

    public TerrenoCommand(TerrenosPlus plugin, TerrenoManager manager,
                          TrackingStickListener trackingStickListener) {
        this.plugin = plugin;
        this.manager = manager;
        this.trackingStickListener = trackingStickListener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Comando disponível apenas para jogadores.");
            return true;
        }

        if (args.length == 0) {
            Optional<Terreno> current = manager.find(player.getLocation());
            if (current.isEmpty() || !current.get().ownerId().equals(player.getUniqueId())) {
                plugin.send(player, "messages.command-own-claim-only");
                return true;
            }

            player.openInventory(TerrenosGUI.main(player, manager));
            return true;
        }

        if (args[0].equalsIgnoreCase("info")) {
            Optional<Terreno> terreno = manager.find(player.getLocation());
            if (terreno.isEmpty()) {
                plugin.send(player, "messages.no-claim");
                return true;
            }

            Terreno t = terreno.get();
            String createdAt = t.createdAt() > 0L
                    ? CREATION_DATE_FORMAT.format(Instant.ofEpochMilli(t.createdAt()))
                    : "Desconhecida";

            plugin.send(player, "messages.info-owner",
                    "{owner}", t.ownerName(),
                    "{area}", String.valueOf(t.area()),
                    "{created}", createdAt,
                    "{width}", String.valueOf(t.width()),
                    "{depth}", String.valueOf(t.depth()),
                    "{world}", t.world(),
                    "{id}", t.id().toString().substring(0, 8));
            return true;
        }

        if (args[0].equalsIgnoreCase("remover")) {
            Optional<Terreno> terreno = manager.find(player.getLocation());
            if (terreno.isEmpty()) {
                plugin.send(player, "messages.no-claim");
                return true;
            }

            Terreno t = terreno.get();
            if (!t.ownerId().equals(player.getUniqueId()) && !player.hasPermission("terrenosplus.admin")) {
                plugin.send(player, "messages.not-owner");
                return true;
            }

            manager.remove(t);
            trackingStickListener.onTerrainRemoved(player, t);
            plugin.send(player, "messages.removed");
            return true;
        }

        if (args[0].equalsIgnoreCase("listar")) {
            player.openInventory(TerrenosListGUI.build(player, manager, 0));
            return true;
        }

        player.sendMessage("§e/terreno info §7- mostra o dono do local");
        player.sendMessage("§e/terreno remover §7- remove seu terreno atual");
        player.sendMessage("§e/terreno listar §7- lista seus terrenos");
        return true;
    }
}
