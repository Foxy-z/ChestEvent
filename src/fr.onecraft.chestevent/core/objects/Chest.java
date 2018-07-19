package fr.onecraft.chestevent.core.objects;

import fr.onecraft.chestevent.ChestEvent;
import fr.onecraft.chestevent.core.helpers.Configs;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
                short metadata = slot.getString("type").split(":").length > 1
                        ? Short.parseShort(slot.getString("type").split(":")[1])
                        : 0;
                String name = slot.getString("name") != null
                        ? "§f" + slot.getString("name").replace("&", "§")
                        : "";
                int amount = (slot.getString("amount") != null
                        ? slot.getInt("amount") : 1);

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

    public Menu getMenu() {
        return new Menu(plugin, id, itemList);
    }
}