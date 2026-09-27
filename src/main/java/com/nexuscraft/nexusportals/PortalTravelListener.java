package com.nexuscraft.nexusportals;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;

/**
 * Real hook #2 of this project's real, deliberate linking system: a real {@code PlayerPortalEvent}
 * (real Bukkit's own event for "a player is about to be teleported by a Nether/End portal"),
 * always cancelled here so real vanilla's own blind 8:1 coordinate-scaling never runs at all --
 * this project's own browsable menu decides the real destination instead. See
 * {@code PlayerPortalEvent.java}'s own stub comment for why this works purely through cancellation
 * + a manual {@code Player#teleport}, never a real {@code TravelAgent} (which the modern real API
 * doesn't expose on this event at all).
 */
public final class PortalTravelListener implements Listener {

    private final PortalConfig config;
    private final PortalRegistry registry;
    private final Random random;

    public PortalTravelListener(PortalConfig config, PortalRegistry registry, Random random) {
        this.config = config;
        this.registry = registry;
        this.random = random;
    }

    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) {
            // End portals/gateways are deliberately left to real vanilla -- this project's own End
            // expansion is the separate stronghold-triangulation aid (see EnderEyeListener), not a
            // network-menu takeover of the End portal itself.
            return;
        }

        Player player = event.getPlayer();
        Location from = event.getFrom();
        PortalRecord source = registry.findByBlock(from);
        if (source == null) {
            source = lazilyRegister(player, from);
        } else {
            source.markDiscovered(player.getUniqueId());
        }

        event.setCancelled(true);

        applyRoughTransitIfAny(player, source);

        if (!hasOtherReachableDestination(player, source)) {
            player.sendMessage("§5[Portals] §fYou haven't discovered any other portals yet -- light one elsewhere, "
                    + "or travel through a friend's, to grow your network.");
            return;
        }

        Inventory menu = PortalMenu.build(player, source, config, registry);
        player.openInventory(menu);
    }

    private boolean hasOtherReachableDestination(Player player, PortalRecord source) {
        for (PortalRecord candidate : registry.discoveredBy(player.getUniqueId())) {
            if (!candidate.key().equals(source.key()) && !candidate.isDormant()) {
                return true;
            }
        }
        return false;
    }

    private PortalRecord lazilyRegister(Player player, Location from) {
        PortalKey anchor = PortalKey.of(from);
        PortalRecord record = new PortalRecord(anchor, registry.nextAutoName(config.autoNamePrefix));
        record.addBlockCoordinate(from.getBlockX(), from.getBlockY(), from.getBlockZ());
        record.setStandLocation(from);
        record.markDiscovered(player.getUniqueId());
        registry.register(record);
        player.sendMessage("§5[Portals] §fThis portal wasn't in the network yet -- cataloged it as §d" + record.name() + "§f.");
        return record;
    }

    private void applyRoughTransitIfAny(Player player, PortalRecord source) {
        double chance = switch (source.tier()) {
            case UNSTABLE -> config.roughTransitChanceUnstable;
            case COLLAPSING -> config.roughTransitChanceCollapsing;
            default -> 0.0;
        };
        if (chance <= 0.0 || random.nextDouble() >= chance) {
            return;
        }
        player.setHealth(Math.max(1.0, player.getHealth() - config.roughTransitDamage));
        player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 100, 0));
        player.sendMessage("§5[Portals] §c" + source.name() + "§f's own instability made for a rough crossing.");
    }
}
