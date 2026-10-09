package com.rpgcustom.terrenosplus.service;

import com.rpgcustom.terrenosplus.TerrenosPlus;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

public final class MarcoManager {

    private static final long HOURLY_SECONDS = 3600L;
    private static final int[] HOURLY_REWARDS = {25, 50, 75};

    private final TerrenosPlus plugin;
    private final File dataFile;
    private final Map<UUID, Integer> balances = new HashMap<>();
    private final Map<UUID, Long> playedSeconds = new HashMap<>();
    private final Map<UUID, LocalDate> lastDailyClaim = new HashMap<>();
    private final Map<UUID, Integer> dailyCycleIndex = new HashMap<>();
    private BukkitTask playtimeTask;

    public MarcoManager(TerrenosPlus plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "marcos.yml");
    }

    public void load() {
        balances.clear();
        playedSeconds.clear();
        lastDailyClaim.clear();
        dailyCycleIndex.clear();

        if (!dataFile.exists()) return;

        YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection section = data.getConfigurationSection("players");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String base = "players." + key + ".";
                balances.put(uuid, Math.max(0, data.getInt(base + "balance", 0)));
                playedSeconds.put(uuid, Math.max(0L, data.getLong(base + "played-seconds", 0L)));
                dailyCycleIndex.put(uuid, Math.max(0, Math.min(6, data.getInt(base + "daily-cycle-index", 0))));

                String date = data.getString(base + "last-daily-claim", "");
                if (date != null && !date.isBlank()) {
                    lastDailyClaim.put(uuid, LocalDate.parse(date));
                }
            } catch (RuntimeException exception) {
                plugin.getLogger().log(Level.WARNING, "Entrada inválida ignorada em marcos.yml: " + key, exception);
            }
        }
    }

    public void save() {
        YamlConfiguration data = new YamlConfiguration();

        java.util.Set<UUID> ids = new java.util.HashSet<>();
        ids.addAll(balances.keySet());
        ids.addAll(playedSeconds.keySet());
        ids.addAll(lastDailyClaim.keySet());
        ids.addAll(dailyCycleIndex.keySet());

        for (UUID uuid : ids) {
            String base = "players." + uuid + ".";
            data.set(base + "balance", getBalance(uuid));
            data.set(base + "played-seconds", getPlayedSeconds(uuid));
            data.set(base + "daily-cycle-index", getDailyCycleIndex(uuid));
            LocalDate last = lastDailyClaim.get(uuid);
            data.set(base + "last-daily-claim", last == null ? null : last.toString());
        }

        try {
            dataFile.getParentFile().mkdirs();
            data.save(dataFile);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Não foi possível salvar marcos.yml", exception);
        }
    }

    public void start() {
        stop();
        playtimeTask = plugin.getServer().getScheduler().runTaskTimer(
                plugin,
                this::tickOnlinePlayers,
                20L * 60L,
                20L * 60L
        );
    }

    public void stop() {
        if (playtimeTask != null) {
            playtimeTask.cancel();
            playtimeTask = null;
        }
    }

    private void tickOnlinePlayers() {
        boolean changed = false;

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            long seconds = getPlayedSeconds(uuid) + 60L;

            while (seconds >= HOURLY_SECONDS) {
                seconds -= HOURLY_SECONDS;
                grantHourlyReward(player);
            }

            playedSeconds.put(uuid, seconds);
            changed = true;
        }

        if (changed) save();
    }

    private int grantHourlyReward(Player player) {
        int reward = HOURLY_REWARDS[ThreadLocalRandom.current().nextInt(HOURLY_REWARDS.length)];
        add(player.getUniqueId(), reward);
        plugin.sendMarcos(player, "messages.marcos-hourly-reward",
                "{amount}", String.valueOf(reward),
                "{balance}", String.valueOf(getBalance(player.getUniqueId())));
        showHourlyRewardTitle(player, reward);
        return reward;
    }

    public int forceHourlyReward(Player player) {
        if (player == null || !player.isOnline()) return 0;
        int reward = grantHourlyReward(player);
        save();
        return reward;
    }

    private void showHourlyRewardTitle(Player player, int reward) {
        String title = plugin.getConfig().getString(
                "marcos.hourly.title",
                "&e&l+" + reward + " Marcos"
        );
        String subtitle = plugin.getConfig().getString(
                "marcos.hourly.subtitle",
                "&fRecompensa por &e1 hora &fonline"
        );

        title = title.replace("{amount}", String.valueOf(reward));
        subtitle = subtitle.replace("{amount}", String.valueOf(reward));

        player.sendTitle(
                ChatColor.translateAlternateColorCodes('&', title),
                ChatColor.translateAlternateColorCodes('&', subtitle),
                10,
                60,
                10
        );
    }

    public int getBalance(UUID uuid) {
        return Math.max(0, balances.getOrDefault(uuid, 0));
    }

    public void add(UUID uuid, int amount) {
        if (uuid == null || amount <= 0) return;
        long updated = (long) getBalance(uuid) + amount;
        balances.put(uuid, (int) Math.min(Integer.MAX_VALUE, updated));
    }

    public boolean take(UUID uuid, int amount) {
        if (uuid == null || amount <= 0) return false;
        int current = getBalance(uuid);
        if (current < amount) return false;
        balances.put(uuid, current - amount);
        save();
        return true;
    }

    public long getPlayedSeconds(UUID uuid) {
        return Math.max(0L, playedSeconds.getOrDefault(uuid, 0L));
    }

    public long getSecondsUntilHourlyReward(UUID uuid) {
        return Math.max(0L, HOURLY_SECONDS - getPlayedSeconds(uuid));
    }

    public int getDailyCycleIndex(UUID uuid) {
        normalizeDailyCycle(uuid);
        return Math.max(0, Math.min(6, dailyCycleIndex.getOrDefault(uuid, 0)));
    }

    public boolean canClaimDaily(UUID uuid) {
        normalizeDailyCycle(uuid);
        return !LocalDate.now().equals(lastDailyClaim.get(uuid));
    }

    public int getTodayDailyReward(UUID uuid) {
        return getDailyReward(getDailyCycleIndex(uuid));
    }

    public int getDailyReward(int index) {
        java.util.List<Integer> configured = plugin.getConfig().getIntegerList("marcos.daily.rewards");
        int[] fallback = {5, 5, 10, 10, 15, 20, 25};
        if (index < 0 || index > 6) index = 0;
        if (configured.size() >= 7) return Math.max(1, configured.get(index));
        return fallback[index];
    }

    public boolean claimDaily(Player player) {
        UUID uuid = player.getUniqueId();
        normalizeDailyCycle(uuid);
        if (!canClaimDaily(uuid)) return false;

        int index = getDailyCycleIndex(uuid);
        int reward = getDailyReward(index);
        add(uuid, reward);
        lastDailyClaim.put(uuid, LocalDate.now());
        dailyCycleIndex.put(uuid, (index + 1) % 7);
        save();

        plugin.sendMarcos(player, "messages.marcos-daily-claimed",
                "{amount}", String.valueOf(reward),
                "{balance}", String.valueOf(getBalance(uuid)));
        return true;
    }

    public boolean wasDailyClaimedToday(UUID uuid) {
        normalizeDailyCycle(uuid);
        return LocalDate.now().equals(lastDailyClaim.get(uuid));
    }

    private void normalizeDailyCycle(UUID uuid) {
        LocalDate last = lastDailyClaim.get(uuid);
        if (last == null) return;

        long days = ChronoUnit.DAYS.between(last, LocalDate.now());
        if (days > 1) {
            dailyCycleIndex.put(uuid, 0);
        }
    }
}
