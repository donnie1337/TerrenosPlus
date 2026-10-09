package com.rpgcustom.terrenosplus.gui;

import com.rpgcustom.terrenosplus.service.MarcoManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;

public final class DailyRewardsGUI {

    // Caminho em zigue-zague, inspirado em uma trilha de progresso.
    public static final int[] DAY_SLOTS = {10, 20, 30, 40, 32, 24, 16};
    public static final int SLOT_BACK = 45;
    public static final int SLOT_PROFILE = 49;

    private DailyRewardsGUI() {
    }

    public static Inventory build(Player player, MarcoManager marcos) {
        MarcosGUIHolder holder = new MarcosGUIHolder(MarcosGUIHolder.View.DAILY);
        Inventory inventory = Bukkit.createInventory(holder, 54, "Recompensas Diárias");
        holder.setInventory(inventory);

        int currentIndex = marcos.getDailyCycleIndex(player.getUniqueId());
        boolean claimedToday = marcos.wasDailyClaimedToday(player.getUniqueId());

        for (int day = 0; day < 7; day++) {
            int reward = marcos.getDailyReward(day);
            boolean completed = claimedToday
                    ? day < currentIndex || (currentIndex == 0 && day == 6)
                    : day < currentIndex;
            boolean today = !claimedToday && day == currentIndex;

            Material material;
            String name;
            List<String> lore = new ArrayList<>();
            lore.add("");

            if (completed) {
                material = Material.LIME_CONCRETE;
                name = "&a&lᴅɪᴀ " + (day + 1) + " ✓";
                lore.add("&aRecompensa coletada");
                lore.add("");
                lore.add("&7+" + reward + " Marcos");
            } else if (today) {
                material = Material.SUNFLOWER;
                name = "&e&lᴅɪᴀ " + (day + 1);
                lore.add("&fRecompensa disponível");
                lore.add("");
                lore.add("&e+" + reward + " Marcos");
                lore.add("");
                lore.add("&aClique para coletar");
            } else {
                material = day == 6 ? Material.GOLD_BLOCK : Material.GRAY_CONCRETE;
                name = day == 6 ? "&6&lᴅɪᴀ 7" : "&7&lᴅɪᴀ " + (day + 1);
                lore.add("&7Complete os dias anteriores");
                lore.add("");
                lore.add("&8+" + reward + " Marcos");
            }

            inventory.setItem(DAY_SLOTS[day], TerrenosGUI.item(material, name, lore));
        }

        inventory.setItem(SLOT_BACK, TerrenosGUI.item(
                Material.ARROW,
                "&cVoltar",
                List.of("", "&7Clique para voltar ao menu de Marcos.")
        ));

        inventory.setItem(SLOT_PROFILE, TerrenosGUI.item(
                Material.PLAYER_HEAD,
                "&b&lᴍᴀʀᴄᴏs",
                List.of(
                        "",
                        "&fSaldo atual: &e" + marcos.getBalance(player.getUniqueId()) + " Marcos",
                        "",
                        claimedToday
                                ? "&7Você já coletou a recompensa de hoje."
                                : "&aVocê possui uma recompensa disponível."
                )
        ));

        return inventory;
    }

    public static int dayFromSlot(int slot) {
        for (int i = 0; i < DAY_SLOTS.length; i++) {
            if (DAY_SLOTS[i] == slot) return i;
        }
        return -1;
    }
}
