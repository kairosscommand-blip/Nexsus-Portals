package com.nexuscraft.nexusportals;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the real, browsable portal-network menu -- the feature this project's own pitch was told
 * to build out. Opened the moment a player steps into a lit, registered portal instead of vanilla
 * silently teleporting them by blind coordinate math: every OTHER portal this player has
 * discovered becomes a real, clickable entry, dyed by its current {@link StabilityTier} so a player
 * can see at a glance which destinations are due for upkeep before they commit to one.
 */
public final class PortalMenu {

    private PortalMenu() {
    }

    public static Inventory build(Player viewer, PortalRecord source, PortalConfig config, PortalRegistry registry) {
        List<PortalRecord> destinations = new ArrayList<>();
        for (PortalRecord candidate : registry.discoveredBy(viewer.getUniqueId())) {
            if (candidate.key().equals(source.key())) {
                continue;
            }
            if (candidate.isDormant()) {
                continue;
            }
            destinations.add(candidate);
        }
        destinations.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));

        int size = Math.max(9, Math.min(54, roundUpToNine(destinations.size())));
        Map<Integer, PortalKey> slotMap = new HashMap<>();
        PortalMenuHolder holder = new PortalMenuHolder(viewer.getUniqueId(), source.key(), slotMap);
        Inventory inventory = org.bukkit.Bukkit.createInventory(holder, size, config.menuTitle);
        holder.setInventory(inventory);

        int slot = 0;
        for (PortalRecord destination : destinations) {
            if (slot >= size) {
                break;
            }
            inventory.setItem(slot, destinationIcon(destination));
            slotMap.put(slot, destination.key());
            slot++;
        }
        return inventory;
    }

    private static int roundUpToNine(int count) {
        if (count == 0) {
            return 9;
        }
        return ((count + 8) / 9) * 9;
    }

    private static ItemStack destinationIcon(PortalRecord record) {
        // Real, current, non-deprecated Component-based accessors -- a real mvn clean package
        // against real paper-api flagged this class's earlier String-based
        // setDisplayName/setLore as deprecated-but-functional; switched here to the real
        // replacement rather than leaving a known warning in place, same "prefer the real,
        // current accessor" discipline this family already applies elsewhere.
        LegacyComponentSerializer legacy = LegacyComponentSerializer.legacySection();
        ItemStack item = new ItemStack(iconMaterial(record.tier()));
        ItemMeta meta = item.getItemMeta();
        meta.displayName(legacy.deserialize(record.tier().display() + " §f" + record.name()));
        List<Component> lore = new ArrayList<>();
        lore.add(legacy.deserialize("§7World: §f" + record.key().world()));
        lore.add(legacy.deserialize("§7Stability: §f" + Math.round(record.stability()) + "/100 (" + record.tier().name() + ")"));
        lore.add(legacy.deserialize("§7" + record.tier().description()));
        lore.add(legacy.deserialize(""));
        lore.add(legacy.deserialize("§eClick to travel here"));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static Material iconMaterial(StabilityTier tier) {
        return switch (tier) {
            case STABLE -> Material.LIME_DYE;
            case WEATHERED -> Material.YELLOW_DYE;
            case UNSTABLE -> Material.ORANGE_DYE;
            case COLLAPSING -> Material.RED_DYE;
            case DORMANT -> Material.GRAY_DYE;
        };
    }
}
