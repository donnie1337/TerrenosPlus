package com.rpgcustom.terrenosplus;

import com.rpgcustom.terrenosplus.api.TerrenosApi;
import com.rpgcustom.terrenosplus.command.MarcosCommand;
import com.rpgcustom.terrenosplus.command.TerrenoCommand;
import com.rpgcustom.terrenosplus.listener.ClaimToolListener;
import com.rpgcustom.terrenosplus.listener.ProtectionListener;
import com.rpgcustom.terrenosplus.listener.TrackingStickListener;
import com.rpgcustom.terrenosplus.listener.TerrainEnterListener;
import com.rpgcustom.terrenosplus.listener.VisitorFlyListener;
import com.rpgcustom.terrenosplus.gui.MarcosGUIListener;
import com.rpgcustom.terrenosplus.gui.TerrenosGUIListener;
import com.rpgcustom.terrenosplus.service.ClaimExpirationService;
import com.rpgcustom.terrenosplus.service.MarcoManager;
import com.rpgcustom.terrenosplus.service.TerrenoManager;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class TerrenosPlus extends JavaPlugin {

    private TerrenoManager terrenoManager;
    private MarcoManager marcoManager;
    private TrackingStickListener trackingStickListener;
    private VisitorFlyListener visitorFlyListener;
    private ClaimExpirationService claimExpirationService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();

        terrenoManager = new TerrenoManager(this);
        terrenoManager.load();

        marcoManager = new MarcoManager(this);
        marcoManager.load();
        marcoManager.start();

        claimExpirationService = new ClaimExpirationService(this, terrenoManager);
        getServer().getPluginManager().registerEvents(claimExpirationService, this);
        claimExpirationService.start();

        getServer().getServicesManager().register(
                TerrenosApi.class,
                terrenoManager,
                this,
                ServicePriority.Highest
        );

        trackingStickListener = new TrackingStickListener(this, terrenoManager);
        getServer().getPluginManager().registerEvents(
                new ClaimToolListener(this, terrenoManager, trackingStickListener), this);
        getServer().getPluginManager().registerEvents(
                new ProtectionListener(this, terrenoManager), this);
        getServer().getPluginManager().registerEvents(
                trackingStickListener, this);
        getServer().getPluginManager().registerEvents(
                new TerrainEnterListener(terrenoManager), this);

        visitorFlyListener = new VisitorFlyListener(terrenoManager);
        getServer().getPluginManager().registerEvents(visitorFlyListener, this);

        getServer().getPluginManager().registerEvents(
                new TerrenosGUIListener(this, terrenoManager, marcoManager, visitorFlyListener), this);
        getServer().getPluginManager().registerEvents(
                new MarcosGUIListener(marcoManager, terrenoManager), this);

        TerrenoCommand terrenoCommand =
                new TerrenoCommand(this, terrenoManager, trackingStickListener, marcoManager);

        PluginCommand command = getCommand("terreno");
        if (command == null) {
            throw new IllegalStateException(
                    "Comando 'terreno' não foi registrado pelo plugin.yml. Verifique o JAR instalado."
            );
        }
        command.setExecutor(terrenoCommand);
        command.setTabCompleter(terrenoCommand);

        PluginCommand marcosCommand = getCommand("marcos");
        if (marcosCommand == null) {
            throw new IllegalStateException(
                    "Comando 'marcos' não foi registrado pelo plugin.yml. Verifique o JAR instalado."
            );
        }
        MarcosCommand marcosExecutor = new MarcosCommand(marcoManager);
        marcosCommand.setExecutor(marcosExecutor);

        getLogger().info(
                "TerrenosPlus ativado com " + terrenoManager.all().size()
                        + " terreno(s). Comandos registrados: /terreno, /terrenos, /marcos e /marco."
        );
    }

    @Override
    public void onDisable() {
        if (claimExpirationService != null) {
            claimExpirationService.markOnlinePlayersSeen();
            claimExpirationService.stop();
        }
        if (marcoManager != null) {
            marcoManager.stop();
            marcoManager.save();
        }
        if (terrenoManager != null) {
            terrenoManager.save();
        }
        getServer().getServicesManager().unregisterAll(this);
    }

    public TerrenoManager getTerrenoManager() {
        return terrenoManager;
    }

    public MarcoManager getMarcoManager() {
        return marcoManager;
    }

    public TrackingStickListener getTrackingStickListener() {
        return trackingStickListener;
    }

    public VisitorFlyListener getVisitorFlyListener() {
        return visitorFlyListener;
    }

    public void sendMarcos(Player player, String path, String... replacements) {
        String prefix = "&6[Marcos] &r";
        String message = getConfig().getString(path);
        if (message == null || message.equals(path)) {
            message = path;
        }
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            message = message.replace(replacements[i], replacements[i + 1]);
        }
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + message));
    }

    public void send(Player player, String path, String... replacements) {
        String prefix = "&a[Terrenos] &r";
        String message = getConfig().getString(path);
        if (message == null || message.equals(path)) {
            message = switch (path) {
                case "messages.tracker-unprotected" -> "&7Este bloco não está protegido por nenhum terreno.";
                case "messages.tracker-info" -> "&7Dono: &f{owner} &8| &7Tamanho: &f{width}x{depth}";
                case "messages.command-own-claim-only" -> "&cEste comando só pode ser usado dentro do seu próprio terreno.";
                default -> path;
            };
        }
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            message = message.replace(replacements[i], replacements[i + 1]);
        }
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + message));
    }
}
