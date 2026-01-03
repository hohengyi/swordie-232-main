package net.swordie.ms.constants;

public final class ChannelScaling {

    // Safety caps
    public static final int MAX_CHANNEL = 20;
    public static final int MAX_PDR_MDR = 60; // percent cap

    /**
     * EXP multiplier per channel
     * Channel 1 = 1.0x
     */
    public static double getExpMultiplier(int channel) {
        if (channel <= 1) {
            return 1.0;
        }
        // 2x per channel growth (your idea, smoothed)
        return 1.0 + (channel - 1) * 0.75;
    }

    /**
     * HP multiplier per channel
     */
    public static double getHpMultiplier(int channel) {
        if (channel <= 1) {
            return 1.0;
        }
        return 1.0 + Math.pow(channel - 1, 1.25);
    }

    /**
     * PDR/MDR scaling (this replaces damage reduction)
     */
    public static int getDefenseBonus(int channel) {
        if (channel <= 1) {
            return 0;
        }
        int pdr = (int) ((channel - 1) * 3);
        return Math.min(pdr, MAX_PDR_MDR);
    }

    public static boolean isBuffedChannel(int channel) {
        return channel > 1;
    }
}
