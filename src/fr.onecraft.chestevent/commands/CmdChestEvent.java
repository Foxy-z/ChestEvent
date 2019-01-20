package fr.onecraft.chestevent.commands;

import fr.onecraft.chestevent.ChestEvent;
import fr.onecraft.chestevent.core.objects.Chest;
import fr.onecraft.chestevent.core.objects.Model;
import fr.onecraft.chestevent.core.objects.Pager;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.*;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CmdChestEvent implements CommandExecutor {
    private ChestEvent plugin;

    public CmdChestEvent(ChestEvent plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        String action = args[0].toLowerCase();
        if (action.startsWith(":") && (sender instanceof Player)) {
            showPage(((Player) sender), action);
        } else if (!sender.hasPermission("chestevent." + action)) {
            sender.sendMessage(ChestEvent.ERROR + "Tu n'as pas la permission.");
        } else if (action.equals("list")) {
            showEventList(sender);
        } else if (action.equals("reload")) {
            reloadCache(sender);
        } else if (args.length < 2) {
            showHelp(sender);
        } else {
            String event = args[1];
            if (action.equals("getlink")) {
                getLink(plugin, sender, args);
            } else if (Model.get(event) == null) {
                sender.sendMessage(ChestEvent.ERROR + "Cet événement n'existe pas.");
            } else if (action.equals("info")) {
                info(sender, event);
            } else if (action.equals("give")) {
                give(sender, event, args);
            } else if (action.equals("viewcontent")) {
                viewContent(sender, event);
            } else {
                showHelp(sender);
            }
        }
        return true;
    }

    private void showHelp(CommandSender sender) {
        List<String> cmds = new ArrayList<>();

        if (sender.hasPermission("chestevent.info"))
            cmds.add("§b/chestevent info <event>§7 informations sur un événement");
        if (sender.hasPermission("chestevent.viewcontent"))
            cmds.add("§b/chestevent viewcontent <event>§7 contenu d'un événement");
        if (sender.hasPermission("chestevent.list"))
            cmds.add("§b/chestevent list §7liste des événements");
        if (sender.hasPermission("chestevent.give"))
            cmds.add("§b/chestevent give <event> [pseudo]§7 give un coffre d'événement");
        if (sender.hasPermission("chestevent.getlink"))
            cmds.add("§b/chestevent getlink <id> [pseudo] §7donne un lien vers un autre coffre");
        if (sender.hasPermission("chestevent.reload"))
            cmds.add("§b/chestevent reload §7met à jour les événements");

        if (cmds.isEmpty()) {
            sender.sendMessage(ChestEvent.ERROR + "Tu n'as pas la permission.");
        } else {
            sender.sendMessage(ChestEvent.PREFIX + "Gère les récompenses d'événements");
            cmds.forEach(sender::sendMessage);
        }
    }

    private void showPage(Player player, String arg) {
        int page;
        try {
            // parse the string to a number
            page = Integer.parseInt(arg.substring(1));
        } catch (NumberFormatException e) {
            player.sendMessage(ChestEvent.ERROR + "Cette page n'existe pas.");
            return;
        }

        // get pager from cache
        Pager pager = plugin.getPagers().get((player).getUniqueId());

        // return an error if the page doesn't exists
        if (page > pager.getMaxPage() || page <= 0) {
            player.sendMessage(ChestEvent.ERROR + "Cette page n'existe pas.");
            return;
        }

        // update the current page
        pager.setCurrentPage(page);

        // add the first line with prefix + event name + page selector
        TextComponent message = new TextComponent(ChestEvent.PREFIX + "Contenu de §a" + pager.getEvent() + "§7: ");
        message.setColor(ChatColor.GRAY);
        message.addExtra(getPageSelector(pager));

        // send messages
        player.sendMessage("\n");
        player.spigot().sendMessage(message);
        pager.getView().forEach(msg -> player.spigot().sendMessage(msg));

    }

    private void showEventList(CommandSender sender) {
        if (Model.getAllNames().isEmpty()) {
            sender.sendMessage(ChestEvent.PREFIX + "Il n'y a aucun événement.");
        } else {
            sender.sendMessage(ChestEvent.PREFIX + "Liste des événements:");
            Model.getAllNames().stream().map(model -> " §7– §b" + model).forEach(sender::sendMessage);
        }
    }

    private void reloadCache(CommandSender sender) {
        Model.reloadAll(plugin);
        sender.sendMessage(ChestEvent.PREFIX + "Les modèles ont été chargés.");
    }

    private void info(CommandSender sender, String event) {
        Model model = Model.get(event);
        sender.sendMessage(ChestEvent.PREFIX + "Informations sur l'événement §a" + event + "§7: \n"
                + " §8- §7Description: §b" + model.getDescription() + "\n"
                + " §8- §7Permission: §b" + model.getPermission() + "\n"
        );
    }

    private void give(CommandSender sender, String event, String[] args) {
        Player target;
        boolean self;

        if (args.length > 2) {
            target = Bukkit.getPlayerExact(args[2]);
            self = false;
        } else if (sender instanceof Player) {
            target = ((Player) sender);
            self = true;
        } else {
            showHelp(sender);
            return;
        }

        if (target == null) {
            sender.sendMessage(ChestEvent.ERROR + "§6" + args[2] + " §7est introuvable.");
            return;
        }

        if (target.getInventory().firstEmpty() == -1) {
            sender.sendMessage(ChestEvent.ERROR + (self ? "Votre" : "Son") + " inventaire est plein.");
            return;
        }

        Model model = Model.get(event);
        Chest chest = model.createChest();
        target.getInventory().addItem(chest.getLinkItem());
        target.sendMessage(ChestEvent.PREFIX + "Vous avez reçu le coffre de l'événement §a" + event + "§7.");
        if (!self) {
            sender.sendMessage(ChestEvent.PREFIX + "§a" + target.getName() + " §7a reçu le coffre de l'événement §a" + event + "§7.");
        }

        plugin.logToFile("GIVE", sender.getName() + " gave " + chest.getEventName() + " to " + target.getName() + " (ChestID: " + chest.getId() + ", total items: " + model.getContent().size() + ")");
    }

    private void getLink(ChestEvent plugin, CommandSender sender, String[] args) {
        Player target;
        boolean self;

        if (args.length > 2) {
            target = Bukkit.getPlayerExact(args[2]);
            self = false;
        } else if (sender instanceof Player) {
            target = ((Player) sender);
            self = true;
        } else {
            showHelp(sender);
            return;
        }

        if (target == null) {
            sender.sendMessage(ChestEvent.ERROR + "§6" + args[2] + " §7est introuvable.");
            return;
        }

        if (target.getInventory().firstEmpty() == -1) {
            sender.sendMessage(ChestEvent.ERROR + (self ? "Votre" : "Son") + " inventaire est plein.");
            return;
        }

        int id;
        try {
            id = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChestEvent.ERROR + "§6" + args[1] + " §7n'est pas un nombre valide.");
            return;
        }

        Chest chest = Chest.fromId(plugin, id);
        if (chest == null) {
            sender.sendMessage(ChestEvent.ERROR + "Ce coffre est introuvable.");
            return;
        }

        target.getInventory().addItem(chest.getLinkItem());
        target.sendMessage(ChestEvent.PREFIX + "Vous avez reçu le coffre de l'événement §a" + chest.getEventName() + "§7.");
        if (!self) {
            sender.sendMessage(ChestEvent.PREFIX + "§a" + target.getName() + " §7a reçu le coffre de l'événement §a" + chest.getEventName() + "§7.");
        }

        plugin.logToFile("GIVE_LINK", sender.getName() + " gave " + chest.getEventName() + " to " + target.getName() + " (ChestID: " + chest.getId() + ", total items: " + chest.getItems().size() + ")");
    }

    private void viewContent(CommandSender sender, String event) {
        Model model = Model.get(event);
        List<ItemStack> items = model.getContent();
        List<TextComponent> messages = new ArrayList<>();

        for (ItemStack itemStack : items) {
            ItemMeta meta = itemStack.getItemMeta();
            String displayName = itemStack.getItemMeta().getDisplayName();

            // add item type + item amount + display name
            TextComponent component = new TextComponent("§8- §b" + itemStack.getType() + " x" + itemStack.getAmount() + " §7" + (displayName == null ? "" : displayName));

            // add lore
            String lore = "";
            if (meta.getLore() != null) {
                lore = meta.getLore().stream()
                        .map(desc -> "\n" + desc)
                        .collect(Collectors.joining());
            }

            // add enchants
            StringBuilder enchants = new StringBuilder();
            Map<Enchantment, Integer> enchantments = meta.getEnchants();
            if (!enchantments.isEmpty()) {
                int index = 0;
                for (Enchantment enchantment : enchantments.keySet()) {
                    enchants.append("\n §8– §b").append(enchantment.getName()).append(" niv.").append(enchantments.values().toArray()[index]);
                    index++;
                }
            }

            // add lore and enchants texts to the hover event
            component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(
                    (!lore.isEmpty() ? "§7§lDescription" + lore : "")
                            + (!lore.isEmpty() && !enchantments.isEmpty() ? "\n \n" : "")
                            + (!enchantments.isEmpty() ? "§7§lEnchantements" + enchants : "")
            ).create()));
            messages.add(component);
        }

        Pager pager = new Pager(event, messages);
        // put the pager in cache if the command sender is a player
        if (sender instanceof Player) {
            plugin.getPagers().put(((Player) sender).getUniqueId(), pager);
        }

        // add the first line with prefix + event name
        TextComponent message = new TextComponent(ChestEvent.PREFIX + "Contenu de §a" + event + "§7: ");
        message.setColor(ChatColor.GRAY);

        // add page selector if there is too much items
        if (items.size() > Pager.PAGE_SIZE && sender instanceof Player) {
            message.addExtra(getPageSelector(pager));
        }

        // send messages
        sender.sendMessage("\n");
        if (sender instanceof Player) {
            Player player = (Player) sender;
            player.spigot().sendMessage(message);
            pager.getView().forEach(msg -> player.spigot().sendMessage(msg));
        } else {
            sender.sendMessage(BaseComponent.toLegacyText(message));
            messages.stream().map(msg -> BaseComponent.toLegacyText(msg)).forEach(sender::sendMessage);
        }
    }

    private TextComponent getPageSelector(Pager pager) {
        TextComponent message = new TextComponent(" ");
        // add previous page text
        TextComponent previousPage = new TextComponent("[<--]");
        previousPage.setColor(pager.getCurrentPage() > 1 ? ChatColor.YELLOW : ChatColor.DARK_GRAY);
        previousPage.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(
                "§7Aller à la page précédente"
        ).create()));

        // add click event to go to previous page if there is a page before
        if (pager.getCurrentPage() > 1) {
            previousPage.setClickEvent(new ClickEvent(
                    ClickEvent.Action.RUN_COMMAND,
                    "/chestevent :" + (pager.getCurrentPage() - 1)
            ));
        }
        message.addExtra(previousPage);

        // add current / total pages
        TextComponent pages = new TextComponent(" " + pager.getCurrentPage() + "/" + pager.getMaxPage() + " ");
        pages.setColor(ChatColor.GRAY);
        message.addExtra(pages);

        // add next page text
        TextComponent nextPage = new TextComponent("[-->]");
        nextPage.setColor(pager.getCurrentPage() < pager.getMaxPage() ? ChatColor.YELLOW : ChatColor.DARK_GRAY);
        nextPage.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(
                "§7Aller à la page suivante"
        ).create()));

        // add click event to run next page commande if there is a page after
        if (pager.getCurrentPage() < pager.getMaxPage()) {
            nextPage.setClickEvent(new ClickEvent(
                    ClickEvent.Action.RUN_COMMAND,
                    "/chestevent :" + (pager.getCurrentPage() + 1)
            ));
        }
        message.addExtra(nextPage);

        return message;
    }
}
