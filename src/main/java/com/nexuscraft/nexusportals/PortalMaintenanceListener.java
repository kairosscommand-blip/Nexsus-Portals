package com.nexuscraft.nexusportals;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Real hook for topping up a portal's stability: right-clicking an obsidian frame block directly
 * touching a registered portal's own interior with the config'd real fuel item
 * ({@code fuel.item}, default {@code GLOWSTONE_DUST}) in hand consumes it, up to a config'd cap per
 * interaction, and restores stability -- the real, ongoing maintenance cost this project's own
 * pitch was told to lean into. A dormant portal can't be topped up at all; see
 * {@code PortalLightingListener}'s own reignite-ritual handling for what a truly extinguished
 * portal needs instead.
 */
public final class PortalMaintenanceListener implements Listener {

    private final PortalConfig config;
    private final PortalRegistry registry;
    private final PortalMaintenanceService maintenance;

    public PortalMaintenanceListener(PortalConfig config, PortalRegistry registry, PortalMaintenanceService maintenance) {
        this.config = config;
        this.registry = registry;
        this.maintenance = maintenance;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null || item.getType() != config.fuelItem) {
            return;
        }
        Block clicked = event.getClickedBlock();
        if (clicked == null) {
            return;
        }

        PortalRecord record = registry.findByBlock(clicked.getLocation());
        if (record == null) {
            for (BlockFace face : BlockFace.values()) {
                record = registry.findByBlock(clicked.getRelative(face).getLocation());
                if (record != null) {
                    break;
                }
            }
        }
        if (record == null) {
            return;
        }

        Player player = event.getPlayer();
        if (record.isDormant()) {
            player.sendMessage("§5[Portals] §c" + record.name()
                    + "§f has gone dormant -- fuel alone won't bring it back. It needs the real reignite ritual: relight "
                    + "the frame while holding " + config.reigniteCatalystOneAmount + " " + config.reigniteCatalystOne
                    + " and " + config.reigniteCatalystTwoAmount + " " + config.reigniteCatalystTwo + ".");
            return;
        }

        double missing = 100.0 - record.stability();
        if (missing <= 0.0) {
            player.sendMessage("§5[Portals] §d" + record.name() + "§f is already at full stability.");
            return;
        }

        int needed = config.fuelRestorePerItem > 0
                ? (int) Math.ceil(missing / config.fuelRestorePerItem)
                : item.getAmount();
        int used = Math.min(item.getAmount(), Math.min(config.fuelMaxItemsPerInteraction, Math.max(1, needed)));

        event.setCancelled(true);
        item.setAmount(item.getAmount() - used);

        double newStability = maintenance.topUp(record, used);
        player.sendMessage("§5[Portals] §fFed " + used + " " + config.fuelItem + " into §d" + record.name()
                + "§f -- stability now §e" + Math.round(newStability) + "/100 §f(" + record.tier().name() + ").");
    }
}
