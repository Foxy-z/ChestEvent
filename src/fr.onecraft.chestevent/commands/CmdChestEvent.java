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
        if (!sender.hasPermission("chestevent." + action)) {
            sender.sendMessage(ChestEvent.ERROR + "Tu n'as pas la permission.");
        } else if (action.startsWith(":") && (sender instanceof Player)) {
            showPage(sender, action);
        } else if (action.equalsIgnoreCase("list")) {
            showEventList(sender);
        } else if (action.equalsIgnoreCase("reload")) {
            reloadCache(sender);
        } else if (args.length < 2) {
            showHelp(sender);
        } else {
            String event = args[1];
            if (!Model.eventExists(event, plugin)) {
                sender.sendMessage(ChestEvent.ERROR + "Cet événement n'existe pas.");
            } else if (action.equalsIgnoreCase("viewcontent")) {
                viewContent(sender, event);
            } else if (action.equalsIgnoreCase("info")) {
                info(sender, event);
            } else if (action.equalsIgnoreCase("give")) {
                give(sender, event, args);
            } else {
                showHelp(sender);
            }
        }
        return true;
    }

    private void reloadCache(CommandSender sender) {
        Model.loadEventList(plugin);
        sender.sendMessage(ChestEvent.PREFIX + "Les modèles ont été chargés.");
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(ChestEvent.PREFIX + "Gère les récompenses d'événements"
                + (sender.hasPermission("chestevent.info") ? "\n§b/chestevent info <event>§7 informations sur un événement" : "")
                + (sender.hasPermission("chestevent.viewcontent") ? "\n§b/chestevent viewcontent <event>§7 contenu d'un événement" : "")
                + (sender.hasPermission("chestevent.list") ? "\n§b/chestevent list §7liste des événements" : "")
                + (sender.hasPermission("chestevent.give") ? "\n§b/chestevent give <event> [pseudo]§7 give un coffre d'événement" : "")
                + (sender.hasPermission("chestevent.reload") ? "\n§b/compensation reload §7met à jour les événements" : ""));
    }

    private void showEventList(CommandSender sender) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Model> models = Model.getEventList();

            // return an error if there is not any event
            if (models.isEmpty()) {
                sender.sendMessage(ChestEvent.PREFIX + "Il n'y a aucun événement.");
                return;
            }

            sender.sendMessage(ChestEvent.PREFIX + "Liste des événements");
            models.stream().map(model -> " §7– §b" + model.getName()).forEach(sender::sendMessage);
        });
    }

    private void info(CommandSender sender, String event) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Model model = Model.fromName(plugin, event);

            // return an error if the model's configuration is not valid
            if (model == null) {
                sender.sendMessage(ChestEvent.PREFIX + "La configuration du modèle n'est pas valide.");
                return;
            }

            sender.sendMessage(ChestEvent.PREFIX + "Informations sur l'événement §a" + event + "\n"
                    + " §8- §7Description: §b" + model.getDescription() + "\n"
                    + " §8- §7Permission: §b" + model.getPermission() + "\n"
            );
        });
    }

    private void give(CommandSender sender, String event, String[] args) {
        // return an error if the model's name is not valid
        if (!Model.isValidName(event)) {
            sender.sendMessage(ChestEvent.PREFIX + "Le nom du modèle n'est pas valide, il ne peut comporter que des lettres, chiffres et tirets.");
            return;
        }

        Model model = Model.fromName(plugin, event);

        // return an error if the model's configuration is not valid
        if (model == null) {
            sender.sendMessage(ChestEvent.PREFIX + "La configuration du modèle n'est pas valide.");
            return;
        }

        Chest chest = model.createChest();

        // return an error if the plugin can't create the chest
        if (chest == null) {
            sender.sendMessage(ChestEvent.PREFIX + "Erreur lors de la création du coffre.");
            return;
        }

        // if there is a specified player to get the chest or else give it to the command sender
        if (args.length > 2) {
            Player target = Bukkit.getPlayer(args[2]);
            // give the chest if the target is online or else return an error

            if (target != null) {
                // return an error if the target's inventory is full
                if (target.getInventory().firstEmpty() == -1) {
                    sender.sendMessage(ChestEvent.ERROR + "Impossible de donner le coffre, l'inventaire de §a" + target.getName() + " §7est plein.");
                    return;
                }

                sender.sendMessage(ChestEvent.PREFIX + "§a" + target.getName() + " §7a reçu le coffre de l'événement §a" + event + "§7.");
                target.getInventory().addItem(chest.getChestItem());
            } else {
                sender.sendMessage(ChestEvent.ERROR + "§a" + args[2] + " §7est introuvable.");
            }
        } else {
            // give the chest to the sender if it is a player
            if (sender instanceof Player) {
                Player player = (Player) sender;

                // return an error if the player's inventory is full
                if (player.getInventory().firstEmpty() == -1) {
                    sender.sendMessage(ChestEvent.ERROR + "Impossible de vous donner le coffre, votre inventaire est plein.");
                    return;
                }

                sender.sendMessage(ChestEvent.PREFIX + "Vous avez reçu le coffre de l'événement §a" + event + "§7.");
                player.getInventory().addItem(chest.getChestItem());
            } else {
                sender.sendMessage(ChestEvent.PREFIX + "Vous devez etre un joueur pour effectuer cette action.");
            }
        }
    }

    private void showPage(CommandSender sender, String arg) {
        String number = arg.substring(1);
        try {
            // parse the string to a number
            int page = Integer.parseInt(number);

            // get pager from cache
            Pager pager = plugin.getPagers().get(((Player) sender).getUniqueId());

            // return an error if the page doesn't exists
            if (pager.getMaxPage() < page || page < 1) {
                sender.sendMessage(ChestEvent.ERROR + "Cette page n'existe pas.");
                return;
            }

            // update the current page
            pager.setCurrentPage(page);

            // add the first line with prefix + event name + page selector
            TextComponent message = new TextComponent(ChestEvent.PREFIX + "Contenu de l'événement §a" + pager.getEvent() + ":" + getPageSelector(pager));
            message.setColor(ChatColor.GRAY);

            // send messages
            sender.sendMessage("\n");
            ((Player) sender).spigot().sendMessage(message);
            pager.getPage(pager.getCurrentPage()).forEach(msg -> ((Player) sender).spigot().sendMessage(msg));
        } catch (NumberFormatException e) {
            sender.sendMessage(ChestEvent.ERROR + "Cette page n'existe pas.");
        }
    }

    private void viewContent(CommandSender sender, String event) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Model model = Model.fromName(plugin, event);

            // return an error if the model is not valid
            if (model == null) {
                sender.sendMessage(ChestEvent.PREFIX + "La configuration du modèle n'est pas valide.");
                return;
            }

            List<ItemStack> items = model.getContent();
            List<TextComponent> messages = new ArrayList<>();
            for (ItemStack itemStack : items) {
                ItemMeta meta = itemStack.getItemMeta();

                // add item type + item amount + display name
                TextComponent component = new TextComponent("§8- §b" + itemStack.getType() + " x" + itemStack.getAmount() + "§8, §7" + itemStack.getItemMeta().getDisplayName());

                // add lore
                String lore = "";
                if (meta.getLore() != null) {
                    lore = meta.getLore().stream()
                            .map(desc -> "\n" + desc)
                            .collect(Collectors.joining());
                }

                // add enchants
                String enchants = "";
                Map<Enchantment, Integer> enchantements = meta.getEnchants();
                if (!enchantements.isEmpty()) {
                    int index = 0;
                    for (Enchantment enchantment : enchantements.keySet()) {
                        enchants = enchants + "\n §8– §b" + enchantment.getName() + " niv." + enchantements.values().toArray()[index];
                        index++;
                    }
                }

                // add lore and enchants texts to the hover event
                component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(
                        (!lore.isEmpty() ? "§7§lDescription" + lore : "")
                                + (!lore.isEmpty() && !enchantements.isEmpty() ? "\n \n" : "")
                                + (!enchantements.isEmpty() ? "§7§lEnchantements" + enchants : "")
                ).create()));

                messages.add(component);
            }

            Pager pager = new Pager(event, messages);
            // put the pager in cache if the command sender is a player
            if (sender instanceof Player) {
                plugin.getPagers().put(((Player) sender).getUniqueId(), pager);
            }

            // add the first line with prefix + event name
            TextComponent message = new TextComponent(ChestEvent.PREFIX + "Contenu de l'événement §a" + event + ":");
            message.setColor(ChatColor.GRAY);

            // add page selector if there is more than 15 items
            if (items.size() > 15 && sender instanceof Player) {
                message.addExtra(getPageSelector(pager));
            }

            // send messages
            sender.sendMessage("\n");
            if (sender instanceof Player) {
                Player player = (Player) sender;
                player.spigot().sendMessage(message);
                pager.getPage(1).forEach(msg -> player.spigot().sendMessage(msg));
            } else {
                sender.sendMessage(BaseComponent.toLegacyText(message));
                messages.stream().map(b -> b.toLegacyText()).forEach(sender::sendMessage);
            }
        });
    }

    private TextComponent getPageSelector(Pager pager) {
        TextComponent message = new TextComponent("");
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