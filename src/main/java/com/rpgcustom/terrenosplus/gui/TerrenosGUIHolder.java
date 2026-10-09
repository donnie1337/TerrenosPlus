package com.rpgcustom.terrenosplus.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public final class TerrenosGUIHolder implements InventoryHolder {

    public enum View { MAIN, LIST, MANAGE, DAILY }

    private final View view;
    private final int page;
    private Inventory inventory;

    public TerrenosGUIHolder(View view, int page) {
        this.view = view;
        this.page = page;
    }

    public View view() {
        return view;
    }

    public int page() {
        return page;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
