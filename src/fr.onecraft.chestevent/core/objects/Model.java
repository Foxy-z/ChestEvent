package fr.onecraft.chestevent.core.objects;

import fr.onecraft.chestevent.ChestEvent;
import fr.onecraft.chestevent.core.helpers.Configs;
import org.bukkit.Bukkit;
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

public class Model {
    private ChestEvent plugin;
    private String eventName;
    private String description;
    private List<ItemStack> itemList;
    private static List<Model> MODEL_LIST = new ArrayList<>();

    public static Model fromName(ChestEvent plugin, String name) {
        try {
            YamlConfiguration configuration = new YamlConfiguration();
            File file = new File(plugin.getDataFolder() + "/Models", name + ".yml");
            FileInputStream fileinputstream = new FileInputStream(file);
            configuration.load(new InputStreamReader(fileinputstream, Charset.forName("UTF-8")));
            return new Model(plugin, configuration, name);
        } catch (InvalidConfigurationException | IOException e) {
            return null;
        }
    }

    private Model(ChestEvent plugin, ConfigurationSection config, String eventName) {
        this.plugin = plugin;
        this.eventName = eventName;
        this.description = config.getString("description");
        this.itemList = loadContent(config);
    }

    private String getCode() {
        return eventName.toLowerCase();
    }

    public String getName() {
        return eventName;
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

    public static boolean isValidName(String name) {
        return name.matches("^[a-zA-Z0-9_-]{3,30}$");
    }

    public Chest createChest() {
        YamlConfiguration chestConfig = new YamlConfiguration();
        ConfigurationSection data = Configs.get(plugin, "", "data");
        data.set("id", data.getInt("id") + 1);
        chestConfig.set("", Configs.get(plugin, "Models", eventName));
        chestConfig.set("expire-date", System.currentTimeMillis() + 345600000);
        Configs.save(plugin, chestConfig, "Chests", data.getInt("id") + "");
        Configs.save(plugin, data, "", "data");
        return Chest.fromId(plugin, data.getInt("id"));
    }

    private List<ItemStack> loadContent(ConfigurationSection configuration) {
        List<ItemStack> items = new ArrayList<>();
        List<String> slots = new ArrayList<>(configuration.getConfigurationSection("items").getKeys(false));
        slots.forEach(item -> {
            try {
                ConfigurationSection slot = configuration.getConfigurationSection("items." + item);
                Material type = Material.valueOf(slot.getString("type").split(":")[0]);

                short metadata = slot.getString("type").split(":").length > 1
                        ? Short.parseShort(slot.getString("type").split(":")[1])
                        : 0;

                String name = slot.getString("name") != null
                        ? "§f" + slot.getString("name").replace("&", "§")
                        : "";

                int amount = (slot.getString("amount") != null
                        ? slot.getInt("amount")
                        : 1);

                List<String> lore = slot.getStringList("lore");
                List<String> coloredLore = lore.stream()
                        .map(string -> "§7" + string.replace("&", "§"))
                        .collect(Collectors.toList());

                List<String> enchants = slot.getStringList("enchant");
                ItemStack itemStack = new ItemStack(type,
                        amount,
                        metadata);

                ItemMeta meta = itemStack.getItemMeta();
                if (!name.isEmpty())
                    meta.setDisplayName(name);

                meta.setLore(coloredLore);
                if (enchants != null)
                    enchants.forEach(enchant -> meta.addEnchant(Enchantment.getByName(enchant.split(":")[0]),
                            Integer.parseInt(enchant.split(":")[1]),
                            true));

                itemStack.setItemMeta(meta);
                items.add(itemStack);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return items;
    }

    public static boolean eventExists(String eventName, ChestEvent plugin) {
        File file = new File(plugin.getDataFolder() + "/Models", eventName + ".yml");
        return file.exists();
    }

    public static void loadEventList(ChestEvent plugin) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            File path = new File(plugin.getDataFolder() + "/Models");
            if (!path.exists()) return;
            MODEL_LIST.clear();
            for (File file : path.listFiles()) {
                String fileName = file.getName().replace(".yml", "");
                Model model = fromName(plugin, fileName);
                if (model != null && isValidName(fileName))
                    MODEL_LIST.add(fromName(plugin, fileName));
            }
        });
    }

    public static List<Model> getEventList() {
        return MODEL_LIST;
    }
}