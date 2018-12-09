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
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class EventListener implements Listener {

    private final ChestEvent plugin;

    public EventListener(ChestEvent plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void on(PlayerQuitEvent event) {
        // remove the pager of the player from the cache
        plugin.getPagers().remove(event.getPlayer().getUniqueId());
    }

    /*
     * When an item is clicked
     */
    @EventHandler
    public void on(PlayerInteractEvent event) {
        // if it is a right click
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.RIGHT_CLICK_AIR) return;

        // if clicked item is a chest
        Player player = event.getPlayer();
        ItemStack item = player.getItemInHand();
        if (item == null || item.getType() != Material.CHEST) return;

        // check if the chest's name is right
        ItemMeta meta = item.getItemMeta();
        if (meta.getDisplayName() == null || !meta.getDisplayName().equals(Chest.CHEST_NAME)) return;

        // check if lore is right
        List<String> lore = meta.getLore();
        if (lore == null || lore.size() < 2) return;

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
            event.setCancelled(true);
            plugin.logToFile("OPEN_ERROR", player.getName() + " tried to open a non-existent chest link (ChestID: " + id + ")");
            return;
        }

        // open the inventory if the player has the permission
        if (player.hasPermission(chest.getPermission())) {
            player.openInventory(chest.getMenu().getView());
            event.setCancelled(true);
            plugin.logToFile("OPEN", player.getName() + " opened " + chest.getEventName() + " (ChestID: " + chest.getId() + ")");
        }
    }

    /*
     * When a menu's item is clicked
     */
    @EventHandler
    public void on(InventoryClickEvent event) {
        // cancel if this is not an inventory from the plugin
        if (!(event.getInventory().getHolder() instanceof Menu)) return;
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getClickedInventory();
        if (inventory == null) return;

        // cancel if the clicked inventory is not the top inventory
        if (!event.getClickedInventory().equals(player.getOpenInventory().getTopInventory())) return;

        Menu menu = (Menu) inventory.getHolder();
        ItemStack clickedItem = event.getCurrentItem();
        // cancel if clicked item is null or is air
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        int clickedSlot = event.getSlot();

        // if there is pages
        if (clickedSlot >= Menu.ITEMS_PER_PAGE) {
            if (clickedSlot == Menu.ITEMS_PER_PAGE) {
                // clicked on previous page button
                if (menu.getCurrentPage() > 1) {
                    menu.updatePageIndex(-1);
                    player.openInventory(menu.getView());
                    plugin.logToFile("CHANGE_PAGE", player.getName() + " changed the page (ChestID: " + menu.getChestId() + ", page: " + menu.getCurrentPage() + "/" + menu.getMaxPage() + ")");
                }
            } else if (clickedSlot == Menu.ITEMS_PER_PAGE + 8) {
                // clicked on next page button
                if (menu.getCurrentPage() < menu.getMaxPage()) {
                    menu.updatePageIndex(+1);
                    player.openInventory(menu.getView());
                    plugin.logToFile("CHANGE_PAGE", player.getName() + " changed the page (ChestID: " + menu.getChestId() + ", page: " + menu.getCurrentPage() + "/" + menu.getMaxPage() + ")");
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
                plugin.logToFile("ITEM", player.getName() + " took " + clickedItem.getAmount() + "x " + clickedItem.getType().toString() + ":" + clickedItem.getData().getData() + " (ChestID: " + menu.getChestId() + ", remaining items: " + menu.getItems().size() + ")");
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
            player.sendMessage(ChestEvent.PREFIX + "Le coffre est désormais vide !");
            plugin.logToFile("REMOVE_EMPTY", player.getName() + " took the last item of a chest, it has been removed (ChestID: " + menu.getChestId() + ")");
        }
    }

    /*
     * When an inventory is closed
     */
    @EventHandler
    public void on(InventoryCloseEvent event) {
        // if this is not an inventory from the plugin
        if (!(event.getInventory().getHolder() instanceof Menu)) return;

        Player player = (Player) event.getPlayer();
        Menu menu = ((Menu) event.getInventory().getHolder());

        // one tick later
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // check if the opened inventory is from the plugin
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof Menu) return;

            // save only if there is items
            if (!menu.getItems().isEmpty()) {
                menu.saveChest();
            }
        }, 1);
    }
}
