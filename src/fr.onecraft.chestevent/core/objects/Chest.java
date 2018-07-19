package fr.onecraft.chestevent.core.objects;

import fr.onecraft.chestevent.ChestEvent;
import fr.onecraft.chestevent.core.helpers.Configs;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.List;

public class Chest {
    private ChestEvent plugin;
    private int id;
    private String code;
    private List<ItemStack> itemList;

    public static Chest fromId(ChestEvent plugin, int id) {
        ConfigurationSection configuration = Configs.get(plugin, "Chests", id + "");
        return new Chest(plugin, configuration, id);
    }

    private Chest(ChestEvent plugin, ConfigurationSection config, int id) {
        this.plugin = plugin;
        this.id = id;
        this.code = config.getString("code-name");
        this.itemList = Model.loadContent(config);
    }

    private int getId() {
        return id;
    }

    private String getCode() {
        return code;
    }

    public String getPermission() {
        return "chestevent.open." + getCode();
    }

    public ItemStack getChestItem() {
        ItemStack itemStack = new ItemStack(Material.CHEST);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName("§6§lCoffre d'événement §f§ln°" + getId());
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public void delete() {
        File file = new File(plugin.getDataFolder() + "/Chests", id + ".yml");
        file.delete();
    }

    public Menu getMenu() {
        return new Menu(plugin, id, itemList);
    }
}