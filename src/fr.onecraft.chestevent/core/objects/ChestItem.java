package fr.onecraft.chestevent.core.objects;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class ChestItem {
    private static Inventory inv = Bukkit.createInventory(null, 9);
    private ItemStack originalItem;
    private ItemStack inventoryItem;

    public ChestItem(ItemStack itemStack) {
        this.originalItem = itemStack;
    }

    public boolean equalsInventory(ItemStack other) {
        inv.setItem(0, originalItem);
        this.inventoryItem = inv.getItem(0);
        return inventoryItem.equals(other);
    }

    public ItemStack getOriginal() {
        return originalItem;
    }
}