package com.nexuscraft.nexusportals;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * The persistent portal network catalog -- every named, cataloged portal this plugin knows about,
 * disk-backed in {@code portal_network.yml} (this project's own data file, separate from
 * {@code config.yml}), same "own YAML data file next to config.yml" convention as this family's
 * other per-world/per-location persistent stores. In-memory cache backed by an eager-saved file:
 * every mutation that matters is followed by a {@link #save()} call, so a server crash between
 * saves loses at most the most recent single change, never the whole catalog.
 */
public final class PortalRegistry {

    private final JavaPlugin plugin;
    private final File file;
    private final Map<PortalKey, PortalRecord> records = new HashMap<>();
    /** Reverse index: a specific real portal block's coordinate -> which registered portal it
     *  belongs to. This is what lets {@code PortalTravelListener} answer "which registered portal
     *  is this player physically standing in" by exact block membership, not a fuzzy radius guess
     *  that two portals built close together could confuse. Keyed by
     *  {@code "world:x:y:z"} for the individual block, distinct from {@link PortalKey}'s own
     *  anchor-only encoding. */
    private final Map<String, PortalKey> blockIndex = new HashMap<>();

    public PortalRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "portal_network.yml");
    }

    public void load(Logger logger) {
        records.clear();
        blockIndex.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection portalsSection = data.getConfigurationSection("portals");
        if (portalsSection == null) {
            return;
        }
        for (String storageKey : portalsSection.getKeys(false)) {
            PortalKey key = PortalKey.fromStorageKey(storageKey);
            if (key == null) {
                logger.warning("nexusportals: skipping malformed portal_network.yml entry '" + storageKey + "'");
                continue;
            }
            ConfigurationSection section = portalsSection.getConfigurationSection(storageKey);
            if (section == null) {
                continue;
            }
            PortalRecord record = new PortalRecord(key, section.getString("name", key.toStorageKey()));
            String ownerString = section.getString("owner", null);
            if (ownerString != null) {
                try {
                    record.setOwnerId(UUID.fromString(ownerString));
                } catch (IllegalArgumentException ignored) {
                    // Malformed owner id -- leave unowned rather than fail the whole load.
                }
            }
            record.setStability(section.getDouble("stability", 100.0));
            record.setLastMaintainedMillis(section.getLong("last-maintained-millis", System.currentTimeMillis()));
            record.setDormant(section.getBoolean("dormant", record.stability() <= 0.0));
            record.setStandLocationRaw(
                    section.getString("stand.world", key.world()),
                    section.getDouble("stand.x", key.x() + 0.5),
                    section.getDouble("stand.y", key.y()),
                    section.getDouble("stand.z", key.z() + 0.5),
                    (float) section.getDouble("stand.yaw", 0.0),
                    (float) section.getDouble("stand.pitch", 0.0));
            for (String coordinate : section.getStringList("blocks")) {
                String[] parts = coordinate.split(",");
                if (parts.length == 3) {
                    try {
                        int bx = Integer.parseInt(parts[0]);
                        int by = Integer.parseInt(parts[1]);
                        int bz = Integer.parseInt(parts[2]);
                        record.addBlockCoordinate(bx, by, bz);
                    } catch (NumberFormatException ignored) {
                        // Malformed block coordinate -- skip just that one block.
                    }
                }
            }
            for (String discovererId : section.getStringList("discovered-by")) {
                try {
                    record.markDiscovered(UUID.fromString(discovererId));
                } catch (IllegalArgumentException ignored) {
                    // Malformed discoverer id -- skip just that one entry.
                }
            }
            records.put(key, record);
            reindexBlocks(record);
        }
    }

    public void save() {
        YamlConfiguration data = new YamlConfiguration();
        ConfigurationSection portalsSection = data.createSection("portals");
        for (PortalRecord record : records.values()) {
            ConfigurationSection section = portalsSection.createSection(record.key().toStorageKey());
            section.set("name", record.name());
            section.set("owner", record.ownerId() != null ? record.ownerId().toString() : null);
            section.set("stability", record.stability());
            section.set("last-maintained-millis", record.lastMaintainedMillis());
            section.set("dormant", record.isDormant());
            section.set("stand.world", record.standWorld() != null ? record.standWorld() : record.key().world());
            section.set("stand.x", record.standX());
            section.set("stand.y", record.standY());
            section.set("stand.z", record.standZ());
            section.set("stand.yaw", (double) record.standYaw());
            section.set("stand.pitch", (double) record.standPitch());
            section.set("blocks", new ArrayList<>(record.blockCoordinates()));
            List<String> discovererIds = new ArrayList<>();
            for (UUID id : record.discoveredBy()) {
                discovererIds.add(id.toString());
            }
            section.set("discovered-by", discovererIds);
        }
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("nexusportals: failed to save portal_network.yml: " + e.getMessage());
        }
    }

    public void register(PortalRecord record) {
        records.put(record.key(), record);
        reindexBlocks(record);
        save();
    }

    private void reindexBlocks(PortalRecord record) {
        for (String coordinate : record.blockCoordinates()) {
            blockIndex.put(record.key().world() + ":" + coordinate.replace(",", ":"), record.key());
        }
    }

    public PortalRecord get(PortalKey key) {
        return records.get(key);
    }

    /** Which registered portal (if any) the given location's exact block belongs to -- the real
     *  hook {@code PortalTravelListener} uses to resolve "which portal is this player standing
     *  in" from a real {@code PlayerPortalEvent}'s {@code getFrom()}. */
    public PortalRecord findByBlock(Location location) {
        String lookupKey = location.getWorld().getName() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
        PortalKey key = blockIndex.get(lookupKey);
        return key != null ? records.get(key) : null;
    }

    public PortalRecord findByName(String name) {
        for (PortalRecord record : records.values()) {
            if (record.name().equalsIgnoreCase(name)) {
                return record;
            }
        }
        return null;
    }

    public Collection<PortalRecord> all() {
        return records.values();
    }

    public Collection<PortalRecord> discoveredBy(UUID playerId) {
        List<PortalRecord> result = new ArrayList<>();
        for (PortalRecord record : records.values()) {
            if (record.isDiscoveredBy(playerId)) {
                result.add(record);
            }
        }
        return result;
    }

    public String nextAutoName(String prefix) {
        return prefix + " #" + (records.size() + 1);
    }
}
