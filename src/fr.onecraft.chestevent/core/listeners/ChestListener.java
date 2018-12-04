package fr.onecraft.chestevent.core.listeners;

import fr.onecraft.chestevent.ChestEvent;
import fr.onecraft.chestevent.core.objects.Chest;
import fr.onecraft.chestevent.core.objects.Menu;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class ChestListener implements Listener {
    private ChestEvent plugin;

    public ChestListener(ChestEvent plugin) {
        this.plugin = plugin;
    }

    /*
     * When an item is clicked
     */
    @EventHandler
    public void on(PlayerInteractEvent event) {
        // if it is a right click
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.RIGHT_CLICK_AIR) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = player.getItemInHand();
        // if clicked item is a chest
        if (item == null || item.getType() != Material.CHEST) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        // check if the chest's name is right
        if (meta.getDisplayName() == null || !meta.getDisplayName().equals(Chest.CHEST_NAME)) {
            return;
        }

        List<String> lore = meta.getLore();
        // check if lore is right
        if (lore == null || lore.size() < 2) {
            return;
        }

        int id;
        try {
            // get the id from the lore
            String lastLoreLine = lore.get(lore.size() - 1);
            id = Integer.parseInt(lastLoreLine.substring(lastLoreLine.lastIndexOf("#") + 1));
        } catch (NumberFormatException e) {
            return;
        }

        Chest chest = Chest.fromId(plugin, id);
        // delete the chest if it is null
        if (chest == null) {
            player.getInventory().setItemInHand(new ItemStack(Material.AIR));
            player.sendMessage(ChestEvent.ERROR + "Ce coffre n'existe plus.");
            return;
        }

        // open the inventory if the player has the permission
        if (player.hasPermission(chest.getPermission())) {
            Menu menu = chest.getMenu();
            player.openInventory(menu.getPage(1));
            event.setCancelled(true);
        }
    }

    /*
     * When a menu's item is clicked
     */
    @EventHandler
    public void on(InventoryClickEvent event) {
        // cancel if this is not an inventory from the plugin
        if (!(event.getInventory().getHolder() instanceof Menu)) {
            return;
        }
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getClickedInventory();
        if (inventory == null) {
            return;
        }

        // cancel if the clicked inventory is not the top inventory
        if (!event.getClickedInventory().equals(player.getOpenInventory().getTopInventory())) {
            return;
        }

        Menu menu = (Menu) inventory.getHolder();
        ItemStack clickedItem = event.getCurrentItem();
        // cancel if clicked item is null or is air
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        int clickedSlot = event.getSlot();
        // if clicked item is a button
        if (clickedItem.getType() == Menu.PAGE_BUTTON || clickedItem.getType() == Menu.SEPARATION_BUTTON) {
            // if clicked button is next page button
            if (clickedSlot == 51) {
                if (menu.getCurrentPage() < Math.ceil((double) menu.getSize() / 45)) {
                    menu.setCurrentPage(menu.getCurrentPage() + 1);
                    player.openInventory(menu.getPage(menu.getCurrentPage()));
                }
                // if clicked button is previous page button
            } else if (clickedSlot == 47) {
                if (menu.getCurrentPage() > 1) {
                    menu.setCurrentPage(menu.getCurrentPage() - 1);
                    player.openInventory(menu.getPage(menu.getCurrentPage()));
                }
            }
            return;
        }

        // if player's inventory is not full else tell him
        if (player.getInventory().firstEmpty() != -1) {
            // give the item to the player and remove it from the chest
            if (menu.getItems().remove(clickedItem)) {
                player.getInventory().addItem(clickedItem);
                inventory.setItem(clickedSlot, new ItemStack(Material.AIR));
                player.updateInventory();
            } else {
                player.sendMessage(ChestEvent.ERROR + "Impossible de récupérer cet item.");
            }
        } else {
            player.sendMessage(ChestEvent.ERROR + "Votre inventaire est plein !");
        }

        // delete the inventory if it is empty
        if (menu.getItems().isEmpty()) {
            player.getInventory().setItemInHand(new ItemStack(Material.AIR));
            player.updateInventory();
            player.closeInventory();
            menu.deleteChest();
        }
    }

    /*
     * When an inventory is closed
     */
    @EventHandler
    public void on(InventoryCloseEvent event) {
        // if this is not an inventory from the plugin
        if (!(event.getInventory().getHolder() instanceof Menu)) {
            return;
        }

        Inventory inventory = event.getInventory();
        // one tick later
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // check if the opened inventory is from the plugin
            if (event.getPlayer().getOpenInventory().getTopInventory().getHolder() instanceof Menu)
                return;
            Menu menu = (Menu) inventory.getHolder();

            // skip the saving if the inventory is empty
            if (menu.getItems().isEmpty()) {
                return;
            }

            menu.saveChest();
        }, 1);
    }
}