package com.nexuscraft.nexusportals;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/**
 * {@code /nexusportals name|list|rename|reignite|stronghold|reload} -- same shape as every
 * sibling's own command. {@code reload} is admin-only, double-checked here rather than trusting
 * {@code plugin.yml}'s own default alone.
 */
final class PortalCommand implements CommandExecutor {

    private final PortalConfig config;
    private final PortalRegistry registry;
    private final StrongholdSightingTracker sightings;
    private final Runnable reload;

    PortalCommand(PortalConfig config, PortalRegistry registry, StrongholdSightingTracker sightings, Runnable reload) {
        this.config = config;
        this.registry = registry;
        this.sightings = sightings;
        this.reload = reload;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> handleReload(sender);
            case "list" -> handleList(sender);
            case "rename" -> handleRename(sender, args);
            case "stronghold" -> handleStronghold(sender, args);
            default -> sendUsage(sender);
        }
        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage("§7Usage: /nexusportals <list|rename <old> <new>|stronghold [clear]|reload>");
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("nexusportals.admin")) {
            sender.sendMessage("§cYou don't have permission to do that.");
            return;
        }
        reload.run();
        sender.sendMessage("§aNexusPortals reloaded.");
    }

    private void handleList(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly a player has a discovered portal list.");
            return;
        }
        List<PortalRecord> discovered = List.copyOf(registry.discoveredBy(player.getUniqueId()));
        if (discovered.isEmpty()) {
            player.sendMessage("§7You haven't discovered any portals yet -- light one, or step through a friend's.");
            return;
        }
        player.sendMessage("§6=== Portals you've discovered ===");
        for (PortalRecord record : discovered) {
            player.sendMessage(record.tier().display() + " §f" + record.name() + " §7(" + record.key().world()
                    + ", " + Math.round(record.stability()) + "/100)");
        }
    }

    private void handleRename(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly a player can rename a portal.");
            return;
        }
        if (args.length < 3) {
            sender.sendMessage("§7Usage: /nexusportals rename <current name> <new name>");
            return;
        }
        PortalRecord record = registry.findByName(args[1]);
        if (record == null) {
            player.sendMessage("§cNo portal named '" + args[1] + "' found.");
            return;
        }
        if (!record.isDiscoveredBy(player.getUniqueId())) {
            player.sendMessage("§cYou haven't discovered that portal yet.");
            return;
        }
        String newName = args[2];
        if (registry.findByName(newName) != null) {
            player.sendMessage("§cA portal is already named '" + newName + "'.");
            return;
        }
        record.setName(newName);
        registry.register(record);
        player.sendMessage("§aRenamed to §d" + newName + "§a.");
    }

    private void handleStronghold(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly a player has a stronghold estimate.");
            return;
        }
        if (args.length >= 2 && args[1].equalsIgnoreCase("clear")) {
            sightings.clear(player.getUniqueId());
            player.sendMessage("§7Cleared your recorded eye-of-ender sightings.");
            return;
        }
        List<Sighting> recorded = sightings.sightingsOf(player.getUniqueId());
        if (recorded.size() < config.strongholdMinSightingsForEstimate) {
            player.sendMessage("§7Throw an eye of ender first -- no sightings recorded yet.");
            return;
        }
        StrongholdEstimate estimate = StrongholdTriangulator.estimate(recorded);
        player.sendMessage("§6=== Stronghold estimate ===");
        player.sendMessage("§7Estimated X/Z: §f" + Math.round(estimate.x()) + ", " + Math.round(estimate.z()));
        player.sendMessage("§7Based on §f" + estimate.sightingCount() + " §7sighting(s) -- confidence: "
                + confidenceDisplay(estimate.confidence()));
        if (estimate.confidence() != StrongholdEstimate.Confidence.HIGH) {
            player.sendMessage("§7Throw another eye from a different spot to narrow this down further.");
        }
    }

    private static String confidenceDisplay(StrongholdEstimate.Confidence confidence) {
        return switch (confidence) {
            case LOW -> "§cLow";
            case MEDIUM -> "§eMedium";
            case HIGH -> "§aHigh";
        };
    }
}
