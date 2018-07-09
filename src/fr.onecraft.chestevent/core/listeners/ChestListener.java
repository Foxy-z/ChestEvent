package fr.onecraft.chestevent.core.listeners;

import fr.onecraft.chestevent.ChestEvent;
import fr.onecraft.chestevent.core.objects.Chest;
import fr.onecraft.chestevent.core.objects.Menu;
import fr.onecraft.chestevent.core.objects.Model;
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
     * Détection du clic sur un item
     */
    @EventHandler
    public void onInterractWithChest(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();
        if (!action.equals(Action.RIGHT_CLICK_BLOCK) && !action.equals(Action.RIGHT_CLICK_AIR))
            return;
        ItemStack item = player.getItemInHand();
        if (item == null || !item.getType().equals(Material.CHEST))
            return;
        ItemMeta meta = item.getItemMeta();

        /*
         * Vérification du nom de l'item coffre
         */
        if (meta.getDisplayName() == null || !meta.getDisplayName().startsWith("§6§lCoffre d'événement §f§ln°"))
            return;

        int id;
        try {
            id = Integer.parseInt(meta.getDisplayName().substring(29));
        } catch (NumberFormatException e) {
            return;
        }

        Chest chest = Chest.fromId(plugin, id);
        if (chest == null) {
            player.getInventory().setItemInHand(new ItemStack(Material.AIR));
            player.updateInventory();
            chest.delete();
            player.sendMessage(ChestEvent.ERROR + "Ce coffre n'existe plus.");
            return;
        }

        if (player.hasPermission(chest.getPermission())) {
            Menu menu = chest.getMenu();
            player.openInventory(menu.getPage(1));
            event.setCancelled(true);
        }
    }

    /*
     * Lorsqu'item du menu est cliqué
     */
    @EventHandler
    public void onUseButton(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof Menu)) return;
        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getInventory();

        Menu menu = (Menu) inventory.getHolder();
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem != null && (clickedItem.getType().equals(Menu.PAGE_BUTTON) || clickedItem.getType().equals(Menu.SEPARATION_BUTTON))) {
            if (clickedItem.equals(inventory.getItem(51))) {
                if (menu.getCurrentPage() < Math.ceil((double) menu.getSize() / 45)) {
                    menu.setCurrentPage(menu.getCurrentPage() + 1);
                    player.openInventory(menu.getPage(menu.getCurrentPage()));
                }
                event.setCancelled(true);
            } else if (clickedItem.equals(inventory.getItem(47))) {
                if (menu.getCurrentPage() > 1) {
                    menu.setCurrentPage(menu.getCurrentPage() - 1);
                    player.openInventory(menu.getPage(menu.getCurrentPage()));
                }
                event.setCancelled(true);
            } else if (clickedItem.equals(inventory.getItem(49)))
                event.setCancelled(true);
        }
    }

    /*
     * Lorsqu'un item du menu est cliqué
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked().getOpenInventory().getTopInventory().getHolder() instanceof Menu)
            event.setCancelled(true);
        if (!(event.getInventory().getHolder() instanceof Menu)) return;
        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getClickedInventory();
        if (inventory == null) return;

        Menu menu = (Menu) inventory.getHolder();
        event.setCancelled(true);
        ItemStack itemStack = event.getCurrentItem();
        if (itemStack == null || itemStack.getType().equals(Material.AIR)) return;

        if (menu.getSize() > 54) {
            if (itemStack.equals(inventory.getItem(51)) || itemStack.equals(inventory.getItem(49)) || itemStack.equals(inventory.getItem(47)))
                return;
            if (player.getInventory().firstEmpty() != -1) {
                menu.getItems().remove(itemStack);
                player.getInventory().addItem(itemStack);
                inventory.setItem(event.getRawSlot(), new ItemStack(Material.AIR));
                player.updateInventory();
            } else {
                player.sendMessage(ChestEvent.ERROR + "Votre inventaire est plein !");
            }
        } else {
            if (player.getInventory().firstEmpty() != -1) {
                menu.getItems().remove(itemStack);
                player.getInventory().addItem(itemStack);
                inventory.setItem(event.getRawSlot(), new ItemStack(Material.AIR));
                player.updateInventory();
            } else {
                player.sendMessage(ChestEvent.ERROR + "Votre inventaire est plein !");
            }
        }

        if (menu.getItems().isEmpty()) {
            player.getInventory().setItemInHand(new ItemStack(Material.AIR));
            player.updateInventory();
            player.closeInventory();
            menu.deleteChest();
        }
    }

    /*
     * Quand un inventaire est fermé
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
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