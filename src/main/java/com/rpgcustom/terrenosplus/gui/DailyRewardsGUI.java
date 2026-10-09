package com.rpgcustom.terrenosplus.gui;

import com.rpgcustom.terrenosplus.service.MarcoManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;

public final class DailyRewardsGUI {

    public static final int[] DAY_SLOTS = {10, 11, 12, 13, 14, 15, 16};
    public static final int SLOT_PROFILE = 22;
    public static final int SLOT_BACK = 26;

    private DailyRewardsGUI() {
    }

    public static Inventory build(Player player, MarcoManager marcos) {
        TerrenosGUIHolder holder = new TerrenosGUIHolder(TerrenosGUIHolder.View.DAILY, 0);
        Inventory inventory = Bukkit.createInventory(holder, 27, "Recompensas Diárias");
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
                material = Material.LIME_STAINED_GLASS_PANE;
                name = "&a&lDIA " + (day + 1) + " ✓";
                lore.add("&aRecompensa coletada");
            } else if (today) {
                material = Material.SUNFLOWER;
                name = "&e&lDIA " + (day + 1);
                lore.add("&fRecompensa de hoje");
                lore.add("");
                lore.add("&e+" + reward + " Marcos");
                lore.add("");
                lore.add("&aClique para coletar");
            } else {
                material = Material.GRAY_STAINED_GLASS_PANE;
                name = "&7&lDIA " + (day + 1);
                lore.add("&7Disponível futuramente");
                lore.add("");
                lore.add("&8+" + reward + " Marcos");
            }

            inventory.setItem(DAY_SLOTS[day], TerrenosGUI.item(material, name, lore));
        }

        long seconds = marcos.getSecondsUntilHourlyReward(player.getUniqueId());
        long minutes = (seconds + 59L) / 60L;

        inventory.setItem(SLOT_PROFILE, TerrenosGUI.item(
                Material.PLAYER_HEAD,
                "&b&lSEUS MARCOS",
                List.of(
                        "",
                        "&fSaldo: &e" + marcos.getBalance(player.getUniqueId()) + " Marcos",
                        "",
                        "&7Próxima recompensa por tempo:",
                        "&faprox. &e" + minutes + " minuto(s)",
                        "",
                        "&8A recompensa horária pode ser",
                        "&825, 50 ou 75 Marcos."
                )
        ));

        inventory.setItem(SLOT_BACK, TerrenosGUI.item(
                Material.ARROW,
                "&cVoltar",
                List.of("", "&7Clique para voltar ao menu de terrenos.")
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
