package com.nexuscraft.nexusportals;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;

/**
 * The real-time decay/upkeep side of the maintenance system -- the feature this project's own
 * pitch was told to lean into. {@link #tick()} runs on a real, config-driven interval
 * ({@code decay.interval-minutes}) and drops every non-dormant portal's stability by a fixed
 * amount; a portal that crosses into {@link StabilityTier#DORMANT} as a result is really,
 * physically extinguished -- its own real {@code NETHER_PORTAL} blocks revert to air, matching the
 * in-fiction idea that an unmaintained portal doesn't just become risky to use, it eventually
 * stops working at all.
 */
public final class PortalMaintenanceService {

    private final PortalConfig config;
    private final PortalRegistry registry;

    public PortalMaintenanceService(PortalConfig config, PortalRegistry registry) {
        this.config = config;
        this.registry = registry;
    }

    /** Pure decay math, exposed separately so it can be unit-tested without a real World/Block --
     *  returns the new stability value, never applying it itself. */
    public static double decay(double currentStability, double amountPerInterval) {
        return Math.max(0.0, currentStability - amountPerInterval);
    }

    /** Pure fuel-top-up math: how much stability {@code itemsConsumed} real fuel items actually
     *  restore, capped at 100. */
    public static double refuel(double currentStability, int itemsConsumed, double restorePerItem) {
        return Math.min(100.0, currentStability + itemsConsumed * restorePerItem);
    }

    /** Runs one real decay pass over every registered portal. Returns the portals that were
     *  extinguished this pass (crossed from non-dormant into dormant), so the caller can message
     *  anyone nearby. */
    public List<PortalRecord> tick() {
        List<PortalRecord> newlyDormant = new ArrayList<>();
        boolean changed = false;
        for (PortalRecord record : registry.all()) {
            if (record.isDormant()) {
                continue;
            }
            double before = record.stability();
            record.setStability(decay(before, config.decayAmountPerInterval));
            changed = true;
            if (record.isDormant() && before > 0.0) {
                extinguish(record);
                newlyDormant.add(record);
            }
        }
        if (changed) {
            registry.save();
        }
        return newlyDormant;
    }

    /** Really removes this portal's own frame-interior blocks (sets them to air), the physical
     *  half of "dormant" -- a dormant portal isn't just a number in {@code portal_network.yml},
     *  it's a real, dark hole in the obsidian where a working portal used to be. Frame obsidian
     *  itself is left untouched (only the {@code NETHER_PORTAL} blocks this record's own
     *  {@link PortalRecord#blockCoordinates()} lists are cleared) -- exactly what's needed for a
     *  later reignite to relight the same frame with flint and steel again. */
    public void extinguish(PortalRecord record) {
        World world = Bukkit.getWorld(record.key().world());
        if (world == null) {
            return;
        }
        for (String coordinate : record.blockCoordinates()) {
            String[] parts = coordinate.split(",");
            if (parts.length != 3) {
                continue;
            }
            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            int z = Integer.parseInt(parts[2]);
            world.getBlockAt(x, y, z).setType(Material.AIR);
        }
    }

    public double topUp(PortalRecord record, int itemsConsumed) {
        double restored = refuel(record.stability(), itemsConsumed, config.fuelRestorePerItem);
        record.setStability(restored);
        record.setLastMaintainedMillis(System.currentTimeMillis());
        registry.save();
        return restored;
    }

    /** The real reignite ritual's own math -- a dormant portal that's re-lit WITH both real
     *  catalysts consumed returns to a real, working (if not pristine) state; see
     *  {@code PortalLightingListener}'s own comment for the "wild relight" consequence of skipping
     *  this. */
    public void reignite(PortalRecord record, Location newStandLocation) {
        record.setStability(config.reigniteRestoredStability);
        record.setLastMaintainedMillis(System.currentTimeMillis());
        if (newStandLocation != null) {
            record.setStandLocation(newStandLocation);
        }
        registry.save();
    }
}
