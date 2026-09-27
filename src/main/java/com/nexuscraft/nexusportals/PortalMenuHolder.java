package com.nexuscraft.nexusportals;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.Map;
import java.util.UUID;

/**
 * The real {@code InventoryHolder} tag on a browsable portal-network menu, same "InventoryHolder
 * subclass carries its own context" pattern NexusMenu/NexusWeatherReport already established in
 * this family. {@link #destinationOf(int)} is what {@code PortalMenuListener} uses to resolve a
 * real {@code InventoryClickEvent}'s slot back into an actual destination portal.
 */
public final class PortalMenuHolder implements InventoryHolder {

    private final UUID viewerId;
    private final PortalKey sourceKey;
    private final Map<Integer, PortalKey> destinationsBySlot;
    private Inventory inventory;

    public PortalMenuHolder(UUID viewerId, PortalKey sourceKey, Map<Integer, PortalKey> destinationsBySlot) {
        this.viewerId = viewerId;
        this.sourceKey = sourceKey;
        this.destinationsBySlot = destinationsBySlot;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public UUID viewerId() {
        return viewerId;
    }

    public PortalKey sourceKey() {
        return sourceKey;
    }

    public PortalKey destinationOf(int slot) {
        return destinationsBySlot.get(slot);
    }
}
