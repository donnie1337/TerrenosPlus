package com.rpgcustom.terrenosplus.service;

import com.rpgcustom.terrenosplus.TerrenosPlus;
import com.rpgcustom.terrenosplus.model.Terreno;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class ClaimExpirationService implements Listener {

    private static final BlockFace[] LEAF_DIRECTIONS = {
            BlockFace.UP,
            BlockFace.UP,
            BlockFace.UP,
            BlockFace.NORTH,
            BlockFace.SOUTH,
            BlockFace.EAST,
            BlockFace.WEST
    };

    private static final Set<String> NATURAL_BLOCKS = Set.of(
            "AIR", "CAVE_AIR", "VOID_AIR",
            "GRASS_BLOCK", "DIRT", "COARSE_DIRT", "ROOTED_DIRT", "PODZOL", "MYCELIUM",
            "STONE", "DEEPSLATE", "TUFF", "CALCITE", "DRIPSTONE_BLOCK",
            "GRANITE", "DIORITE", "ANDESITE",
            "SAND", "RED_SAND", "SANDSTONE", "RED_SANDSTONE", "GRAVEL", "CLAY",
            "NETHERRACK", "SOUL_SAND", "SOUL_SOIL", "BASALT", "SMOOTH_BASALT",
            "BLACKSTONE", "MAGMA_BLOCK", "BEDROCK", "END_STONE",
            "WATER", "LAVA", "ICE", "PACKED_ICE", "BLUE_ICE", "SNOW", "SNOW_BLOCK"
    );

    private final TerrenosPlus plugin;
    private final TerrenoManager manager;
    private BukkitTask expirationTask;

    public ClaimExpirationService(TerrenosPlus plugin, TerrenoManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    public void start() {
        stop();

        long intervalHours = Math.max(1L,
                plugin.getConfig().getLong("claims.expiration.check-interval-hours", 24L));
        long periodTicks = intervalHours * 60L * 60L * 20L;

        expirationTask = plugin.getServer().getScheduler().runTaskTimer(
                plugin,
                this::checkExpiredClaims,
                20L * 10L,
                periodTicks
        );
    }

    public void stop() {
        if (expirationTask != null) {
            expirationTask.cancel();
            expirationTask = null;
        }
    }

    public void markOnlinePlayersSeen() {
        long now = System.currentTimeMillis();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            manager.markOwnerSeen(player.getUniqueId(), now);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        manager.markOwnerSeen(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        manager.markOwnerSeen(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    private void checkExpiredClaims() {
        if (!plugin.getConfig().getBoolean("claims.expiration.enabled", true)) return;

        long offlineDays = Math.max(1L,
                plugin.getConfig().getLong("claims.expiration.offline-days", 90L));
        long expirationMillis = TimeUnit.DAYS.toMillis(offlineDays);
        long now = System.currentTimeMillis();

        List<Terreno> expired = new ArrayList<>();
        for (Terreno terreno : manager.all()) {
            Player owner = plugin.getServer().getPlayer(terreno.ownerId());
            if (owner != null && owner.isOnline()) continue;

            long lastSeen = terreno.ownerLastSeenAt();
            if (lastSeen <= 0L) continue;
            if (now - lastSeen >= expirationMillis) {
                expired.add(terreno);
            }
        }

        if (expired.isEmpty()) return;

        int delay = 0;
        for (Terreno terreno : expired) {
            if (!manager.remove(terreno)) continue;

            plugin.getLogger().info(
                    "Terreno " + terreno.id() + " de " + terreno.ownerName()
                            + " expirou após " + offlineDays + " dias offline."
            );

            if (plugin.getConfig().getBoolean("claims.expiration.abandoned-foliage.enabled", true)) {
                plugin.getServer().getScheduler().runTaskLater(
                        plugin,
                        () -> growAbandonedFoliage(terreno),
                        delay
                );
                delay += 20;
            }
        }
    }

    private void growAbandonedFoliage(Terreno terreno) {
        World world = plugin.getServer().getWorld(terreno.world());
        if (world == null) return;

        int maxLeaves = Math.max(0,
                plugin.getConfig().getInt("claims.expiration.abandoned-foliage.max-leaves", 18));
        if (maxLeaves <= 0) return;

        Material leafMaterial = resolveLeafMaterial();
        BlockData leafData = leafMaterial.createBlockData();
        if (leafData instanceof Leaves leaves) {
            leaves.setPersistent(true);
        }

        int attempts = Math.max(64, maxLeaves * 10);
        Random random = new Random(
                terreno.id().getMostSignificantBits()
                        ^ terreno.id().getLeastSignificantBits()
                        ^ terreno.world().toLowerCase(Locale.ROOT).hashCode()
        );

        int placed = 0;
        for (int attempt = 0; attempt < attempts && placed < maxLeaves; attempt++) {
            int x = randomBetween(random, terreno.minX(), terreno.maxX());
            int z = randomBetween(random, terreno.minZ(), terreno.maxZ());

            Block source = findConstructionBlock(world, x, z, random);
            if (source == null) continue;

            BlockFace face = LEAF_DIRECTIONS[random.nextInt(LEAF_DIRECTIONS.length)];
            Block target = source.getRelative(face);

            if (!insideTerrainXZ(terreno, target)) continue;
            if (!target.getType().isAir()) continue;
            if (!hasNaturalExposure(target)) continue;

            target.setBlockData(leafData.clone(), false);
            placed++;
        }

        if (placed > 0) {
            plugin.getLogger().info(
                    "Vegetação de abandono adicionada ao terreno expirado "
                            + terreno.id() + ": " + placed + " folha(s)."
            );
        }
    }

    private Block findConstructionBlock(World world, int x, int z, Random random) {
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight() - 1;
        int height = maxY - minY + 1;
        if (height <= 0) return null;

        int startOffset = random.nextInt(height);
        for (int i = 0; i < height; i++) {
            int y = minY + ((startOffset + i) % height);
            Block block = world.getBlockAt(x, y, z);
            if (isConstructionLike(block.getType())) {
                return block;
            }
        }
        return null;
    }

    private boolean isConstructionLike(Material material) {
        if (material == null || material.isAir() || !material.isSolid()) return false;

        String name = material.name();
        if (NATURAL_BLOCKS.contains(name)) return false;
        if (name.endsWith("_ORE") || name.endsWith("_LEAVES") || name.endsWith("_LOG")
                || name.endsWith("_WOOD") || name.endsWith("_SAPLING")
                || name.endsWith("_STEM") || name.endsWith("_HYPHAE")) {
            return false;
        }
        return true;
    }

    private boolean hasNaturalExposure(Block target) {
        return target.getRelative(BlockFace.UP).getType().isAir()
                || target.getRelative(BlockFace.NORTH).getType().isAir()
                || target.getRelative(BlockFace.SOUTH).getType().isAir()
                || target.getRelative(BlockFace.EAST).getType().isAir()
                || target.getRelative(BlockFace.WEST).getType().isAir();
    }

    private boolean insideTerrainXZ(Terreno terreno, Block block) {
        return block.getX() >= terreno.minX() && block.getX() <= terreno.maxX()
                && block.getZ() >= terreno.minZ() && block.getZ() <= terreno.maxZ();
    }

    private Material resolveLeafMaterial() {
        String configured = plugin.getConfig().getString(
                "claims.expiration.abandoned-foliage.material",
                "OAK_LEAVES"
        );
        Material material = Material.matchMaterial(configured == null ? "OAK_LEAVES" : configured);
        if (material == null || !material.name().endsWith("_LEAVES")) {
            return Material.OAK_LEAVES;
        }
        return material;
    }

    private int randomBetween(Random random, int min, int max) {
        if (min >= max) return min;
        return min + random.nextInt(max - min + 1);
    }
}
