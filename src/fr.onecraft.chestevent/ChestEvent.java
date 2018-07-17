package fr.onecraft.chestevent;

import fr.onecraft.chestevent.commands.CmdChestEvent;
import fr.onecraft.chestevent.core.listeners.ChestListener;
import fr.onecraft.chestevent.core.listeners.PlayerListener;
import fr.onecraft.chestevent.core.objects.Model;
import fr.onecraft.chestevent.core.objects.Pager;
import fr.onecraft.chestevent.tabCompleter.CompleterChestEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChestEvent extends JavaPlugin {
    public static String PREFIX = "§9ChestEvent > §7";
    public static String ERROR = "§cErreur > §7";

    private Map<UUID, Pager> PAGER_CACHE = new HashMap<>();

    @Override
    public void onEnable() {
        PluginManager pluginManager = Bukkit.getPluginManager();
        pluginManager.registerEvents(new ChestListener(this), this);
        pluginManager.registerEvents(new PlayerListener(this), this);

        PluginCommand command = this.getCommand("chestevent");
        command.setExecutor(new CmdChestEvent(this));
        command.setTabCompleter(new CompleterChestEvent());

        generateFiles();
        removeOldFiles();
        Model.loadEventList(this);
        getLogger().info(this.getDescription().getName() + " has been enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info(this.getDescription().getName() + " has been disabled.");
    }

    private void generateFiles() {
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(new File(this.getDataFolder(), "data.yml"));
        if (configuration.get("id") == null) {
            configuration.set("id", 0);
            try {
                configuration.save(new File(this.getDataFolder(), "data.yml"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        File file = new File(this.getDataFolder() + "/Models");
        if (!file.exists()) file.mkdir();
    }

    private void removeOldFiles() {
        File[] files = new File(this.getDataFolder() + "/Chests").listFiles();
        if (files == null) return;
        Arrays.stream(files).filter(file -> {
            ConfigurationSection configuration = YamlConfiguration.loadConfiguration(file);
            long expireDate = configuration.getLong("expire-date");
            return System.currentTimeMillis() > expireDate;
        }).forEach(File::delete);
    }

    public Map<UUID, Pager> getPagers() {
        return PAGER_CACHE;
    }
}