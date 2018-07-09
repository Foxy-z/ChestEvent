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
import java.util.Set;
import java.util.stream.Collectors;

public class CmdChestEvent implements CommandExecutor {
    private ChestEvent plugin;

    public CmdChestEvent(ChestEvent plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        if (!sender.hasPermission("chestevent.give") && !sender.hasPermission("chestevent.viewcontent")
                && !sender.hasPermission("chestevent.info") && !sender.hasPermission("chestevent.list")) {
            sender.sendMessage(ChestEvent.ERROR + "Tu n'as pas la permission.");
            return true;
        } else if (args.length == 1 && args[0].startsWith(":")) {
            showPage(sender, args[0]);
            return true;
        } else if (args.length == 1 && args[0].equalsIgnoreCase("list")) {
            showEventList(sender);
            return true;
        }

        if (args.length < 2) {
            showHelp(sender);
            return true;
        }

        String action = args[0];
        String event = args[1];

        if (!Model.eventExists(event, plugin)) sender.sendMessage(ChestEvent.ERROR + "Cet événement n'existe pas.");

        if (action.equalsIgnoreCase("viewcontent"))
            viewContent(sender, event);
        else if (action.equalsIgnoreCase("info"))
            info(sender, event);
        else if (action.equalsIgnoreCase("give"))
            give(sender, event, args);
        return true;
    }

    /*
     * METHODS
     */

    private void showHelp(CommandSender sender) {
        sender.sendMessage(ChestEvent.PREFIX + "Gère les récompenses d'événements"
                + (sender.hasPermission("chestevent.info") ? "\n§b/chestevent info <event>§7 informations sur un événement" : "")
                + (sender.hasPermission("chestevent.viewcontent") ? "\n§b/chestevent viewcontent <event>§7 contenu d'un événement" : "")
                + (sender.hasPermission("chestevent.list") ? "\n§b/chestevent list §7 liste des événements" : "")
                + (sender.hasPermission("chestevent.give") ? "\n§b/chestevent give <event> [pseudo]§7 give un coffre d'événement" : ""));
    }

    private void showEventList(CommandSender sender) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (Model.getEventList(plugin).size() == 0) {
                sender.sendMessage(ChestEvent.PREFIX + "Il n'y a aucun événement.");
                return;
            }

