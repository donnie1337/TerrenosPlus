package com.rpgcustom.terrenosplus.model;

import com.rpgcustom.terrenosplus.api.TerrenosApi;
import org.bukkit.Location;

import java.util.UUID;

public final class Terreno {

    private final UUID id;
    private final UUID ownerId;
    private final String ownerName;
    private final String world;
    private final int minX;
    private final int minZ;
    private final int maxX;
    private final int maxZ;
    private final long createdAt;
    private long ownerLastSeenAt;

    public Terreno(UUID id, UUID ownerId, String ownerName, String world,
                   int x1, int z1, int x2, int z2) {
        this(id, ownerId, ownerName, world, x1, z1, x2, z2, 0L, 0L);
    }

    public Terreno(UUID id, UUID ownerId, String ownerName, String world,
                   int x1, int z1, int x2, int z2, long createdAt) {
        this(id, ownerId, ownerName, world, x1, z1, x2, z2, createdAt, createdAt);
    }

    public Terreno(UUID id, UUID ownerId, String ownerName, String world,
                   int x1, int z1, int x2, int z2, long createdAt, long ownerLastSeenAt) {
        this.id = id;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.world = world;
        this.minX = Math.min(x1, x2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxZ = Math.max(z1, z2);
        this.createdAt = Math.max(0L, createdAt);
        this.ownerLastSeenAt = Math.max(0L, ownerLastSeenAt);
    }

    public UUID id() { return id; }
    public UUID ownerId() { return ownerId; }
    public String ownerName() { return ownerName; }
    public String world() { return world; }
    public int minX() { return minX; }
    public int minZ() { return minZ; }
    public int maxX() { return maxX; }
    public int maxZ() { return maxZ; }
    public long createdAt() { return createdAt; }
    public long ownerLastSeenAt() { return ownerLastSeenAt; }
    public void markOwnerSeen(long timestamp) {
        ownerLastSeenAt = Math.max(ownerLastSeenAt, Math.max(0L, timestamp));
    }

    public int width() { return maxX - minX + 1; }
    public int depth() { return maxZ - minZ + 1; }
    public long area() { return (long) width() * depth(); }

    public boolean contains(Location location) {
        return location != null
                && location.getWorld() != null
                && world.equals(location.getWorld().getName())
                && contains(location.getBlockX(), location.getBlockZ());
    }

    public boolean contains(int x, int z) {
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    public boolean overlaps(Terreno other) {
        if (!world.equals(other.world)) return false;
        return minX <= other.maxX && maxX >= other.minX
                && minZ <= other.maxZ && maxZ >= other.minZ;
    }

    public TerrenosApi.TerrenoInfo toInfo() {
        return new TerrenosApi.TerrenoInfo(
                id, ownerId, ownerName, world, minX, minZ, maxX, maxZ, area()
        );
    }
}
