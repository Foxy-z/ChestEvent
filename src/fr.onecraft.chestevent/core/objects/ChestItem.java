package fr.onecraft.chestevent.core.objects;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class ChestItem {
    private static Inventory inv = Bukkit.createInventory(null, 9);
    private final ItemStack originalItem;
    private final ItemStack inventoryItem;

    public ChestItem(ItemStack itemStack) {
        this.originalItem = itemStack;

        inv.setItem(0, itemStack);
        this.inventoryItem = inv.getItem(0);
    }

    public boolean equalsInventory(ItemStack other) {
        return inventoryItem.equals(other);
    }

    public ItemStack getOriginal() {
        return originalItem;
    }
}