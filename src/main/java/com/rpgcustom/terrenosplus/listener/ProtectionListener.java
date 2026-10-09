package com.rpgcustom.terrenosplus.listener;

import com.rpgcustom.terrenosplus.TerrenosPlus;
import com.rpgcustom.terrenosplus.model.Terreno;
import com.rpgcustom.terrenosplus.service.TerrenoManager;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Optional;

public final class ProtectionListener implements Listener {

    private final TerrenosPlus plugin;
    private final TerrenoManager manager;

    public ProtectionListener(TerrenosPlus plugin, TerrenoManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!plugin.getConfig().getBoolean("protection.block-break", true)) return;
        protect(event.getPlayer(), event.getBlock().getLocation(), event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!plugin.getConfig().getBoolean("protection.block-place", true)) return;
        protect(event.getPlayer(), event.getBlockPlaced().getLocation(), event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (!plugin.getConfig().getBoolean("protection.interactions", true)) return;
        if (!event.getAction().isRightClick() || event.getClickedBlock() == null) return;
        if (event.getItem() != null && event.getItem().getType().name().equals(
                plugin.getConfig().getString("tool.material", "GOLDEN_SHOVEL"))) return;

        if (!manager.canBuild(event.getPlayer(), event.getClickedBlock().getLocation())) {
            event.setCancelled(true);
            deny(event.getPlayer(), event.getClickedBlock().getLocation());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (!plugin.getConfig().getBoolean("protection.buckets", true)) return;
        Location target = event.getBlockClicked().getRelative(event.getBlockFace()).getLocation();
        if (!manager.canBuild(event.getPlayer(), target)) {
            event.setCancelled(true);
            deny(event.getPlayer(), target);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (!plugin.getConfig().getBoolean("protection.buckets", true)) return;
        Location target = event.getBlockClicked().getLocation();
        if (!manager.canBuild(event.getPlayer(), target)) {
            event.setCancelled(true);
            deny(event.getPlayer(), target);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplosion(EntityExplodeEvent event) {
        if (!plugin.getConfig().getBoolean("protection.explosions", true)) return;
        event.blockList().removeIf(block ->
                manager.find(block.getLocation())
                        .map(terrain -> !terrain.explosionsEnabled())
                        .orElse(false));
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplosion(BlockExplodeEvent event) {
        if (!plugin.getConfig().getBoolean("protection.explosions", true)) return;
        event.blockList().removeIf(block ->
                manager.find(block.getLocation())
                        .map(terrain -> !terrain.explosionsEnabled())
                        .orElse(false));
    }

    @EventHandler(ignoreCancelled = true)
    public void onFluid(BlockFromToEvent event) {
        if (!plugin.getConfig().getBoolean("protection.fluids", true)) return;
        Optional<Terreno> source = manager.find(event.getBlock().getLocation());
        Optional<Terreno> target = manager.find(event.getToBlock().getLocation());

        UUIDPair pair = new UUIDPair(
                source.map(Terreno::id).orElse(null),
                target.map(Terreno::id).orElse(null)
        );
        if (!java.util.Objects.equals(pair.source(), pair.target()) && target.isPresent()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (!plugin.getConfig().getBoolean("protection.pistons", true)) return;
        for (Block block : event.getBlocks()) {
            Location destination = block.getRelative(event.getDirection()).getLocation();
            if (crossesClaimBoundary(block.getLocation(), destination)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (!plugin.getConfig().getBoolean("protection.pistons", true)) return;
        for (Block block : event.getBlocks()) {
            Location destination = block.getRelative(event.getDirection()).getLocation();
            if (crossesClaimBoundary(block.getLocation(), destination)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    private boolean crossesClaimBoundary(Location from, Location to) {
        UUIDPair pair = new UUIDPair(
                manager.find(from).map(Terreno::id).orElse(null),
                manager.find(to).map(Terreno::id).orElse(null)
        );
        return !java.util.Objects.equals(pair.source(), pair.target());
    }

    private void protect(Player player, Location location, org.bukkit.event.Cancellable event) {
        if (manager.canBuild(player, location)) return;
        event.setCancelled(true);
        deny(player, location);
    }

    private void deny(Player player, Location location) {
        manager.find(location).ifPresent(terreno ->
                plugin.send(player, "messages.protected", "{owner}", terreno.ownerName()));
    }

    private record UUIDPair(java.util.UUID source, java.util.UUID target) {}
}
