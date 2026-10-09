package com.rpgcustom.terrenosplus.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public final class MarcosGUIHolder implements InventoryHolder {

    public enum View { MAIN, DAILY }

    private final View view;
    private Inventory inventory;

    public MarcosGUIHolder(View view) {
        this.view = view;
    }

    public View view() {
        return view;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
