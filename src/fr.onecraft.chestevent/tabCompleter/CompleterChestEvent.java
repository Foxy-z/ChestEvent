package fr.onecraft.chestevent.tabCompleter;

import fr.onecraft.chestevent.ChestEvent;
import fr.onecraft.chestevent.core.objects.Model;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CompleterChestEvent implements TabCompleter {
    private ChestEvent plugin;

    public CompleterChestEvent(ChestEvent plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String s, String[] args) {
        Set<String> list = new HashSet<>();
        List<String> result;
        Player player = (Player) sender;

        /*
         * Si c'est le premier argument
         * Si le joueur a les permissions
         * Ajouter les completions
         */

        if (args.length == 1) {
            if (player.hasPermission("chestevent.give"))
                list.add("give");
            if (player.hasPermission("chestevent.info"))
                list.add("info");
            if (player.hasPermission("chestevent.viewcontent"))
                list.add("viewcontent");
            if (player.hasPermission("chestevent.list"))
                list.add("list");
            result = list.stream().filter(string -> string.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
            return result;
        }

        /*
         * Si c'est le deuxième argument
         * Si le joueur a les permissions
         * Ajouter les completions
         */

        if (args.length == 2) {
            if (player.hasPermission("chestevent.give") || player.hasPermission("chestevent.info")
                    || player.hasPermission("chestevent.viewcontent")) {
                if (Model.getEventList(plugin).size() == 0) return null;
                list = Model.getEventList(plugin).stream().map(file -> file.getName().replace(".yml", "")).collect(Collectors.toSet());
            }
            result = list.stream().filter(string -> string.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
            return result;
        }

        return null;
    }
}