package fr.onecraft.chestevent.core.objects;

import fr.onecraft.chestevent.ChestEvent;
import fr.onecraft.chestevent.core.helpers.Configs;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Menu implements InventoryHolder {
    private ChestEvent plugin;
    private int id;
    private int size;
    private int currentPage;
    private List<ItemStack> items;

    public static Material PAGE_BUTTON = Material.REDSTONE;
    public static Material SEPARATION_BUTTON = Material.REDSTONE;

    Menu(ChestEvent plugin, int id, List<ItemStack> items) {
        this.plugin = plugin;
        this.id = id;
        this.size = items.size();
        this.currentPage = 1;
        this.items = items;
    }

    public Inventory getPage(int page) {
        Inventory result = Bukkit.createInventory(this, 54, "§6§lCoffre d'événement §f§ln°" + id);
        int count = 0;
        for (ItemStack item : items) {
            if (size > 54) {
                if (count >= (page - 1) * 45 && count < page * 45)
                    result.setItem(count, item);
                result.setItem(51, getNextButton());
                result.setItem(49, getPageButton(page));
                result.setItem(47, getPreviewButton());
            } else {
                result.setItem(count, item);
            }
            count++;
        }

        return result;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public int getSize() {
        return size;
    }

    public List<ItemStack> getItems() {
        return items;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }

    public void deleteChest() {
        File file = new File(plugin.getDataFolder() + "/Chests", id + ".yml");
        file.delete();
    }

    public void saveChest() {
        Configuration configuration = Configs.get(plugin, "Chests", id);
        int slot = 1;
        for (ItemStack itemStack : getItems()) {
            configuration.createSection("items.slot" + slot);
            ConfigurationSection items = configuration.getConfigurationSection("items.slot" + slot);
            ItemMeta meta = itemStack.getItemMeta();

            // save item type and metadata
            String itemType = itemStack.getType().toString().split(":")[0];
            String metadata = ":" + itemStack.getData().getData();
            items.set("type", itemType + metadata);

            // save item amount if bigger than 1
            if (itemStack.getAmount() > 1) {
                items.set("amount", itemStack.getAmount());
            }

            // save item name if exists
            if (!meta.getDisplayName().isEmpty()) {
                items.set("name", meta.getDisplayName().replace("§", "&").substring(2));
            }

            // save lore if exists
            List<String> lore = new ArrayList<>();
            if (meta.getLore() != null) {
                lore = meta.getLore().stream()
                        .map(string -> string.replace("§", "&").substring(2))
                        .collect(Collectors.toList());
            }

            if (!lore.isEmpty()) {
                items.set("lore", lore);
            }

            // translate enchants to string
            List<String> enchants = new ArrayList<>();
            int count = 0;
            for (Enchantment enchantment : meta.getEnchants().keySet()) {
                enchants.add(enchantment.getName() + ":" + meta.getEnchants().values().toArray()[count]);
                count++;
            }
            
            // save enchants if exists
            if (!enchants.isEmpty()) {
                items.set("items." + "slot" + slot + ".enchant", enchants);
            }

            slot++;
            configuration.set("items.slot" + slot, items);
        }

        Configs.save(plugin, configuration, "Chests", id);
    }

    /*
     * Génération des boutons du menu
     */

    private static ItemStack getNextButton() {
        ItemStack itemStack = new ItemStack(PAGE_BUTTON);
        ItemMeta meta = itemStack.getItemMeta();
        meta.setDisplayName("§6§l >>> Page suivante >>> ");
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    private static ItemStack getPreviewButton() {
        ItemStack itemStack = new ItemStack(PAGE_BUTTON);
        ItemMeta meta = itemStack.getItemMeta();
        meta.setDisplayName("§6§l <<< Page précédente <<< ");
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    private static ItemStack getPageButton(int page) {
        ItemStack itemStack = new ItemStack(SEPARATION_BUTTON, 1);
        ItemMeta meta = itemStack.getItemMeta();
        meta.setDisplayName("§6§l <<< Page " + page + " >>> ");
        itemStack.setItemMeta(meta);
        return itemStack;
    }
}