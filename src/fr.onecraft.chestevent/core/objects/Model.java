package fr.onecraft.chestevent.core.objects;

import fr.onecraft.chestevent.ChestEvent;
import fr.onecraft.chestevent.core.helpers.Configs;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Model {
    private ChestEvent plugin;
    private String eventName;
    private String description;
    private List<ItemStack> itemList;
    private static List<Model> MODEL_LIST = new ArrayList<>();

    public static Model fromName(ChestEvent plugin, String name) {
        Configuration configuration = Configs.get(plugin, "Models", name);
        if (!(configuration == null)) {
            return new Model(plugin, configuration, name);
        } else {
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
        Configuration chestConfig = Configs.get(plugin, "Models", eventName);
        chestConfig.set("expire-date", System.currentTimeMillis() + 345600000);
        int id = getAndIncrementId();
        Configs.save(plugin, chestConfig, "Chests", id);
        return Chest.fromId(plugin, id);
    }

    private int getAndIncrementId() {
        Configuration data = Configs.get(plugin, "", "data");
        data.set("id", data.getInt("id") + 1);
        Configs.save(plugin, data, "", "data");
        return data.getInt("id");
    }

    public static List<ItemStack> loadContent(ConfigurationSection configuration) {
        List<ItemStack> items = new ArrayList<>();
        List<String> slots = new ArrayList<>(configuration.getConfigurationSection("items").getKeys(false));
        slots.forEach((String item) -> {
            ConfigurationSection slot = configuration.getConfigurationSection("items." + item);

            // get item type
            Material type = Material.valueOf(slot.getString("type").split(":")[0]);

            // get item amount
            int amount = slot.getInt("amount");
            if (amount <= 0) {
                amount = 1;
            }

            // get metadata
            short metadata = 0;
            // if metadata is set in config
            if (slot.getString("type").split(":").length > 1) {
                metadata = Short.parseShort(slot.getString("type").split(":")[1]);
            }

            ItemStack itemStack = new ItemStack(type, amount, metadata);
            ItemMeta meta = itemStack.getItemMeta();

            // set name
            String name = slot.getString("name");
            if (name != null) {
                meta.setDisplayName("§f" + ChatColor.translateAlternateColorCodes('&', name));
            }

            // add lore
            List<String> loreList = new ArrayList<>();
            slot.getStringList("lore").stream()
                    .map(lore -> "§7" + ChatColor.translateAlternateColorCodes('&', lore))
                    .forEach(loreList::add);
            meta.setLore(loreList);

            // add enchants
            slot.getStringList("enchant").stream()
                    .forEach(enchant -> meta.addEnchant(
                            Enchantment.getByName(enchant.split(":")[0]),
                            Integer.parseInt(enchant.split(":")[1]),
                            true
                    ));

            itemStack.setItemMeta(meta);
            items.add(itemStack);
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
                if (file.getName().endsWith(".yml")) {
                    String fileName = file.getName().replace(".yml", "");
                    Model model = fromName(plugin, fileName);
                    if (model != null && isValidName(fileName)) {
                        MODEL_LIST.add(fromName(plugin, fileName));
                    }
                }
            }
        });
    }

    public static List<Model> getEventList() {
        return MODEL_LIST;
    }
}