            Set<String> list = Model.getEventList(plugin).stream().map(file -> file.getName().replace(".yml", "")).collect(Collectors.toSet());
            sender.sendMessage(ChestEvent.PREFIX + "Liste des événements");
            list.stream().map(string -> " §7– §b" + string).forEach(sender::sendMessage);
        });
    }

    private void showPage(CommandSender sender, String arg) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChestEvent.PREFIX + "Vous devez etre un joueur pour effectuer cette commande.");
            return;
        }

        if (!sender.hasPermission("chestevent.viewcontent")) {
            sender.sendMessage(ChestEvent.ERROR + "Tu n'as pas la permission.");
            return;
        }

        Player player = (Player) sender;

        String number = arg.substring(1);
        try {
            int page = Integer.parseInt(number);
            Pager pager = plugin.getPagers().get(player);
            if (pager.getPages() < page) {
                sender.sendMessage(ChestEvent.ERROR + "Cette page n'existe pas.");
                return;
            }

            pager.setCurrentPage(page);
            TextComponent message = new TextComponent(ChestEvent.PREFIX + "Contenu de l'événement §a" + pager.getEvent());
            message.setColor(ChatColor.GRAY);
            message.addExtra(": ");
            TextComponent previewPage = new TextComponent("[<--]");
            previewPage.setColor(pager.getCurrentPage() >= 2 ? ChatColor.YELLOW : ChatColor.DARK_GRAY);
            previewPage.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(
                    "§7Aller à la page précédente"
            ).create()));
            if (pager.getCurrentPage() >= 2)
                previewPage.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/chestevent :" + (pager.getCurrentPage() - 1)));
            message.addExtra(previewPage);

            TextComponent pages = new TextComponent(" " + pager.getCurrentPage());
            pages.setColor(ChatColor.GRAY);
            message.addExtra(pages);
            pages = new TextComponent("/");
            pages.setColor(ChatColor.GRAY);
            message.addExtra(pages);
            pages = new TextComponent(pager.getPages() + " ");
            pages.setColor(ChatColor.GRAY);
            message.addExtra(pages);

            TextComponent nextPage = new TextComponent("[-->]");
            nextPage.setColor(pager.getCurrentPage() < pager.getPages() ? ChatColor.YELLOW : ChatColor.DARK_GRAY);
            nextPage.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(
                    "§7Aller à la page suivante"
            ).create()));
            if (pager.getCurrentPage() < pager.getPages())
                nextPage.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/chestevent :" + (pager.getCurrentPage() + 1)));
            message.addExtra(nextPage);

            sender.sendMessage("\n");
            player.spigot().sendMessage(message);

            pager.getPage(pager.getCurrentPage()).forEach(msg -> player.spigot().sendMessage(msg));
        } catch (NumberFormatException e) {
            sender.sendMessage(ChestEvent.ERROR + "Cette page n'existe pas.");
        }
    }

    private void viewContent(CommandSender sender, String event) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (!sender.hasPermission("chestevent.viewcontent")) {
                sender.sendMessage(ChestEvent.ERROR + "Tu n'as pas la permission.");
                return;
            }

            ArrayList<ItemStack> rawItems = new ArrayList<>(Model.fromName(plugin, event).getContent());
            ArrayList<TextComponent> items = new ArrayList<>();
            int num = 1;
            for (ItemStack itemStack : rawItems) {
                ItemMeta meta = itemStack.getItemMeta();
                int count = 0;
                TextComponent component = new TextComponent("§7" + num + " §8- §b" + itemStack.getType() + " x" + itemStack.getAmount() + "§8, §7" + itemStack.getItemMeta().getDisplayName());
                String lore = "";
                if (itemStack.getItemMeta().getLore() != null)
                    lore = itemStack.getItemMeta().getLore().stream().map(desc -> "\n" + desc).collect(Collectors.joining());
                StringBuilder enchants = new StringBuilder();
                if (itemStack.getItemMeta().getEnchants().size() > 0) {
                    for (Enchantment enchantment : meta.getEnchants().keySet()) {
                        enchants.append("\n").append(" §8– §b").append(enchantment.getName()).append(" niv.").append(meta.getEnchants().values().toArray()[count]);
                        count++;
                    }
                }

                component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(
                        (lore.length() > 0 ? "§7§lDescription" : "")
                                + lore
                                + ((lore.length() > 0 && meta.getEnchants().size() > 0) ? "\n\n" : "")
                                + (meta.getEnchants().size() > 0 ? "§7§lEnchantements" : "")
                                + enchants.toString()
                ).create()));
                items.add(component);
                num++;
            }

            Pager pager = new Pager(event, items);
            if (sender instanceof Player)
                plugin.getPagers().put(((Player) sender).getUniqueId(), pager);

            TextComponent message = new TextComponent(ChestEvent.PREFIX + "Contenu de l'événement §a" + event);
            message.setColor(ChatColor.GRAY);
            message.addExtra("§7: ");

            if (items.size() > 15 && sender instanceof Player) {
                TextComponent previewPage = new TextComponent("[<--]");
                previewPage.setColor(pager.getCurrentPage() > 1 ? ChatColor.YELLOW : ChatColor.DARK_GRAY);
                previewPage.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(
                        "§7Aller à la page précédente"
                ).create()));
                if (pager.getCurrentPage() > 1)
                    previewPage.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/chestevent :" + (pager.getCurrentPage() - 1)));
                message.addExtra(previewPage);

                TextComponent pages = new TextComponent(" " + pager.getCurrentPage());
                pages.setColor(ChatColor.GRAY);
                message.addExtra(pages);
                pages = new TextComponent("/");
                pages.setColor(ChatColor.GRAY);
                message.addExtra(pages);
                pages = new TextComponent(pager.getPages() + " ");
                pages.setColor(ChatColor.GRAY);
                message.addExtra(pages);

                TextComponent nextPage = new TextComponent("[-->]");
                nextPage.setColor(pager.getCurrentPage() < pager.getPages() ? ChatColor.YELLOW : ChatColor.DARK_GRAY);
                nextPage.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(
                        "§7Aller à la page suivante"
                ).create()));
                if (pager.getCurrentPage() < pager.getPages())
                    nextPage.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/chestevent :" + (pager.getCurrentPage() + 1)));
                message.addExtra(nextPage);
            }

            sender.sendMessage("\n");

            if (sender instanceof Player) {
                Player player = (Player) sender;
                player.spigot().sendMessage(message);
                pager.getPage(1).forEach(msg -> player.spigot().sendMessage(msg));
            } else {
                sender.sendMessage(BaseComponent.toLegacyText(message));
                items.stream().map(b -> b.toLegacyText()).forEach(sender::sendMessage);
            }
        });
    }

    private void info(CommandSender sender, String event) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (!sender.hasPermission("chestevent.info")) {
                sender.sendMessage(ChestEvent.ERROR + "Tu n'as pas la permission.");
                return;
            }

            Model model = Model.fromName(plugin, event);

            sender.sendMessage(ChestEvent.PREFIX + "Informations sur l'événement §a" + event + "\n"
                    + " §8- §7Description: §b" + model.getDescription() + "\n"
                    + " §8- §7Permission: §b" + model.getPermission() + "\n"
            );
        });
    }

    private void give(CommandSender sender, String event, String[] args) {
        if (!sender.hasPermission("chestevent.give")) {
            sender.sendMessage(ChestEvent.ERROR + "Tu n'as pas la permission.");
            return;
        }

        Chest chest = Model.fromName(plugin, event).createChest();

        if (args.length > 2) {
            Player target = Bukkit.getPlayer(args[2]);
            if (target != null) {
                if (target.getInventory().firstEmpty() == -1) {
                    sender.sendMessage(ChestEvent.ERROR + "Impossible de donner le coffre, l'inventaire de §a" + target.getName() + " §7est plein.");
                    return;
                }

                sender.sendMessage(ChestEvent.PREFIX + "§a" + sender.getName() + " §7a reçu le coffre de l'événement §a" + event + "§7.");
                target.getInventory().addItem(chest.getChestItem());
            } else
                sender.sendMessage(ChestEvent.ERROR + "§a" + args[2] + " §7est introuvable.");
        } else {
            if (sender instanceof Player) {
                Player player = (Player) sender;
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
}