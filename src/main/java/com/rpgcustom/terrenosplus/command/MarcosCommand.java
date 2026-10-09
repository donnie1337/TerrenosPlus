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

        player.openInventory(MarcosGUI.main(player, marcos));
        return true;
    }
}
