package com.rpgcustom.terrenosplus.gui;

import com.rpgcustom.terrenosplus.TerrenosPlus;
import com.rpgcustom.terrenosplus.model.Terreno;
import com.rpgcustom.terrenosplus.service.MarcoManager;
import com.rpgcustom.terrenosplus.service.TerrenoManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

import java.util.Optional;

public final class TerrenosGUIListener implements Listener {

    private final TerrenosPlus plugin;
    private final TerrenoManager manager;
    private final MarcoManager marcos;

    public TerrenosGUIListener(TerrenosPlus plugin, TerrenoManager manager, MarcoManager marcos) {
        this.plugin = plugin;
        this.manager = manager;
        this.marcos = marcos;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof TerrenosGUIHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) return;

        if (holder.view() == TerrenosGUIHolder.View.MAIN) {
            if (slot == TerrenosGUI.SLOT_LIST) {
                player.openInventory(TerrenosListGUI.build(player, manager, 0));
                return;
            }

            if (slot == TerrenosGUI.SLOT_MANAGE) {
                Optional<Terreno> current = manager.find(player.getLocation());
                if (current.isEmpty() || !current.get().ownerId().equals(player.getUniqueId())) {
                    player.sendMessage(color("&a[Terrenos] &r&cFique dentro de um terreno seu para gerenciá-lo."));
                    return;
                }

                Terreno t = current.get();
                player.openInventory(TerrenosGUI.manage(player, t, marcos, plugin));
                return;
            }

            if (slot == TerrenosGUI.SLOT_MARCOS) {
                player.openInventory(MarcosGUI.main(player, marcos));
            }
            return;
        }

        if (holder.view() == TerrenosGUIHolder.View.MANAGE) {
            Optional<Terreno> current = manager.find(player.getLocation());
            if (current.isEmpty() || !current.get().ownerId().equals(player.getUniqueId())) {
                player.closeInventory();
                player.sendMessage(color("&a[Terrenos] &r&cFique dentro de um terreno seu para gerenciá-lo."));
                return;
            }

            Terreno terrain = current.get();

            if (slot == TerrenosGUI.SLOT_BACK) {
                player.openInventory(TerrenosGUI.main(player, manager));
                return;
            }

            if (slot == TerrenosGUI.SLOT_EXPLOSIONS) {
                boolean enabled = !terrain.explosionsEnabled();
                if (!manager.setExplosionsEnabled(terrain, enabled)) {
                    plugin.send(player, "messages.explosion-toggle-failed");
                    return;
                }
                plugin.send(player, enabled
                        ? "messages.explosions-enabled"
                        : "messages.explosions-disabled");
                player.openInventory(TerrenosGUI.manage(player, terrain, marcos, plugin));
                return;
            }

            TerrenoManager.Direction direction = null;
            if (slot == TerrenosGUI.SLOT_EXPAND_NORTH) direction = TerrenoManager.Direction.NORTH;
            if (slot == TerrenosGUI.SLOT_EXPAND_SOUTH) direction = TerrenoManager.Direction.SOUTH;
            if (slot == TerrenosGUI.SLOT_EXPAND_EAST) direction = TerrenoManager.Direction.EAST;
            if (slot == TerrenosGUI.SLOT_EXPAND_WEST) direction = TerrenoManager.Direction.WEST;

            if (direction != null) {
                long borderLength = switch (direction) {
                    case EAST, WEST -> terrain.depth();
                    case NORTH, SOUTH -> terrain.width();
                };
                int costPerBlock = Math.max(1,
                        plugin.getConfig().getInt("claims.expansion.marcos-per-block", 1));
                long rawCost = borderLength * costPerBlock;
                if (rawCost <= 0L || rawCost > Integer.MAX_VALUE) {
                    plugin.send(player, "messages.expand-invalid");
                    return;
                }
                int cost = (int) rawCost;
                int balance = marcos.getBalance(player.getUniqueId());

                if (balance < cost) {
                    plugin.send(player, "messages.expand-no-marcos",
                            "{cost}", String.valueOf(cost),
                            "{balance}", String.valueOf(balance));
                    return;
                }

                TerrenoManager.ExpandResult result = manager.expand(terrain, player, direction, 1);
                switch (result.type()) {
                    case SUCCESS -> {
                        if (!marcos.take(player.getUniqueId(), cost)) {
                            plugin.getLogger().warning("Falha ao debitar Marcos após expansão via GUI de "
                                    + player.getName());
                            return;
                        }
                        Terreno expanded = result.terreno();
                        plugin.getTrackingStickListener().showExpandedTerrain(player, expanded);
                        plugin.send(player, "messages.expanded",
                                "{amount}", "1",
                                "{direction}", directionName(direction),
                                "{cost}", String.valueOf(cost),
                                "{width}", String.valueOf(expanded.width()),
                                "{depth}", String.valueOf(expanded.depth()),
                                "{balance}", String.valueOf(marcos.getBalance(player.getUniqueId())));
                        player.openInventory(TerrenosGUI.manage(player, expanded, marcos, plugin));
                    }
                    case OVERLAP -> plugin.send(player, "messages.expand-overlap",
                            "{owner}", result.overlap().ownerName());
                    case TOO_LARGE -> plugin.send(player, "messages.expand-too-large",
                            "{max}", String.valueOf(result.value()));
                    case NOT_OWNER -> plugin.send(player, "messages.not-owner");
                    case INVALID -> plugin.send(player, "messages.expand-invalid");
                }
            }
            return;
        }

        if (holder.view() == TerrenosGUIHolder.View.LIST) {
            if (slot == TerrenosListGUI.backSlot(event.getInventory().getSize())) {
                player.openInventory(TerrenosGUI.main(player, manager));
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof TerrenosGUIHolder) {
            event.setCancelled(true);
        }
    }

    private String directionName(TerrenoManager.Direction direction) {
        return switch (direction) {
            case NORTH -> "Norte";
            case SOUTH -> "Sul";
            case EAST -> "Leste";
            case WEST -> "Oeste";
        };
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }
}
