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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Model {
    private static ChestEvent plugin;
    private String eventName;
    private String code;
    private String description;
    private List<ItemStack> itemList;

    public static Model fromName(ChestEvent plugin, String name) {
        try {
            YamlConfiguration configuration = new YamlConfiguration();
            File file = new File(plugin.getDataFolder() + "/Models", name + ".yml");
            FileInputStream fileinputstream = new FileInputStream(file);
            configuration.load(new InputStreamReader(fileinputstream, Charset.forName("UTF-8")));
            return new Model(plugin, configuration);
        } catch (InvalidConfigurationException | IOException e) {
            return null;
        }
    }

    private Model(ChestEvent plugin, ConfigurationSection config) {
        this.plugin = plugin;
        this.eventName = config.getString("display-name");
        this.code = config.getString("code-name");
        this.description = config.getString("description");
        this.itemList = loadContent(config);
    }

    private String getCode() {
        return code;
    }

    public String getPermission() {
        return "chestevent.open." + getCode();
    }

    public String getDescription() {
        return description;
    }

    public List<ItemStack> getContent() {
        return itemList;
    }

    public Chest createChest() {
        File file = new File(plugin.getDataFolder() + "/Models", eventName + ".yml");
        YamlConfiguration configuration = new YamlConfiguration();
        YamlConfiguration data = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "data.yml"));
        data.set("id", data.getInt("id") + 1);
        try {
            FileInputStream fileinputstream = new FileInputStream(file);
            configuration.load(new InputStreamReader(fileinputstream, Charset.forName("UTF-8")));
            configuration.set("expire-date", System.currentTimeMillis() + 345600000);
            configuration.save(new File(plugin.getDataFolder() + "/Chests", data.getInt("id") + ".yml"));
            data.save(new File(plugin.getDataFolder(), "data.yml"));
            return Chest.fromId(plugin, data.getInt("id"));
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
            return null;
        }
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

    public static boolean eventExists(String eventName) {
        File file = new File(plugin.getDataFolder() + "/Models", eventName + ".yml");
        return file.exists();
    }

    public static List<File> getEventList() {
        if (new File(plugin.getDataFolder() + "/Models").listFiles() == null) return new ArrayList<>();
        return Arrays.asList(new File(plugin.getDataFolder() + "/Models").listFiles());
    }
}