package com.rpgcustom.terrenosplus.command;

import com.rpgcustom.terrenosplus.gui.MarcosGUI;
import com.rpgcustom.terrenosplus.service.MarcoManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class MarcosCommand implements CommandExecutor {

    private final MarcoManager marcos;

    public MarcosCommand(MarcoManager marcos) {
        this.marcos = marcos;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Comando disponível apenas para jogadores.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("testtempo")) {
            if (!player.hasPermission("terrenosplus.admin")) {
                player.sendMessage("§cVocê não tem permissão para usar este comando.");
                return true;
            }

            int reward = marcos.forceHourlyReward(player);
            if (reward > 0) {
                player.sendMessage("§6[Marcos] §r§7Teste de recompensa por tempo executado.");
            }
            return true;
        }

        player.openInventory(MarcosGUI.main(player, marcos));
        return true;
    }
}
