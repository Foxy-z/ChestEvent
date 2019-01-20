package fr.onecraft.chestevent.core.objects;

import fr.onecraft.chestevent.ChestEvent;
import fr.onecraft.chestevent.core.helpers.Configs;
import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Model {

    public static final String DIRECTORY = "models";
    private static final Map<String, Model> MODELS = new HashMap<>();

    public static Set<String> getAllNames() {
        return MODELS.keySet();
    }

    public static Model get(String name) {
        return MODELS.get(name);
    }

    public static void reloadAll(ChestEvent plugin) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            File[] files = new File(plugin.getDataFolder() + "/" + DIRECTORY).listFiles();
            if (files == null) return;

            Map<String, Model> models = Arrays.stream(files)
                    .filter(file -> file.getName().endsWith(".yml"))
                    .map(file -> file.getName().replace(".yml", ""))
                    .filter(Model::isValidName)
                    .map(file -> fromName(plugin, file))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(Model::getName, m -> m));

            MODELS.clear();
            MODELS.putAll(models);

        });
    }

    private static boolean isValidName(String name) {
        return name.matches("^[a-z0-9-]{3,30}$");
    }

    private static Model fromName(ChestEvent plugin, String name) {
        Configuration conf = Configs.get(plugin, DIRECTORY, name);
        return conf != null ? new Model(plugin, conf, name) : null;
    }

    // ---------------------

    private final ChestEvent plugin;
    private final String eventName;
    private final String description;

    private final List<ChestItem> itemList;

    private Model(ChestEvent plugin, ConfigurationSection config, String eventName) {
        this.plugin = plugin;
        this.eventName = eventName;
        this.description = config.getString("description");
        this.itemList = Configs.loadItems(config);
    }

    private String getName() {
        return eventName;
    }

    public String getPermission() {
        return "chestevent.open." + eventName.toLowerCase();
    }

    public String getDescription() {
        return description;
    }

    public List<ChestItem> getContent() {
        return itemList;
    }

    public Chest createChest() {
        Configuration chestConfig = Configs.get(plugin, Model.DIRECTORY, eventName);
        if (chestConfig == null) return null;
        chestConfig.set("event-name", eventName);
        chestConfig.set("expire-date", System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30));
        int id = getAndIncrementId();
        if (id == -1) return null;
        Configs.save(plugin, chestConfig, Chest.DIRECTORY, id);
        return Chest.fromId(plugin, id);
    }

    private int getAndIncrementId() {
        Configuration data = Configs.get(plugin, "data");
        if (data == null || data.getInt("id", -1) == -1) return -1;
        data.set("id", data.getInt("id") + 1);
        Configs.save(plugin, data, "data");
        return data.getInt("id");
    }

}
