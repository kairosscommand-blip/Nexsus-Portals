package com.nexuscraft.nexusportals;

import org.bukkit.Location;

/**
 * A portal's deterministic identity: the anchor block coordinate. Real vanilla portal creation
 * (real {@code PortalCreateEvent}) hands over the exact list of {@code NETHER_PORTAL} blocks it
 * just filled in -- this project anchors a portal's identity to the lowest-coordinate block among
 * that list (lowest world name lexically first for tie-breaking across dimensions that could
 * theoretically share coordinates, then lowest x, then y, then z), same "deterministic
 * lowest-coordinate anchor" convention this family's own pond/brewing-stand per-location caching
 * already uses (see {@code Block.java}'s own comment on that pattern).
 *
 * <p>Two different frames never collide on this key by construction (each real portal's own block
 * list is disjoint from every other real portal's), and re-lighting the exact same frame after it
 * collapses reproduces the exact same key, which is what lets {@link PortalRegistry} recognize a
 * relight as "the same portal" rather than a brand new one.
 */
public record PortalKey(String world, int x, int y, int z) {

    public static PortalKey of(Location location) {
        return new PortalKey(location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    /** Encodes/decodes to and from this project's own {@code portal_network.yml} section-key
     *  format -- YAML section keys can't contain the raw characters a {@link Location} would
     *  otherwise need (a colon inside a world name is already vanishingly unlikely but a literal
     *  comma/colon separator keeps this unambiguous either way). */
    public String toStorageKey() {
        return world + ":" + x + ":" + y + ":" + z;
    }

    public static PortalKey fromStorageKey(String key) {
        String[] parts = key.split(":");
        if (parts.length != 4) {
            return null;
        }
        try {
            return new PortalKey(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
