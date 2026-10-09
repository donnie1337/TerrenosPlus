package com.rpgcustom.terrenosplus.gui;

import com.rpgcustom.terrenosplus.TerrenosPlus;
import com.rpgcustom.terrenosplus.model.Terreno;
import com.rpgcustom.terrenosplus.service.MarcoManager;
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
    public static final int SLOT_MARCOS = 15;

    public static final int SLOT_EXPAND_NORTH = 10;
    public static final int SLOT_EXPAND_WEST = 12;
    public static final int SLOT_EXPLOSIONS = 13;
    public static final int SLOT_EXPAND_EAST = 14;
    public static final int SLOT_EXPAND_SOUTH = 16;
    public static final int SLOT_BACK = 22;

    private TerrenosGUI() {
    }

    public static Inventory main(Player player, TerrenoManager manager) {
        TerrenosGUIHolder holder = new TerrenosGUIHolder(TerrenosGUIHolder.View.MAIN, 0);
        Inventory inventory = Bukkit.createInventory(holder, 27, "Terrenos");
        holder.setInventory(inventory);

        List<Terreno> own = manager.getByOwner(player.getUniqueId());
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

        inventory.setItem(SLOT_MARCOS, item(
                Material.SUNFLOWER,
                "&b&lᴍᴀʀᴄᴏs",
                List.of(
                        "",
                        "&fMarcos permitem expandir",
                        "&fseus terrenos protegidos.",
                        "",
                        "&7Como conseguir:",
                        "&e▪ &fTempo jogado",
                        "&e▪ &fMissões e objetivos",
                        "&e▪ &fRecompensas diárias",
                        "",
                        "&7A cada 1 hora online:",
                        "&e25, 50 ou 75 Marcos",
                        "",
                        "&8Marcos não são comprados com Coins."
                )
        ));

        return inventory;
    }

    public static Inventory manage(Player player, Terreno terrain, MarcoManager marcos, TerrenosPlus plugin) {
        TerrenosGUIHolder holder = new TerrenosGUIHolder(TerrenosGUIHolder.View.MANAGE, 0);
        Inventory inventory = Bukkit.createInventory(holder, 27, "Gerenciar Terreno");
        holder.setInventory(inventory);

        int costPerBlock = Math.max(1, plugin.getConfig().getInt("claims.expansion.marcos-per-block", 1));
        long northSouthCost = (long) terrain.width() * costPerBlock;
        long eastWestCost = (long) terrain.depth() * costPerBlock;
        int balance = marcos.getBalance(player.getUniqueId());

        inventory.setItem(SLOT_EXPAND_NORTH, expansionItem(
                Material.ARROW, "Norte", northSouthCost, balance));
        inventory.setItem(SLOT_EXPAND_SOUTH, expansionItem(
                Material.ARROW, "Sul", northSouthCost, balance));
        inventory.setItem(SLOT_EXPAND_WEST, expansionItem(
                Material.ARROW, "Oeste", eastWestCost, balance));
        inventory.setItem(SLOT_EXPAND_EAST, expansionItem(
                Material.ARROW, "Leste", eastWestCost, balance));

        boolean explosions = terrain.explosionsEnabled();
        inventory.setItem(SLOT_EXPLOSIONS, item(
                explosions ? Material.TNT : Material.OBSIDIAN,
                "&b&lEXPLOSÕES",
                List.of(
                        "",
                        "&fControle explosões de TNT, Creepers",
                        "&fe outras fontes dentro do terreno.",
                        "",
                        "&7Estado: " + (explosions ? "&aATIVADAS" : "&cDESATIVADAS"),
                        "",
                        "&eClique para " + (explosions ? "desativar" : "ativar")
                )
        ));

        inventory.setItem(SLOT_BACK, item(
                Material.ARROW,
                "&c&lVOLTAR",
                List.of("", "&7Clique para voltar.")
        ));

        return inventory;
    }

    private static ItemStack expansionItem(Material material, String direction, long cost, int balance) {
        return item(
                material,
                "&b&lEXPANDIR " + direction.toUpperCase(),
                List.of(
                        "",
                        "&fAvança esta borda em &e1 bloco&f.",
                        "&fO custo corresponde à faixa inteira.",
                        "",
                        "&7Custo: &e" + cost + " Marcos",
                        "&7Seu saldo: &e" + balance + " Marcos",
                        "",
                        balance >= cost ? "&aClique para expandir" : "&cMarcos insuficientes"
                )
        );
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
