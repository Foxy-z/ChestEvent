package fr.onecraft.chestevent.core.objects;

import fr.onecraft.chestevent.ChestEvent;
import fr.onecraft.chestevent.core.helpers.Configs;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.Configuration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.List;

public class Menu implements InventoryHolder {

    // must be between 1 and 5 included
    private static final int ITEM_ROWS = 3;

    public static final int ITEMS_PER_PAGE = ITEM_ROWS * 9;

    private final ChestEvent plugin;
    private final int id;
    private final List<ItemStack> items;

    private int currentPage;

    Menu(ChestEvent plugin, int id, List<ItemStack> items) {
        this.plugin = plugin;
        this.id = id;
        this.currentPage = 1;
        this.items = items;
    }

    public Inventory getView() {
        Inventory inv = Bukkit.createInventory(this, (ITEM_ROWS + 1) * 9, "§6§lCoffre d'événement §f§ln°" + id);

        int start = (currentPage - 1) * ITEMS_PER_PAGE;
        int end = Math.min(currentPage * ITEMS_PER_PAGE, items.size());
        for (int i = start; i < end; i++) {
            inv.setItem(i - start, items.get(i));
        }

        inv.setItem(Menu.ITEMS_PER_PAGE, getPreviousButton());
        inv.setItem(Menu.ITEMS_PER_PAGE + 4, getPageButton(currentPage));
        inv.setItem(Menu.ITEMS_PER_PAGE + 8, getNextButton());

        return inv;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void updatePageIndex(int diff) {
        this.currentPage = currentPage + diff;
    }

    public int getMaxPage() {
        return (int) Math.ceil((double) items.size() / Menu.ITEMS_PER_PAGE);
    }

    public List<ItemStack> getItems() {
        return items;
    }

    public int getChestId() {
        return id;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }

    public void deleteChest() {
        // noinspection ResultOfMethodCallIgnored
        new File(plugin.getDataFolder() + "/" + Chest.DIRECTORY, id + ".yml").delete();
    }

    public void saveChest() {
        Configuration conf = Configs.get(plugin, Chest.DIRECTORY, id);
        Configs.dumpItems(conf, items);
        Configs.save(plugin, conf, Chest.DIRECTORY, id);
    }

    /*
     * Génération des boutons du menu
     */

    private ItemStack getNextButton() {
        ItemStack item = new ItemStack(Material.REDSTONE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6§l >>> Page suivante >>> ");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack getPreviousButton() {
        ItemStack item = new ItemStack(Material.REDSTONE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6§l <<< Page précédente <<< ");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack getPageButton(int page) {
        ItemStack item = new ItemStack(Material.DOUBLE_PLANT);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6§l <<< Page " + page + " >>> ");
        item.setItemMeta(meta);
        item.setAmount(page);
        return item;
    }
}
