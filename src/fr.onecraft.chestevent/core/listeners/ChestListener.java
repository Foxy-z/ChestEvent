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
    public void onInterractWithChest(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();
        // if it is a right click
        if (!action.equals(Action.RIGHT_CLICK_BLOCK) && !action.equals(Action.RIGHT_CLICK_AIR))
            return;

        ItemStack item = player.getItemInHand();
        // if clicked item is a chest
        if (item == null || !item.getType().equals(Material.CHEST))
            return;
        ItemMeta meta = item.getItemMeta();

        // check if the chest's name is right
        if (meta.getDisplayName() == null || !meta.getDisplayName().startsWith("§6§lCoffre d'événement §f§ln°"))
            return;

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
     * When an menu's item is clicked
     */
    @EventHandler
    public void onUseButton(InventoryClickEvent event) {
        // if this is not an inventory from the plugin
        if (!(event.getInventory().getHolder() instanceof Menu)) return;
        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getInventory();

        Menu menu = (Menu) inventory.getHolder();
        ItemStack clickedItem = event.getCurrentItem();
        // if clicked item is a button
        if (clickedItem != null && (clickedItem.getType().equals(Menu.PAGE_BUTTON) || clickedItem.getType().equals(Menu.SEPARATION_BUTTON))) {
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
        }
    }

    /*
     * When a menu's item is clicked
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // if this is not an inventory from the plugin
        if (!(event.getInventory().getHolder() instanceof Menu)) return;
        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getClickedInventory();
        if (inventory == null) return;

        Menu menu = (Menu) inventory.getHolder();
        event.setCancelled(true);
        ItemStack itemStack = event.getCurrentItem();
        // check if the clicked item is not null or not air
        if (itemStack == null || itemStack.getType().equals(Material.AIR)) return;

        // if player's inventory is not full else tell him
        if (player.getInventory().firstEmpty() != -1) {
            // check if clicked item is not a button
            if (menu.getSize() > 54 && event.getRawSlot() > 45) {
                return;
            }
            // give the item to the player and remove it from the chest
            menu.getItems().remove(itemStack);
            player.getInventory().addItem(itemStack);
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
    public void onInventoryClose(InventoryCloseEvent event) {
        // if this is not an inventory from the plugin
        if (!(event.getInventory().getHolder() instanceof Menu))
            return;

        Inventory inventory = event.getInventory();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (event.getPlayer().getOpenInventory().getTopInventory().getHolder() instanceof Menu)
                return;
            Menu menu = (Menu) inventory.getHolder();
            if (menu.getItems().isEmpty()) return;
            menu.saveChest();
        }, 1);
    }
}