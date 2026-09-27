package com.nexuscraft.nexusportals;

/**
 * The five-tier stability ladder every registered portal sits on, 0-100. Entirely this project's
 * own invented mechanic -- real vanilla portals never decay at all. Real-time decay
 * ({@link PortalMaintenanceService}) pushes a portal down this ladder; real fuel (right-clicking
 * the frame with the config'd item) pushes it back up.
 */
public enum StabilityTier {
    STABLE("§aStable", "holding steady -- no real cause for concern yet"),
    WEATHERED("§eWeathered", "showing real wear -- due for maintenance before long"),
    UNSTABLE("§6Unstable", "a real, rough-transit risk on every trip through it now"),
    COLLAPSING("§cCollapsing", "on the verge of going dark -- maintain it now or lose it"),
    DORMANT("§8Dormant", "extinguished -- needs a real reignite ritual, not just fuel, to return");

    private final String display;
    private final String description;

    StabilityTier(String display, String description) {
        this.display = display;
        this.description = description;
    }

    public String display() {
        return display;
    }

    public String description() {
        return description;
    }

    public static StabilityTier fromStability(double stability) {
        if (stability <= 0.0) {
            return DORMANT;
        } else if (stability <= 10.0) {
            return COLLAPSING;
        } else if (stability <= 40.0) {
            return UNSTABLE;
        } else if (stability <= 75.0) {
            return WEATHERED;
        }
        return STABLE;
    }
}
