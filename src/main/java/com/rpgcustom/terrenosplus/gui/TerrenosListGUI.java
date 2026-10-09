package com.rpgcustom.terrenosplus.gui;

import com.rpgcustom.terrenosplus.model.Terreno;
import com.rpgcustom.terrenosplus.service.TerrenoManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;

public final class TerrenosListGUI {

    private static final int[] COMPACT_CONTENT_SLOTS = {0, 1, 2, 3};
    private static final int[] EXPANDED_CONTENT_SLOTS = {0, 1, 2, 3, 4};

    private TerrenosListGUI() {
    }

    public static Inventory build(Player player, TerrenoManager manager, int page) {
        List<Terreno> terrains = manager.getByOwner(player.getUniqueId());

        boolean compact = terrains.size() <= 4;
        int size = compact ? 9 : 18;
        int[] contentSlots = compact ? COMPACT_CONTENT_SLOTS : EXPANDED_CONTENT_SLOTS;

        TerrenosGUIHolder holder = new TerrenosGUIHolder(TerrenosGUIHolder.View.LIST, 0);
        Inventory inventory = Bukkit.createInventory(holder, size, "Lista de Terrenos");
        holder.setInventory(inventory);

        int visible = Math.min(terrains.size(), contentSlots.length);
        for (int i = 0; i < visible; i++) {
            Terreno terrain = terrains.get(i);
            inventory.setItem(contentSlots[i], TerrenosGUI.item(
                    Material.GRASS_BLOCK,
                    "&aTerreno #" + (i + 1),
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

        inventory.setItem(backSlot(size), TerrenosGUI.item(
                Material.ARROW,
                "&cVoltar",
                List.of("", "&7Voltar ao menu de terrenos")
        ));

        if (terrains.isEmpty()) {
            inventory.setItem(4, TerrenosGUI.item(
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

        return inventory;
    }

    public static int backSlot(int inventorySize) {
        return inventorySize <= 9 ? 8 : 17;
    }
}
