package com.rpgcustom.terrenosplus.gui;

import com.rpgcustom.terrenosplus.service.MarcoManager;
import com.rpgcustom.terrenosplus.service.TerrenoManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class MarcosGUIListener implements Listener {

    private final MarcoManager marcos;
    private final TerrenoManager terrenos;

    public MarcosGUIListener(MarcoManager marcos, TerrenoManager terrenos) {
        this.marcos = marcos;
        this.terrenos = terrenos;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MarcosGUIHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) return;

        if (holder.view() == MarcosGUIHolder.View.MAIN) {
            if (slot == MarcosGUI.SLOT_BACK) {
                player.openInventory(TerrenosGUI.main(player, terrenos));
                return;
            }
            if (slot == MarcosGUI.SLOT_DAILY) {
                player.openInventory(DailyRewardsGUI.build(player, marcos));
            }
            return;
        }

        if (holder.view() == MarcosGUIHolder.View.DAILY) {
            if (slot == DailyRewardsGUI.SLOT_BACK) {
                player.openInventory(MarcosGUI.main(player, marcos));
                return;
            }

            int day = DailyRewardsGUI.dayFromSlot(slot);
            if (day < 0) return;

            int current = marcos.getDailyCycleIndex(player.getUniqueId());
            if (day != current || !marcos.canClaimDaily(player.getUniqueId())) {
                player.sendMessage(color("&c&lᴍᴀʀᴄᴏs &8• &fEsta recompensa ainda não está disponível."));
                return;
            }

            if (marcos.claimDaily(player)) {
                player.openInventory(DailyRewardsGUI.build(player, marcos));
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof MarcosGUIHolder) {
            event.setCancelled(true);
        }
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }
}
