package com.nexuscraft.nexusportals;

import org.bukkit.Location;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Resolves a real click inside this project's own portal-network menu back into an actual
 * destination and performs the real teleport -- the payoff half of {@link PortalMenu}. A click on
 * an empty slot, or in any inventory that isn't one of this project's own menus, is left alone
 * entirely (never assumes every {@code InventoryClickEvent} in the whole server belongs to this
 * plugin).
 */
public final class PortalMenuListener implements Listener {

    private final PortalRegistry registry;

    public PortalMenuListener(PortalRegistry registry) {
        this.registry = registry;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory clicked = event.getClickedInventory();
        if (clicked == null) {
            return;
        }
        InventoryHolder holder = clicked.getHolder();
        if (!(holder instanceof PortalMenuHolder menuHolder)) {
            return;
        }

        event.setCancelled(true);

        PortalKey destinationKey = menuHolder.destinationOf(event.getSlot());
        if (destinationKey == null) {
            return;
        }
        PortalRecord destination = registry.get(destinationKey);
        if (destination == null || destination.isDormant()) {
            return;
        }

        Location standLocation = destination.standLocation();
        if (standLocation == null) {
            return;
        }

        HumanEntity whoClicked = event.getWhoClicked();
        if (!(whoClicked instanceof Player player)) {
            return;
        }

        player.closeInventory();
        player.teleport(standLocation);
        destination.markDiscovered(player.getUniqueId());
        player.sendMessage("§5[Portals] §fYou step through into §d" + destination.name() + "§f.");
    }
}
