package com.rpgcustom.terrenosplus.gui;

import com.rpgcustom.terrenosplus.TerrenosPlus;
import com.rpgcustom.terrenosplus.model.Terreno;
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

    public TerrenosGUIListener(TerrenosPlus plugin, TerrenoManager manager) {
        this.plugin = plugin;
        this.manager = manager;
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
                    player.sendMessage(color("&c&lᴛᴇʀʀᴇɴᴏs &8• &fFique dentro de um terreno seu para gerenciá-lo."));
                    return;
                }

                Terreno t = current.get();
                player.sendMessage(color("&b&lᴛᴇʀʀᴇɴᴏs &8• &fTerreno atual: &e"
                        + t.width() + "x" + t.depth() + " &8(" + t.area() + " blocos&8)&f."));
                player.sendMessage(color("&7As permissões individuais serão exibidas aqui quando forem configuradas."));
                return;
            }

            if (slot == TerrenosGUI.SLOT_MARCOS) {
                player.sendMessage(color("&b&lᴍᴀʀᴄᴏs &8• &fVocê ganha Marcos por tempo jogado, missões e recompensas diárias."));
            }
            return;
        }

        if (holder.view() == TerrenosGUIHolder.View.LIST) {
            if (slot == TerrenosListGUI.SLOT_BACK) {
                player.openInventory(TerrenosGUI.main(player, manager));
            } else if (slot == TerrenosListGUI.SLOT_PREVIOUS && holder.page() > 0) {
                player.openInventory(TerrenosListGUI.build(player, manager, holder.page() - 1));
            } else if (slot == TerrenosListGUI.SLOT_NEXT) {
                player.openInventory(TerrenosListGUI.build(player, manager, holder.page() + 1));
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof TerrenosGUIHolder) {
            event.setCancelled(true);
        }
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }
}
