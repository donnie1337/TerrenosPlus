package com.rpgcustom.terrenosplus.service;

import com.rpgcustom.terrenosplus.TerrenosPlus;
import com.rpgcustom.terrenosplus.api.TerrenosApi;
import com.rpgcustom.terrenosplus.model.Terreno;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public final class TerrenoManager implements TerrenosApi {

    private final TerrenosPlus plugin;
    private final File dataFile;
    private final Map<UUID, Terreno> terrenos = new HashMap<>();
    private final Map<String, Map<Long, Set<UUID>>> chunkIndex = new HashMap<>();

    public TerrenoManager(TerrenosPlus plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "terrenos.yml");
    }

    public void load() {
        terrenos.clear();
        chunkIndex.clear();
        if (!dataFile.exists()) return;

        YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection section = data.getConfigurationSection("terrenos");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                String base = "terrenos." + key + ".";
                UUID owner = UUID.fromString(data.getString(base + "owner"));
                String ownerName = data.getString(base + "owner-name", "Desconhecido");
                String world = data.getString(base + "world");
                int minX = data.getInt(base + "min-x");
                int minZ = data.getInt(base + "min-z");
                int maxX = data.getInt(base + "max-x");
                int maxZ = data.getInt(base + "max-z");
                long createdAt = data.getLong(base + "created-at", 0L);
                long ownerLastSeenAt = data.getLong(base + "last-seen-at", 0L);
                boolean explosionsEnabled = data.getBoolean(base + "explosions-enabled", false);
                boolean visitorFlyEnabled = data.getBoolean(base + "visitor-fly-enabled", true);
                Set<UUID> trustedPlayers = new HashSet<>();
                for (String trusted : data.getStringList(base + "trusted-players")) {
                    try {
                        trustedPlayers.add(UUID.fromString(trusted));
                    } catch (IllegalArgumentException ignored) {
                        plugin.getLogger().warning("UUID trusted inválido ignorado em terreno " + id + ": " + trusted);
                    }
                }
                if (world == null) continue;

                if (ownerLastSeenAt <= 0L) {
                    long bukkitLastPlayed = plugin.getServer().getOfflinePlayer(owner).getLastPlayed();
                    ownerLastSeenAt = bukkitLastPlayed > 0L
                            ? bukkitLastPlayed
                            : (createdAt > 0L ? createdAt : System.currentTimeMillis());
                }

                Terreno terreno = new Terreno(
                        id, owner, ownerName, world,
                        minX, minZ, maxX, maxZ,
                        createdAt, ownerLastSeenAt, explosionsEnabled, visitorFlyEnabled, trustedPlayers
                );
                terrenos.put(id, terreno);
                index(terreno);
            } catch (RuntimeException exception) {
                plugin.getLogger().log(Level.WARNING, "Terreno inválido ignorado: " + key, exception);
            }
        }
    }

    public void save() {
        YamlConfiguration data = new YamlConfiguration();
        for (Terreno terreno : terrenos.values()) {
            String base = "terrenos." + terreno.id() + ".";
            data.set(base + "owner", terreno.ownerId().toString());
            data.set(base + "owner-name", terreno.ownerName());
            data.set(base + "world", terreno.world());
            data.set(base + "min-x", terreno.minX());
            data.set(base + "min-z", terreno.minZ());
            data.set(base + "max-x", terreno.maxX());
            data.set(base + "max-z", terreno.maxZ());
            data.set(base + "created-at", terreno.createdAt());
            data.set(base + "last-seen-at", terreno.ownerLastSeenAt());
            data.set(base + "explosions-enabled", terreno.explosionsEnabled());
            data.set(base + "visitor-fly-enabled", terreno.visitorFlyEnabled());
            data.set(base + "trusted-players",
                    terreno.trustedPlayers().stream().map(UUID::toString).sorted().toList());
        }

        try {
            dataFile.getParentFile().mkdirs();
            data.save(dataFile);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Não foi possível salvar terrenos.yml", exception);
        }
    }

    public CreateResult create(Player player, Location first, Location second) {
        if (first.getWorld() == null || second.getWorld() == null
                || !first.getWorld().equals(second.getWorld())) {
            return CreateResult.differentWorld();
        }

        int maxClaims = resolveMaxClaims(player);
        if (getByOwner(player.getUniqueId()).size() >= maxClaims) {
            return CreateResult.limit(maxClaims);
        }

        Terreno candidate = new Terreno(
                UUID.randomUUID(),
                player.getUniqueId(),
                player.getName(),
                first.getWorld().getName(),
                first.getBlockX(),
                first.getBlockZ(),
                second.getBlockX(),
                second.getBlockZ(),
                System.currentTimeMillis(),
                System.currentTimeMillis()
        );

        int minWidth = Math.max(1, plugin.getConfig().getInt("claims.minimum-width", 5));
        int initialMaxWidth = Math.max(minWidth, plugin.getConfig().getInt("claims.initial-maximum-width", 10));
        long minArea = Math.max(1L, plugin.getConfig().getLong("claims.minimum-area", 25L));
        if (candidate.width() < minWidth || candidate.depth() < minWidth || candidate.area() < minArea) {
            return CreateResult.tooSmall(minWidth);
        }

        if (candidate.width() > initialMaxWidth || candidate.depth() > initialMaxWidth) {
            return CreateResult.initialTooLarge(initialMaxWidth);
        }

        long maxArea = Math.max(minArea,
                plugin.getConfig().getLong("claims.maximum-area-per-claim", 10000L));
        if (candidate.area() > maxArea) {
            return CreateResult.tooLarge(maxArea);
        }

        Terreno overlap = findOverlap(candidate);
        if (overlap != null) {
            return CreateResult.overlap(overlap);
        }

        terrenos.put(candidate.id(), candidate);
        index(candidate);
        save();
        return CreateResult.success(candidate);
    }

    public ExpandResult expand(Terreno terrain, Player player, Direction direction, int amount) {
        if (terrain == null || player == null || direction == null || amount <= 0) {
            return ExpandResult.invalid();
        }
        if (!terrain.ownerId().equals(player.getUniqueId()) && !player.hasPermission("terrenosplus.admin")) {
            return ExpandResult.notOwner();
        }

        int minX = terrain.minX();
        int minZ = terrain.minZ();
        int maxX = terrain.maxX();
        int maxZ = terrain.maxZ();

        switch (direction) {
            case NORTH -> minZ -= amount;
            case SOUTH -> maxZ += amount;
            case WEST -> minX -= amount;
            case EAST -> maxX += amount;
        }

        Terreno expanded = terrain.resized(minX, minZ, maxX, maxZ);

        long maxArea = Math.max(1L,
                plugin.getConfig().getLong("claims.maximum-area-per-claim", 10000L));
        if (expanded.area() > maxArea) {
            return ExpandResult.tooLarge(maxArea);
        }

        Terreno overlap = findOverlapIgnoring(expanded, terrain.id());
        if (overlap != null) {
            return ExpandResult.overlap(overlap);
        }

        unindex(terrain);
        terrenos.put(expanded.id(), expanded);
        index(expanded);
        save();
        return ExpandResult.success(expanded);
    }

    private int resolveMaxClaims(Player player) {
        int memberLimit = Math.max(1, plugin.getConfig().getInt("claims.limits.membro", 3));
        int defaultLimit = Math.max(1, plugin.getConfig().getInt("claims.limits.default", 5));

        try {
            var cargoPlus = plugin.getServer().getPluginManager().getPlugin("CargoPlus");
            if (cargoPlus == null || !cargoPlus.isEnabled()) {
                return memberLimit;
            }

            var apiMethod = cargoPlus.getClass().getMethod("api");
            Object api = apiMethod.invoke(cargoPlus);
            if (api == null) {
                return memberLimit;
            }

            var getGroup = api.getClass().getMethod("getGroup", UUID.class);
            Object groupValue = getGroup.invoke(api, player.getUniqueId());
            String group = groupValue == null ? "" : String.valueOf(groupValue).trim();

            if (group.equalsIgnoreCase("membro") || group.isBlank()) {
                return memberLimit;
            }

            return defaultLimit;
        } catch (ReflectiveOperationException | LinkageError exception) {
            plugin.getLogger().log(Level.WARNING,
                    "Não foi possível consultar o CargoPlus para o limite de terrenos de "
                            + player.getName() + ". Aplicando limite de membro.",
                    exception);
            return memberLimit;
        }
    }

    public boolean setExplosionsEnabled(Terreno terreno, boolean enabled) {
        if (terreno == null || !terrenos.containsKey(terreno.id())) return false;
        terreno.setExplosionsEnabled(enabled);
        save();
        return true;
    }

    public boolean setVisitorFlyEnabled(Terreno terreno, boolean enabled) {
        if (terreno == null || !terrenos.containsKey(terreno.id())) return false;
        terreno.setVisitorFlyEnabled(enabled);
        save();
        return true;
    }

    public boolean trust(Terreno terreno, UUID playerId) {
        if (terreno == null || playerId == null || !terrenos.containsKey(terreno.id())) return false;
        boolean changed = terreno.trust(playerId);
        if (changed) save();
        return changed;
    }

    public boolean untrust(Terreno terreno, UUID playerId) {
        if (terreno == null || playerId == null || !terrenos.containsKey(terreno.id())) return false;
        boolean changed = terreno.untrust(playerId);
        if (changed) save();
        return changed;
    }

    public boolean remove(Terreno terreno) {
        if (terreno == null || terrenos.remove(terreno.id()) == null) return false;
        unindex(terreno);
        save();
        return true;
    }

    public Optional<Terreno> find(Location location) {
        if (location == null || location.getWorld() == null) return Optional.empty();
        Map<Long, Set<UUID>> worldIndex = chunkIndex.get(location.getWorld().getName());
        if (worldIndex == null) return Optional.empty();

        Set<UUID> ids = worldIndex.get(chunkKey(location.getBlockX() >> 4, location.getBlockZ() >> 4));
        if (ids == null) return Optional.empty();

        for (UUID id : ids) {
            Terreno terreno = terrenos.get(id);
            if (terreno != null && terreno.contains(location)) return Optional.of(terreno);
        }
        return Optional.empty();
    }

    public Optional<Terreno> getById(UUID id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(terrenos.get(id));
    }

    public List<Terreno> getByOwner(UUID owner) {
        List<Terreno> result = new ArrayList<>();
        for (Terreno terreno : terrenos.values()) {
            if (terreno.ownerId().equals(owner)) result.add(terreno);
        }
        result.sort((a, b) -> a.id().toString().compareTo(b.id().toString()));
        return result;
    }

    public Collection<Terreno> all() {
        return Collections.unmodifiableCollection(terrenos.values());
    }

    public void markOwnerSeen(UUID ownerId, long timestamp) {
        if (ownerId == null) return;

        boolean changed = false;
        for (Terreno terreno : terrenos.values()) {
            if (!ownerId.equals(terreno.ownerId())) continue;
            long before = terreno.ownerLastSeenAt();
            terreno.markOwnerSeen(timestamp);
            if (terreno.ownerLastSeenAt() != before) changed = true;
        }

        if (changed) save();
    }

    public boolean canBuild(Player player, Location location) {
        if (player.hasPermission("terrenosplus.bypass")) return true;
        Optional<Terreno> terreno = find(location);
        return terreno.isEmpty()
                || terreno.get().ownerId().equals(player.getUniqueId())
                || terreno.get().isTrusted(player.getUniqueId());
    }

    @Override
    public Optional<TerrenoInfo> getTerrenoAt(Location location) {
        return find(location).map(Terreno::toInfo);
    }

    @Override
    public boolean hasBuildAccess(UUID playerId, Location location) {
        Optional<Terreno> terreno = find(location);
        return terreno.isEmpty()
                || terreno.get().ownerId().equals(playerId)
                || terreno.get().isTrusted(playerId);
    }

    private Terreno findOverlap(Terreno candidate) {
        return findOverlapIgnoring(candidate, null);
    }

    private Terreno findOverlapIgnoring(Terreno candidate, UUID ignoredId) {
        int minChunkX = candidate.minX() >> 4;
        int maxChunkX = candidate.maxX() >> 4;
        int minChunkZ = candidate.minZ() >> 4;
        int maxChunkZ = candidate.maxZ() >> 4;
        Map<Long, Set<UUID>> worldIndex = chunkIndex.get(candidate.world());
        if (worldIndex == null) return null;

        Set<UUID> checked = new HashSet<>();
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                Set<UUID> ids = worldIndex.get(chunkKey(cx, cz));
                if (ids == null) continue;
                for (UUID id : ids) {
                    if (!checked.add(id)) continue;
                    if (ignoredId != null && ignoredId.equals(id)) continue;
                    Terreno existing = terrenos.get(id);
                    if (existing != null && candidate.overlaps(existing)) return existing;
                }
            }
        }
        return null;
    }

    private void index(Terreno terreno) {
        Map<Long, Set<UUID>> world = chunkIndex.computeIfAbsent(terreno.world(), ignored -> new HashMap<>());
        forEachChunk(terreno, (cx, cz) ->
                world.computeIfAbsent(chunkKey(cx, cz), ignored -> new HashSet<>()).add(terreno.id()));
    }

    private void unindex(Terreno terreno) {
        Map<Long, Set<UUID>> world = chunkIndex.get(terreno.world());
        if (world == null) return;

        forEachChunk(terreno, (cx, cz) -> {
            long key = chunkKey(cx, cz);
            Set<UUID> ids = world.get(key);
            if (ids == null) return;
            ids.remove(terreno.id());
            if (ids.isEmpty()) world.remove(key);
        });

        if (world.isEmpty()) chunkIndex.remove(terreno.world());
    }

    private void forEachChunk(Terreno terreno, ChunkConsumer consumer) {
        int minChunkX = terreno.minX() >> 4;
        int maxChunkX = terreno.maxX() >> 4;
        int minChunkZ = terreno.minZ() >> 4;
        int maxChunkZ = terreno.maxZ() >> 4;

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                consumer.accept(cx, cz);
            }
        }
    }

    private long chunkKey(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    @FunctionalInterface
    private interface ChunkConsumer {
        void accept(int chunkX, int chunkZ);
    }

    public enum Direction { NORTH, SOUTH, EAST, WEST }

    public record ExpandResult(ExpandType type, Terreno terreno, Terreno overlap, long value) {
        public enum ExpandType { SUCCESS, INVALID, NOT_OWNER, TOO_LARGE, OVERLAP }

        static ExpandResult success(Terreno t) { return new ExpandResult(ExpandType.SUCCESS, t, null, 0); }
        static ExpandResult invalid() { return new ExpandResult(ExpandType.INVALID, null, null, 0); }
        static ExpandResult notOwner() { return new ExpandResult(ExpandType.NOT_OWNER, null, null, 0); }
        static ExpandResult tooLarge(long max) { return new ExpandResult(ExpandType.TOO_LARGE, null, null, max); }
        static ExpandResult overlap(Terreno t) { return new ExpandResult(ExpandType.OVERLAP, null, t, 0); }
    }

    public record CreateResult(Type type, Terreno terreno, Terreno overlap, long value) {
        public enum Type { SUCCESS, DIFFERENT_WORLD, TOO_SMALL, INITIAL_TOO_LARGE, TOO_LARGE, OVERLAP, LIMIT }

        static CreateResult success(Terreno t) { return new CreateResult(Type.SUCCESS, t, null, 0); }
        static CreateResult differentWorld() { return new CreateResult(Type.DIFFERENT_WORLD, null, null, 0); }
        static CreateResult tooSmall(long min) { return new CreateResult(Type.TOO_SMALL, null, null, min); }
        static CreateResult initialTooLarge(long max) { return new CreateResult(Type.INITIAL_TOO_LARGE, null, null, max); }
        static CreateResult tooLarge(long max) { return new CreateResult(Type.TOO_LARGE, null, null, max); }
        static CreateResult overlap(Terreno t) { return new CreateResult(Type.OVERLAP, null, t, 0); }
        static CreateResult limit(long max) { return new CreateResult(Type.LIMIT, null, null, max); }
    }
}
