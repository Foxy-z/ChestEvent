package fr.onecraft.chestevent.core.objects;

import fr.onecraft.chestevent.ChestEvent;
import fr.onecraft.chestevent.core.helpers.Configs;
import org.bukkit.Material;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class Chest {
    public final static String CHEST_NAME = "§6§lCoffre d'événement";

    private ChestEvent plugin;
    private int id;
    private String name;
    private List<ItemStack> itemList;

    public static Chest fromId(ChestEvent plugin, int id) {
        Configuration configuration = Configs.get(plugin, "Chests", id);
        if (configuration != null) {
            return new Chest(plugin, configuration, id);
        } else {
            return null;
        }
    }

    private Chest(ChestEvent plugin, ConfigurationSection config, int id) {
        this.plugin = plugin;
        this.id = id;
        this.name = config.getString("code-name");
        this.itemList = Model.loadContent(config);
    }

    private int getId() {
        return id;
    }

    private String getName() {
        return name;
    }

    public String getPermission() {
        return "chestevent.open." + getName();
    }

    public ItemStack getChestItem() {
        ItemStack itemStack = new ItemStack(Material.CHEST);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(CHEST_NAME);
        itemMeta.setLore(Arrays.asList("§7Événement: §6" + name, "§7Contenu: §6#" + id));
        itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        itemMeta.addEnchant(Enchantment.OXYGEN, 1, true);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public Menu getMenu() {
        return new Menu(plugin, id, itemList);
    }
}