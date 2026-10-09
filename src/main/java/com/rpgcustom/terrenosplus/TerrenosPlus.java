package com.rpgcustom.terrenosplus;

import com.rpgcustom.terrenosplus.api.TerrenosApi;
import com.rpgcustom.terrenosplus.command.TerrenoCommand;
import com.rpgcustom.terrenosplus.listener.ClaimToolListener;
import com.rpgcustom.terrenosplus.listener.ProtectionListener;
import com.rpgcustom.terrenosplus.gui.TerrenosGUIListener;
import com.rpgcustom.terrenosplus.service.TerrenoManager;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class TerrenosPlus extends JavaPlugin {

    private TerrenoManager terrenoManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        terrenoManager = new TerrenoManager(this);
        terrenoManager.load();

        getServer().getServicesManager().register(
                TerrenosApi.class,
                terrenoManager,
                this,
                ServicePriority.Highest
        );

        getServer().getPluginManager().registerEvents(
                new ClaimToolListener(this, terrenoManager), this);
        getServer().getPluginManager().registerEvents(
                new ProtectionListener(this, terrenoManager), this);
        getServer().getPluginManager().registerEvents(
                new TerrenosGUIListener(this, terrenoManager), this);

        PluginCommand command = getCommand("terreno");
        if (command != null) {
            command.setExecutor(new TerrenoCommand(this, terrenoManager));
        }

        getLogger().info("TerrenosPlus ativado com " + terrenoManager.all().size() + " terreno(s).");
    }

    @Override
    public void onDisable() {
        if (terrenoManager != null) {
            terrenoManager.save();
        }
        getServer().getServicesManager().unregisterAll(this);
    }

    public TerrenoManager getTerrenoManager() {
        return terrenoManager;
    }

    public void send(Player player, String path, String... replacements) {
        String prefix = getConfig().getString("messages.prefix", "&6&lTerrenos &8» &r");
        String message = getConfig().getString(path, path);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            message = message.replace(replacements[i], replacements[i + 1]);
        }
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + message));
    }
}
