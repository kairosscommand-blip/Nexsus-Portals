package com.nexuscraft.nexusportals;

/**
 * One player's current best guess at the nearest stronghold's X/Z, plus how much this project
 * actually trusts it. {@link #confidence()} is deliberately conservative -- see
 * {@code EnderSignal.java}'s own "known-uncertain" note on exactly what real vanilla's
 * {@code getTargetLocation()} value represents moment-to-moment.
 */
public record StrongholdEstimate(double x, double z, int sightingCount, double agreementRadius, Confidence confidence) {

    public enum Confidence {
        /** A single throw -- real vanilla's own immediate target, taken at face value, no
         *  cross-check possible yet. */
        LOW,
        /** Two or more throws whose pairwise triangulated intersections disagree by more than a
         *  loose real-world margin -- still narrows things down, but the player should throw from
         *  a third, different spot before digging. */
        MEDIUM,
        /** Three or more throws whose pairwise intersections all agree within a tight real-world
         *  margin -- as real a "X marks the spot" as this project can honestly offer. */
        HIGH
    }
}
