package com.rpgcustom.terrenosplus.gui;

import com.rpgcustom.terrenosplus.model.Terreno;
import com.rpgcustom.terrenosplus.service.TerrenoManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class TerrenosGUI {

    public static final int SLOT_LIST = 11;
    public static final int SLOT_MANAGE = 13;
    public static final int SLOT_BLOCKS = 15;

    private TerrenosGUI() {
    }

    public static Inventory main(Player player, TerrenoManager manager) {
        TerrenosGUIHolder holder = new TerrenosGUIHolder(TerrenosGUIHolder.View.MAIN, 0);
        Inventory inventory = Bukkit.createInventory(holder, 36, "Terrenos");
        holder.setInventory(inventory);

        List<Terreno> own = manager.getByOwner(player.getUniqueId());
        long protectedBlocks = own.stream().mapToLong(Terreno::area).sum();

        inventory.setItem(SLOT_LIST, item(
                Material.COMPASS,
                "&b&lʟɪsᴛᴀ ᴅᴇ ᴛᴇʀʀᴇɴᴏs",
                List.of(
                        "",
                        "&fVisualize todos os seus",
                        "&fterrenos protegidos",
                        "",
                        "&fSeus terrenos: &e" + own.size(),
                        "",
                        "&aClique para ver seus terrenos"
                )
        ));

        inventory.setItem(SLOT_MANAGE, item(
                Material.COMMAND_BLOCK,
                "&b&lɢᴇʀᴇɴᴄɪᴀʀ ᴛᴇʀʀᴇɴᴏ",
                List.of(
                        "",
                        "&fModifique permissões e controle",
                        "&fquem pode acessar seu terreno",
                        "",
                        "&aClique para gerenciar o terreno"
                )
        ));

        inventory.setItem(SLOT_BLOCKS, item(
                Material.GRASS_BLOCK,
                "&b&lʙʟᴏᴄᴏs ᴅᴇ ᴛᴇʀʀᴇɴᴏ",
                List.of(
                        "",
                        "&fBlocos de terreno permitem",
                        "&fexpandir suas áreas seguras",
                        "",
                        "&7Como conseguir:",
                        "&e▪ &fComprando no &e/shop",
                        "&e▪ &fFicando online no servidor",
                        "&e▪ &fAbrindo caixas em &e/caixas",
                        "",
                        "&fSeus blocos protegidos: &e" + protectedBlocks,
                        "",
                        "&aClique para abrir a loja"
                )
        ));

        return inventory;
    }

    static ItemStack item(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(name));
            meta.setLore(lore.stream().map(TerrenosGUI::color).toList());
            stack.setItemMeta(meta);
        }
        return stack;
    }

    static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }
}
