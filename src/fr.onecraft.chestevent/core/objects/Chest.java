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

import java.util.Arrays;
import java.util.List;

public class Chest {

    public static final String CHEST_NAME = "§6§lCoffre d'événement";
    public static final String DIRECTORY = "chests";

    public static Chest fromId(ChestEvent plugin, int id) {
        Configuration conf = Configs.get(plugin, DIRECTORY, id);
        return conf != null ? new Chest(plugin, conf, id) : null;
    }

    private final ChestEvent plugin;
    private final int id;
    private final String name;
    private final List<ItemStack> items;
    private final long expire;

    private Chest(ChestEvent plugin, ConfigurationSection config, int id) {
        this.plugin = plugin;
        this.id = id;
        this.name = config.getString("event-name");
        this.items = Configs.loadItems(config);
        this.expire = config.getLong("expire-date");
    }

    public int getId() {
        return id;
    }

    public String getEventName() {
        return name;
    }

    public String getPermission() {
        return "chestevent.open." + name;
    }

    public ItemStack getLinkItem() {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(CHEST_NAME);
        meta.setLore(Arrays.asList("§7Événement: §6" + name, "§7Contenu: §6#" + id));
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addEnchant(Enchantment.OXYGEN, 1, true);
        item.setItemMeta(meta);
        return item;
    }

    public Menu getMenu() {
        return new Menu(plugin, id, expire, items);
    }

    public long getExpireDate() {
        return expire;
    }
}
