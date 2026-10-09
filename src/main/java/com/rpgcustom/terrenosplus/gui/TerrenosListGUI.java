package com.rpgcustom.terrenosplus.gui;

import com.rpgcustom.terrenosplus.model.Terreno;
import com.rpgcustom.terrenosplus.service.TerrenoManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;

public final class TerrenosListGUI {

    public static final int ITEMS_PER_PAGE = 21;
    public static final int SLOT_PREVIOUS = 38;
    public static final int SLOT_BACK = 40;
    public static final int SLOT_NEXT = 42;
    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private TerrenosListGUI() {
    }

    public static Inventory build(Player player, TerrenoManager manager, int page) {
        List<Terreno> terrains = manager.getByOwner(player.getUniqueId());
        int pages = Math.max(1, (int) Math.ceil(terrains.size() / (double) ITEMS_PER_PAGE));
        int validPage = Math.max(0, Math.min(page, pages - 1));

        TerrenosGUIHolder holder = new TerrenosGUIHolder(TerrenosGUIHolder.View.LIST, validPage);
        Inventory inventory = Bukkit.createInventory(holder, 45, "Terrenos > Página #" + (validPage + 1));
        holder.setInventory(inventory);

        int start = validPage * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            int index = start + i;
            if (index >= terrains.size()) break;

            Terreno terrain = terrains.get(index);
            inventory.setItem(CONTENT_SLOTS[i], TerrenosGUI.item(
                    Material.GRASS_BLOCK,
                    "&aTerreno #" + (index + 1),
                    List.of(
                            "",
                            "&fMundo: &e" + terrain.world(),
                            "&fÁrea: &e" + terrain.area() + " blocos",
                            "&fTamanho: &e" + terrain.width() + "x" + terrain.depth(),
                            "",
                            "&7X: &f" + terrain.minX() + " &8até &f" + terrain.maxX(),
                            "&7Z: &f" + terrain.minZ() + " &8até &f" + terrain.maxZ(),
                            "",
                            "&8ID: " + terrain.id().toString().substring(0, 8)
                    )
            ));
        }

        if (terrains.isEmpty()) {
            inventory.setItem(22, TerrenosGUI.item(
                    Material.GRAY_DYE,
                    "&7Nenhum terreno protegido",
                    List.of(
                            "",
                            "&7Você ainda não possui terrenos.",
                            "",
                            "&8Use a pá de ouro para selecionar",
                            "&8uma área e criar seu primeiro terreno."
                    )
            ));
        }

        if (validPage > 0) {
            inventory.setItem(SLOT_PREVIOUS, TerrenosGUI.item(
                    Material.ARROW, "&aAnterior", List.of("", "&7Página anterior")
            ));
        }

        inventory.setItem(SLOT_BACK, TerrenosGUI.item(
                Material.ARROW, "&cVoltar", List.of("", "&7Voltar ao menu de terrenos")
        ));

        if (validPage + 1 < pages) {
            inventory.setItem(SLOT_NEXT, TerrenosGUI.item(
                    Material.ARROW, "&aPróxima", List.of("", "&7Próxima página")
            ));
        }

        return inventory;
    }
}
