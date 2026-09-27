package com.nexuscraft.nexusportals;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * Real hook #1 of this project's real, deliberate linking system: the moment a player actually
 * lights a Nether portal frame ({@code PortalCreateEvent.CreateReason.FIRE} -- real Bukkit's own,
 * checked, precise "a player just did this on purpose" reason, distinct from the automatic
 * {@code NETHER_PAIR}/{@code END_PLATFORM} reasons this project deliberately leaves alone). Every
 * frame this project has never seen becomes a brand-new catalog entry; a frame that collapsed into
 * {@link StabilityTier#DORMANT} and is being re-lit either comes back properly (the real reignite
 * ritual) or comes back as a real, deliberate consequence of skipping it (a "wild" relight).
 */
public final class PortalLightingListener implements Listener {

    private final PortalConfig config;
    private final PortalRegistry registry;
    private final PortalMaintenanceService maintenance;

    public PortalLightingListener(PortalConfig config, PortalRegistry registry, PortalMaintenanceService maintenance) {
        this.config = config;
        this.registry = registry;
        this.maintenance = maintenance;
    }

    @EventHandler
    public void onPortalCreate(PortalCreateEvent event) {
        if (event.getReason() != PortalCreateEvent.CreateReason.FIRE || event.getBlocks().isEmpty()) {
            return;
        }

        PortalKey anchor = computeAnchor(event.getBlocks());
        Location standLocation = computeStandLocation(event.getBlocks());
        Entity igniterEntity = event.getEntity();
        Player igniter = igniterEntity instanceof Player ? (Player) igniterEntity : null;

        PortalRecord record = registry.get(anchor);
        if (record == null) {
            record = new PortalRecord(anchor, registry.nextAutoName(config.autoNamePrefix));
            for (BlockState block : event.getBlocks()) {
                record.addBlockCoordinate(block.getBlock().getX(), block.getBlock().getY(), block.getBlock().getZ());
            }
            record.setStandLocation(standLocation);
            if (igniter != null) {
                record.setOwnerId(igniter.getUniqueId());
                record.markDiscovered(igniter.getUniqueId());
            }
            registry.register(record);
            if (igniter != null) {
                igniter.sendMessage("§5[Portals] §fA new portal flickers to life: §d" + record.name()
                        + "§f. Rename it with §7/nexusportals rename " + record.name() + " <new name>");
            }
            return;
        }

        if (!record.isDormant()) {
            // Redundant re-light of an already-active portal (edge case) -- just refresh which
            // blocks belong to it, in case the frame was rebuilt slightly differently.
            for (BlockState block : event.getBlocks()) {
                record.addBlockCoordinate(block.getBlock().getX(), block.getBlock().getY(), block.getBlock().getZ());
            }
            registry.register(record);
            return;
        }

        // A dormant portal is being re-lit. The real reignite ritual (both real catalysts, config-
        // driven materials/amounts) restores it with its full history intact; skipping it still
        // lets vanilla's own fire mechanic relight the frame, but at a real, lasting cost.
        boolean ritualComplete = igniter != null && consumeIfPresent(igniter.getInventory());
        if (ritualComplete) {
            maintenance.reignite(record, standLocation);
            record.markDiscovered(igniter.getUniqueId());
            igniter.sendMessage("§5[Portals] §fThe reignite ritual holds -- §d" + record.name()
                    + "§f returns to the network, history intact.");
        } else {
            String staleName = record.name();
            record.resetHistory();
            record.setName(registry.nextAutoName(config.autoNamePrefix));
            record.setStability(100.0);
            for (BlockState block : event.getBlocks()) {
                record.addBlockCoordinate(block.getBlock().getX(), block.getBlock().getY(), block.getBlock().getZ());
            }
            record.setStandLocation(standLocation);
            if (igniter != null) {
                record.setOwnerId(igniter.getUniqueId());
                record.markDiscovered(igniter.getUniqueId());
                igniter.sendMessage("§5[Portals] §fThis frame collapsed and you relit it without the reignite ritual ("
                        + config.reigniteCatalystOneAmount + " " + config.reigniteCatalystOne + " + "
                        + config.reigniteCatalystTwoAmount + " " + config.reigniteCatalystTwo
                        + "). §c" + staleName + "§f's own network history is gone -- it's come back as a wild portal, §d"
                        + record.name() + "§f.");
            }
            registry.register(record);
        }
    }

    private boolean consumeIfPresent(PlayerInventory inventory) {
        int haveOne = countMaterial(inventory, config.reigniteCatalystOne);
        int haveTwo = countMaterial(inventory, config.reigniteCatalystTwo);
        if (haveOne < config.reigniteCatalystOneAmount || haveTwo < config.reigniteCatalystTwoAmount) {
            return false;
        }
        consumeMaterial(inventory, config.reigniteCatalystOne, config.reigniteCatalystOneAmount);
        consumeMaterial(inventory, config.reigniteCatalystTwo, config.reigniteCatalystTwoAmount);
        return true;
    }

    private static int countMaterial(PlayerInventory inventory, Material material) {
        int count = 0;
        ItemStack offHand = inventory.getItemInOffHand();
        if (offHand != null && offHand.getType() == material) {
            count += offHand.getAmount();
        }
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack != null && stack.getType() == material) {
                count += stack.getAmount();
            }
        }
        return count;
    }

    private static void consumeMaterial(PlayerInventory inventory, Material material, int amount) {
        int remaining = amount;
        ItemStack offHand = inventory.getItemInOffHand();
        if (remaining > 0 && offHand != null && offHand.getType() == material) {
            int take = Math.min(remaining, offHand.getAmount());
            offHand.setAmount(offHand.getAmount() - take);
            remaining -= take;
            if (offHand.getAmount() <= 0) {
                inventory.setItemInOffHand(null);
            }
        }
        for (int i = 0; i < inventory.getSize() && remaining > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack != null && stack.getType() == material) {
                int take = Math.min(remaining, stack.getAmount());
                stack.setAmount(stack.getAmount() - take);
                remaining -= take;
                if (stack.getAmount() <= 0) {
                    inventory.setItem(i, null);
                } else {
                    inventory.setItem(i, stack);
                }
            }
        }
    }

    private static PortalKey computeAnchor(java.util.List<BlockState> blocks) {
        BlockState lowest = null;
        for (BlockState block : blocks) {
            if (lowest == null
                    || block.getBlock().getX() < lowest.getBlock().getX()
                    || (block.getBlock().getX() == lowest.getBlock().getX() && block.getBlock().getY() < lowest.getBlock().getY())
                    || (block.getBlock().getX() == lowest.getBlock().getX() && block.getBlock().getY() == lowest.getBlock().getY() && block.getBlock().getZ() < lowest.getBlock().getZ())) {
                lowest = block;
            }
        }
        return PortalKey.of(lowest.getLocation());
    }

    private static Location computeStandLocation(java.util.List<BlockState> blocks) {
        double sumX = 0;
        double sumY = 0;
        double sumZ = 0;
        for (BlockState block : blocks) {
            sumX += block.getBlock().getX();
            sumY += block.getBlock().getY();
            sumZ += block.getBlock().getZ();
        }
        int count = blocks.size();
        Location any = blocks.get(0).getLocation();
        return new Location(any.getWorld(), sumX / count + 0.5, sumY / count, sumZ / count + 0.5);
    }
}
