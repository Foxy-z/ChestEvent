package fr.onecraft.chestevent.tabCompleter;

import fr.onecraft.chestevent.core.objects.Model;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CompleterChestEvent implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String s, String[] args) {
        if (args.length <= 1) {
            String token = args.length == 0 ? "" : args[0].toLowerCase();
            Set<String> choices = new HashSet<>();
            if (sender.hasPermission("chestevent.give")) choices.add("give");
            if (sender.hasPermission("chestevent.info")) choices.add("info");
            if (sender.hasPermission("chestevent.viewcontent")) choices.add("viewcontent");
            if (sender.hasPermission("chestevent.list")) choices.add("list");
            if (sender.hasPermission("chestevent.reload")) choices.add("reload");
            return choices.stream()
                    .filter(choice -> choice.startsWith(token))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            if (sender.hasPermission("chestevent.give")
                    || sender.hasPermission("chestevent.info")
                    || sender.hasPermission("chestevent.viewcontent")) {

                String token = args[1].toLowerCase();
                return Model.getEventList().stream()
                        .map(model -> model.getName().replace(".yml", ""))
                        .filter(name -> name.toLowerCase().startsWith(token))
                        .collect(Collectors.toList());
            }
        }
        return null;
    }
}