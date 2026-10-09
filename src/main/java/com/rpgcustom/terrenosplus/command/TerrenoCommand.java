package com.rpgcustom.terrenosplus.command;

import com.rpgcustom.terrenosplus.TerrenosPlus;
import com.rpgcustom.terrenosplus.model.Terreno;
import com.rpgcustom.terrenosplus.gui.TerrenosGUI;
import com.rpgcustom.terrenosplus.service.TerrenoManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

public final class TerrenoCommand implements CommandExecutor {

    private final TerrenosPlus plugin;
    private final TerrenoManager manager;

    public TerrenoCommand(TerrenosPlus plugin, TerrenoManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Comando disponível apenas para jogadores.");
            return true;
        }

        if (args.length == 0) {
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
            plugin.send(player, "messages.info-owner",
                    "{owner}", t.ownerName(),
                    "{area}", String.valueOf(t.area()),
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
            plugin.send(player, "messages.removed");
            return true;
        }

        if (args[0].equalsIgnoreCase("listar")) {
            List<Terreno> list = manager.getByOwner(player.getUniqueId());
            plugin.send(player, "messages.list-header", "{count}", String.valueOf(list.size()));
            for (Terreno t : list) {
                plugin.send(player, "messages.list-item",
                        "{id}", t.id().toString().substring(0, 8),
                        "{world}", t.world(),
                        "{area}", String.valueOf(t.area()));
            }
            return true;
        }

        player.sendMessage("§e/terreno info §7- mostra o dono do local");
        player.sendMessage("§e/terreno remover §7- remove seu terreno atual");
        player.sendMessage("§e/terreno listar §7- lista seus terrenos");
        return true;
    }
}
