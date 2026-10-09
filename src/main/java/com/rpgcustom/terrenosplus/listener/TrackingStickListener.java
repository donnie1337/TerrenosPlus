package com.rpgcustom.terrenosplus.listener;

import com.rpgcustom.terrenosplus.TerrenosPlus;
import com.rpgcustom.terrenosplus.model.Terreno;
import com.rpgcustom.terrenosplus.service.TerrenoManager;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TrackingStickListener implements Listener {

    private static final NamespacedKey TERRAIN_TOOL_KEY = new NamespacedKey("terrenosplus", "tool");

    private final TerrenosPlus plugin;
    private final TerrenoManager manager;
    private final Map<UUID, MarkerState> activeMarkers = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> trackedTerrains = new ConcurrentHashMap<>();
    private final Map<UUID, Long> markerExpiry = new ConcurrentHashMap<>();
    private final Map<UUID, Long> messageCooldownUntil = new ConcurrentHashMap<>();
    private final Map<UUID, CreationVisualLock> creationVisualLocks = new ConcurrentHashMap<>();
    private final Map<UUID, Long> trackerPauseUntil = new ConcurrentHashMap<>();

    public TrackingStickListener(TerrenosPlus plugin, TerrenoManager manager) {
        this.plugin = plugin;
        this.manager = manager;

        // Enquanto o jogador estiver segurando o palito, redesenha os limites
        // periodicamente. Ao trocar/largar o item, nenhum novo efeito é enviado.
        plugin.getServer().getScheduler().runTaskTimer(
                plugin,
                this::showHeldTrackerBoundaries,
                1L,
                5L
        );
    }

    private void showHeldTrackerBoundaries() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            UUID playerId = player.getUniqueId();

            CreationVisualLock visualLock = creationVisualLocks.get(playerId);
            if (visualLock != null) {
                if (visualLock.expiresAt() > System.currentTimeMillis()) {
                    // Durante os 5 segundos de confirmação da criação, o tracker
                    // não pode sobrescrever as esmeraldas com blocos de ouro.
                    continue;
                }
                creationVisualLocks.remove(playerId, visualLock);
            }

            long pausedUntil = trackerPauseUntil.getOrDefault(playerId, 0L);
            if (pausedUntil > System.currentTimeMillis()) {
                continue;
            }
            trackerPauseUntil.remove(playerId);

            if (!isHoldingTrackingStick(player)) {
                long expiresAt = markerExpiry.getOrDefault(playerId, 0L);
                if (expiresAt == 0L && trackedTerrains.containsKey(playerId)) {
                    markerExpiry.put(playerId, System.currentTimeMillis() + 30_000L);
                    expiresAt = markerExpiry.get(playerId);
                }

                if (expiresAt > System.currentTimeMillis()) {
                    UUID trackedId = trackedTerrains.get(playerId);
                    if (trackedId != null) {
                        manager.getById(trackedId).ifPresent(terrain -> {
                            showBoundary(player, terrain, Color.YELLOW);
                            showCornerMarkers(player, terrain);
                        });
                    }
                    continue;
                }

                markerExpiry.remove(playerId);
                trackedTerrains.remove(playerId);
                restoreCornerMarkers(player);
                continue;
            }

            markerExpiry.remove(playerId);

            UUID trackedId = trackedTerrains.get(playerId);
            if (trackedId == null) {
                Terreno automatic = findAutomaticTerrain(player);
                if (automatic != null) {
                    trackedTerrains.put(playerId, automatic.id());
                    trackedId = automatic.id();
                }
            }

            if (trackedId == null) {
                restoreCornerMarkers(player);
                continue;
            }

            var terrain = manager.getById(trackedId);
            if (terrain.isEmpty()) {
                trackedTerrains.remove(player.getUniqueId());
                restoreCornerMarkers(player);
                continue;
            }

            Terreno current = terrain.get();
            showBoundary(player, current, Color.YELLOW);
            showCornerMarkers(player, current);
        }
    }

    private Terreno findAutomaticTerrain(Player player) {
        String worldName = player.getWorld().getName();
        Location playerLocation = player.getLocation();

        Terreno nearest = null;
        double nearestDistanceSquared = Double.MAX_VALUE;

        for (Terreno terrain : manager.getByOwner(player.getUniqueId())) {
            if (!terrain.world().equals(worldName)) continue;

            double centerX = (terrain.minX() + terrain.maxX()) / 2.0;
            double centerZ = (terrain.minZ() + terrain.maxZ()) / 2.0;
            double dx = playerLocation.getX() - centerX;
            double dz = playerLocation.getZ() - centerZ;
            double distanceSquared = dx * dx + dz * dz;

            if (distanceSquared < nearestDistanceSquared) {
                nearestDistanceSquared = distanceSquared;
                nearest = terrain;
            }
        }

        return nearest;
    }

    private boolean isHoldingTrackingStick(Player player) {
        return isTrackingStick(player.getInventory().getItemInMainHand())
                || isTrackingStick(player.getInventory().getItemInOffHand());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        markerExpiry.remove(uuid);
        trackedTerrains.remove(uuid);
        activeMarkers.remove(uuid);
        messageCooldownUntil.remove(uuid);
        creationVisualLocks.remove(uuid);
        trackerPauseUntil.remove(uuid);
    }

    public void protectCreationPreview(Player player, Terreno terrain, long durationMillis) {
        if (player == null || terrain == null) return;
        creationVisualLocks.put(
                player.getUniqueId(),
                new CreationVisualLock(terrain.id(), System.currentTimeMillis() + Math.max(0L, durationMillis))
        );

        UUID tracked = trackedTerrains.get(player.getUniqueId());
        if (terrain.id().equals(tracked)) {
            restoreCornerMarkers(player);
        }
    }

    public void onTerrainRemoved(Player remover, Terreno terrain) {
        if (terrain == null) return;

        for (Player online : plugin.getServer().getOnlinePlayers()) {
            UUID playerId = online.getUniqueId();
            UUID tracked = trackedTerrains.get(playerId);
            MarkerState state = activeMarkers.get(playerId);

            if (terrain.id().equals(tracked)
                    || (state != null && terrain.id().equals(state.terrainId()))) {
                trackedTerrains.remove(playerId);
                markerExpiry.remove(playerId);
                creationVisualLocks.remove(playerId);
                restoreCornerMarkers(online);
            }
        }

        if (remover != null && remover.isOnline()) {
            trackerPauseUntil.put(remover.getUniqueId(), System.currentTimeMillis() + 1500L);
            flashRemovedBoundary(remover, terrain);
        }
    }

    private void flashRemovedBoundary(Player player, Terreno terrain) {
        // Mantém o contorno vermelho visível por mais tempo após a remoção.
        // O mesmo cálculo de altura do tracker é usado, então o efeito acompanha
        // o relevo em diagonal e permanece um bloco acima dos blocos da borda.
        for (int pulse = 0; pulse < 8; pulse++) {
            plugin.getServer().getScheduler().runTaskLater(
                    plugin,
                    () -> {
                        if (player.isOnline()) showBoundary(player, terrain, Color.RED);
                    },
                    pulse * 10L
            );
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        ItemStack item = event.getItem();
        if (!isTrackingStick(item)) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        var terrain = manager.find(event.getClickedBlock().getLocation());

        if (terrain.isEmpty()) {
            trackedTerrains.remove(player.getUniqueId());
            restoreCornerMarkers(player);
            sendTrackerMessage(player, "messages.tracker-unprotected");
            return;
        }

        Terreno t = terrain.get();
        trackedTerrains.put(player.getUniqueId(), t.id());
        sendTrackerMessage(player, "messages.tracker-info",
                "{owner}", t.ownerName(),
                "{area}", String.valueOf(t.area()),
                "{width}", String.valueOf(t.width()),
                "{depth}", String.valueOf(t.depth()));

        if (t.ownerId().equals(player.getUniqueId()) || player.hasPermission("terrenosplus.admin")) {
            showBoundary(player, t, Color.YELLOW);
            showCornerMarkers(player, t);
        }
    }

    private void showCornerMarkers(Player player, Terreno terrain) {
        var world = plugin.getServer().getWorld(terrain.world());
        if (world == null) {
            restoreCornerMarkers(player);
            return;
        }

        List<Location> corners = List.of(
                groundCorner(world, terrain.minX(), terrain.minZ()),
                groundCorner(world, terrain.minX(), terrain.maxZ()),
                groundCorner(world, terrain.maxX(), terrain.minZ()),
                groundCorner(world, terrain.maxX(), terrain.maxZ())
        );

        MarkerState previous = activeMarkers.get(player.getUniqueId());
        if (previous != null && !previous.terrainId().equals(terrain.id())) {
            restoreCornerMarkers(player);
        }

        var gold = Material.GOLD_BLOCK.createBlockData();
        for (Location corner : corners) {
            player.sendBlockChange(corner, gold);
        }

        activeMarkers.put(player.getUniqueId(), new MarkerState(terrain.id(), corners));
    }

    private Location groundCorner(org.bukkit.World world, int x, int z) {
        int y = world.getHighestBlockYAt(x, z);
        return new Location(world, x, y, z);
    }

    private void restoreCornerMarkers(Player player) {
        MarkerState state = activeMarkers.remove(player.getUniqueId());
        if (state == null) return;

        for (Location location : state.locations()) {
            if (location.getWorld() == null) continue;
            player.sendBlockChange(location, location.getBlock().getBlockData());
        }
    }

    private void sendTrackerMessage(Player player, String path, String... replacements) {
        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();
        long allowedAt = messageCooldownUntil.getOrDefault(playerId, 0L);
        if (now < allowedAt) return;

        messageCooldownUntil.put(playerId, now + 2_000L);
        plugin.send(player, path, replacements);
    }

    private boolean isTrackingStick(ItemStack item) {
        if (item == null || item.getType() != Material.STICK || !item.hasItemMeta()) return false;
        String type = item.getItemMeta().getPersistentDataContainer()
                .get(TERRAIN_TOOL_KEY, PersistentDataType.STRING);
        return "inspect".equals(type);
    }

    private void showBoundary(Player player, Terreno terrain, Color color) {
        var world = plugin.getServer().getWorld(terrain.world());
        if (world == null) return;

        Location minMin = groundCorner(world, terrain.minX(), terrain.minZ());
        Location minMax = groundCorner(world, terrain.minX(), terrain.maxZ());
        Location maxMin = groundCorner(world, terrain.maxX(), terrain.minZ());
        Location maxMax = groundCorner(world, terrain.maxX(), terrain.maxZ());

        int step = Math.max(1, Math.min(4, Math.max(terrain.width(), terrain.depth()) / 20));

        // A linha acompanha a diferença de altura entre os cantos em diagonal,
        // mas permanece um bloco acima dos marcadores/blocos da borda.
        showEdge(player, minMin, maxMin, step, color);
        showEdge(player, minMax, maxMax, step, color);
        showEdge(player, minMin, minMax, step, color);
        showEdge(player, maxMin, maxMax, step, color);
    }

    private void showEdge(Player player, Location from, Location to, int step, Color color) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double horizontalDistance = Math.max(Math.abs(dx), Math.abs(dz));
        int segments = Math.max(1, (int) Math.ceil(horizontalDistance / Math.max(1, step)));

        for (int i = 0; i <= segments; i++) {
            double progress = i / (double) segments;
            double x = from.getX() + (dx * progress) + 0.5;
            double y = from.getY() + ((to.getY() - from.getY()) * progress) + 2.10D;
            double z = from.getZ() + (dz * progress) + 0.5;
            particle(player, new Location(from.getWorld(), x, y, z), color);
        }
    }

    private void particle(Player player, Location location, Color color) {
        player.spawnParticle(
                Particle.DUST,
                location,
                2,
                0.05, 0.05, 0.05,
                0.0,
                new Particle.DustOptions(color, 1.15f)
        );
    }

    private record MarkerState(UUID terrainId, List<Location> locations) {
    }

    private record CreationVisualLock(UUID terrainId, long expiresAt) {
    }
}
