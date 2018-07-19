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
        List<ItemStack> items = new ArrayList<>();
        List<String> slots = new ArrayList<>(configuration.getConfigurationSection("items").getKeys(false));
        slots.forEach(item -> {
            //Pour tous les items de la config
            try {
                ConfigurationSection slot = configuration.getConfigurationSection("items." + item);

                Material type = Material.valueOf(slot.getString("type").split(":")[0]);
                //Récupération de la metadata si présente
                short metadata = slot.getString("type").split(":").length > 1
                        ? Short.parseShort(slot.getString("type").split(":")[1])
                        : 0;
                //Récupération du nom s'il y en a un + conversion & en §
                String name = slot.getString("name") != null
                        ? "§f" + slot.getString("name").replace("&", "§")
                        : "";
                //Récupération du nombre d'items, 1 par défaut
                int amount = slot.getString("amount") != null
                        ? slot.getInt("amount")
                        : 1;
                //Récupération des lignes de description
                List<String> lore = slot.getStringList("lore");

                //Ajout d'une couleur au début + conversion des & en §
                List<String> coloredLore = lore.stream()
                        .map(string -> "§7" + string.replace("&", "§"))
                        .collect(Collectors.toList());
                //Récupération de la liste des enchants
                List<String> enchants = slot.getStringList("enchant");
                ItemStack itemStack = new ItemStack(type,
                        amount,
                        metadata);

                ItemMeta meta = itemStack.getItemMeta();
                //Si un nom a été entré
                if (!name.isEmpty())
                    meta.setDisplayName(name);
                meta.setLore(coloredLore);

                //S'il y a des enchants
                if (enchants != null)
                    //Les ajouter
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