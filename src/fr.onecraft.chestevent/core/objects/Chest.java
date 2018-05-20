package fr.onecraft.chestevent.core.objects;

import fr.onecraft.chestevent.ChestEvent;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Chest {
    private ChestEvent plugin;
    private int id;
    private String code;
    private List<ItemStack> itemList;

    public static Chest fromId(ChestEvent plugin, int id) {
        try {
            YamlConfiguration configuration = new YamlConfiguration();
            File file = new File(plugin.getDataFolder() + "/Chests", id + ".yml");
            FileInputStream fileinputstream = new FileInputStream(file);
            configuration.load(new InputStreamReader(fileinputstream, Charset.forName("UTF-8")));
            return new Chest(plugin, configuration, file);
        } catch (InvalidConfigurationException | IOException e) {
            return null;
        }
    }

    private Chest(ChestEvent plugin, ConfigurationSection config, File file) {
        this.plugin = plugin;
        this.id = Integer.parseInt(file.getName().replace(".yml", ""));
        this.code = config.getString("code-name");
        this.itemList = loadContent(config);
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

    private List<ItemStack> loadContent(ConfigurationSection configuration) {
        List<String> slots;
        List<ItemStack> items = new ArrayList<>();
        slots = new ArrayList<>(configuration.getConfigurationSection("items").getKeys(false));
        slots.forEach(item -> {
            try {
                ConfigurationSection slot = configuration.getConfigurationSection("items." + item);
                Material type = Material.valueOf(slot.getString("type").split(":")[0]);
                short metadata = slot.getString("type").split(":").length > 1 ? Short.parseShort(slot.getString("type").split(":")[1]) : 0;
                String name = slot.getString("name") != null ? "§f" + slot.getString("name").replace("&", "§") : "";
                int amount = (slot.getString("amount") != null ? slot.getInt("amount") : 1);
                List<String> lore = slot.getStringList("lore");
                List<String> coloredLore;
                coloredLore = lore.stream().map(string -> "§7" + string.replace("&", "§")).collect(Collectors.toList());
                List<String> enchants = slot.getStringList("enchant");
                ItemStack itemStack = new ItemStack(type, amount, metadata);
                ItemMeta meta = itemStack.getItemMeta();
                if (!name.equalsIgnoreCase("")) meta.setDisplayName(name);
                meta.setLore(coloredLore);
                if (enchants != null)
                    enchants.forEach(enchant -> meta.addEnchant(Enchantment.getByName(enchant.split(":")[0]), Integer.parseInt(enchant.split(":")[1]), true));
                itemStack.setItemMeta(meta);
                items.add(itemStack);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return items;
    }

    public Menu getMenu() {
        return new Menu(plugin, id, itemList);
    }
}