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
        if (!action.equals(Action.RIGHT_CLICK_BLOCK) && !action.equals(Action.RIGHT_CLICK_AIR)) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack item = player.getItemInHand();
        // if clicked item is a chest
        if (item == null || !item.getType().equals(Material.CHEST)) {
            return;
        }
        ItemMeta meta = item.getItemMeta();

        // check if the chest's name is right
        if (meta.getDisplayName() == null || !meta.getDisplayName().startsWith("§6§lCoffre d'événement §f§ln°")) {
            return;
        }

        int id;
        try {
            id = Integer.parseInt(meta.getDisplayName().substring(29));
        } catch (NumberFormatException e) {
            return;
        }

        Chest chest = Chest.fromId(plugin, id);

        // delete the chest if he is null
        if (chest == null) {
            player.getInventory().setItemInHand(new ItemStack(Material.AIR));
            player.updateInventory();
            chest.delete();
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
        // if this is not an inventory from the plugin
        if (!(event.getInventory().getHolder() instanceof Menu)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getClickedInventory();
        if (inventory == null) {
            return;
        }

        Menu menu = (Menu) inventory.getHolder();
        event.setCancelled(true);
        ItemStack clickedItem = event.getCurrentItem();
        // check if clicked item is null or is air
        if (clickedItem == null || clickedItem.getType().equals(Material.AIR)) {
            return;
        }

        // if clicked item is a button
        if (clickedItem.getType().equals(Menu.PAGE_BUTTON) || clickedItem.getType().equals(Menu.SEPARATION_BUTTON)) {
            // if clicked button is next page button
            if (clickedItem.equals(inventory.getItem(51))) {
                if (menu.getCurrentPage() < Math.ceil((double) menu.getSize() / 45)) {
                    menu.setCurrentPage(menu.getCurrentPage() + 1);
                    player.openInventory(menu.getPage(menu.getCurrentPage()));
                }
                event.setCancelled(true);
                // if clicked button is previous page button
            } else if (clickedItem.equals(inventory.getItem(47))) {
                if (menu.getCurrentPage() > 1) {
                    menu.setCurrentPage(menu.getCurrentPage() - 1);
                    player.openInventory(menu.getPage(menu.getCurrentPage()));
                }
                event.setCancelled(true);
                // if clicked button is middle button (useless)
            } else if (clickedItem.equals(inventory.getItem(49))) {
                event.setCancelled(true);
            }
            return;
        }

        // if player's inventory is not full else tell him
        if (player.getInventory().firstEmpty() != -1) {
            // give the item to the player and remove it from the chest
            menu.getItems().remove(clickedItem);
            player.getInventory().addItem(clickedItem);
            inventory.setItem(event.getRawSlot(), new ItemStack(Material.AIR));
            player.updateInventory();
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
            if (menu.getItems().isEmpty()) return;
            menu.saveChest();
        }, 1);
    }
}