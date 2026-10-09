package com.rpgcustom.terrenosplus.gui;

import com.rpgcustom.terrenosplus.model.Terreno;
import com.rpgcustom.terrenosplus.service.TerrenoManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;

public final class TerrenosListGUI {

    private static final int[] CONTENT_SLOTS = {10, 11, 12, 13, 14};

    private TerrenosListGUI() {
    }

    public static Inventory build(Player player, TerrenoManager manager, int page) {
        List<Terreno> terrains = manager.getByOwner(player.getUniqueId());

        int size = 36;

        TerrenosGUIHolder holder = new TerrenosGUIHolder(TerrenosGUIHolder.View.LIST, 0);
        Inventory inventory = Bukkit.createInventory(holder, size, "Lista de Terrenos");
        holder.setInventory(inventory);

        int visible = Math.min(terrains.size(), CONTENT_SLOTS.length);
        for (int i = 0; i < visible; i++) {
            Terreno terrain = terrains.get(i);
            inventory.setItem(CONTENT_SLOTS[i], TerrenosGUI.item(
                    Material.GRASS_BLOCK,
                    "&aTerreno #" + (i + 1),
                    List.of(
                            "",
                            "&fMundo: &a" + displayWorldName(terrain.world()),
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
            inventory.setItem(13, TerrenosGUI.item(
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

    private static String displayWorldName(String worldName) {
        if (worldName == null || worldName.isBlank()) return "Desconhecido";
        if ("world".equalsIgnoreCase(worldName)
                || "minecraft:overworld".equalsIgnoreCase(worldName)
                || "overworld".equalsIgnoreCase(worldName)) {
            return "Overworld";
        }
        return worldName;
    }

    public static int backSlot(int inventorySize) {
        return 31;
    }
}
