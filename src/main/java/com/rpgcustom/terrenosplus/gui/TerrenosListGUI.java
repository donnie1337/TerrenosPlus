package com.rpgcustom.terrenosplus.gui;

import com.rpgcustom.terrenosplus.model.Terreno;
import com.rpgcustom.terrenosplus.service.TerrenoManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class TerrenosListGUI {

    private static final int[] CONTENT_SLOTS = {10, 11, 12, 13, 14};

    private TerrenosListGUI() {
    }

    public static Inventory build(Player player, TerrenoManager manager, int page) {
        List<Terreno> terrains = new ArrayList<>(manager.getByOwner(player.getUniqueId()));
        Comparator<Terreno> byCreatedAt = Comparator.comparingLong(Terreno::createdAt);
        if ("antigo".equalsIgnoreCase(terrainSort(player))) {
            terrains.sort(byCreatedAt);
        } else {
            terrains.sort(byCreatedAt.reversed());
        }

        int size = 36;

        TerrenosGUIHolder holder = new TerrenosGUIHolder(TerrenosGUIHolder.View.LIST, 0);
        Inventory inventory = Bukkit.createInventory(holder, size, "Lista de Terrenos");
        holder.setInventory(inventory);

        int visible = Math.min(terrains.size(), CONTENT_SLOTS.length);
        for (int i = 0; i < visible; i++) {
            Terreno terrain = terrains.get(i);
            inventory.setItem(CONTENT_SLOTS[i], TerrenosGUI.item(
                    displayWorldIcon(terrain.world()),
                    "&aTerreno #" + (i + 1),
                    List.of(
                            "",
                            "&fMundo: " + displayWorldName(terrain.world()),
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

    private static String terrainSort(Player player) {
        org.bukkit.plugin.Plugin utilidades = Bukkit.getPluginManager().getPlugin("UtilidadesPlus");
        if (utilidades == null || !utilidades.isEnabled()) return "recente";
        try {
            Object result = utilidades.getClass()
                    .getMethod("terrainSort", Player.class)
                    .invoke(utilidades, player);
            return result instanceof String value ? value : "recente";
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return "recente";
        }
    }

    private static String displayWorldName(String worldName) {
        if (worldName == null || worldName.isBlank()) return "&7Desconhecido";

        return switch (worldName.toLowerCase(java.util.Locale.ROOT)) {
            case "world", "overworld", "minecraft:overworld" -> "&aOverworld";
            case "world_nether", "nether", "minecraft:the_nether" -> "&cNether";
            case "world_the_end", "the_end", "end", "minecraft:the_end" -> "&5The End";
            case "mining", "mineracao", "mineração" -> "&eMineração";
            default -> "&f" + Character.toUpperCase(worldName.charAt(0)) + worldName.substring(1);
        };
    }

    private static Material displayWorldIcon(String worldName) {
        if (worldName == null || worldName.isBlank()) return Material.GRASS_BLOCK;

        return switch (worldName.toLowerCase(java.util.Locale.ROOT)) {
            case "world_nether", "nether", "minecraft:the_nether" -> Material.NETHERRACK;
            case "world_the_end", "the_end", "end", "minecraft:the_end" -> Material.END_STONE;
            case "mining", "mineracao", "mineração" -> Material.DEEPSLATE_DIAMOND_ORE;
            default -> Material.GRASS_BLOCK;
        };
    }

    public static int backSlot(int inventorySize) {
        return 31;
    }
}
