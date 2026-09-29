package com.rpgcustom.terrenosplus.api;

import org.bukkit.Location;

import java.util.Optional;
import java.util.UUID;

public interface TerrenosApi {

    Optional<TerrenoInfo> getTerrenoAt(Location location);

    boolean hasBuildAccess(UUID playerId, Location location);

    record TerrenoInfo(
            UUID id,
            UUID ownerId,
            String ownerName,
            String world,
            int minX,
            int minZ,
            int maxX,
            int maxZ,
            long area
    ) {}
}
