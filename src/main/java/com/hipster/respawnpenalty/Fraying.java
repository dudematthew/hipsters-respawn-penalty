package com.hipster.respawnpenalty;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Durability multiplier and apply/clear rules.
 * Extra wear only happens when vanilla already decided to damage a stack.
 *
 * <p>Death streak applies Fraying I–VI. Display level is {@code amplifier + 1}.
 * {@code /effect} can still go to 255: I–VII are stepped, then the extra percent
 * slows, then a quadratic-exp ramp hits {@link #MAX_MULTIPLIER}.
 */
public final class Fraying {
    /** Minecraft effect level 255. Amplifier is stored as 0–254. */
    public static final int MAX_LEVEL = 255;
    public static final int MAX_AMPLIFIER = MAX_LEVEL - 1;
    /** Last level of the slow linear section (VIII–XXXII). */
    public static final int SLOW_UNTIL_LEVEL = 32;
    public static final double MAX_MULTIPLIER = 1000.0D;
    /** Penalty deaths never apply more than Fraying VI. Higher amplifiers are command-only. */
    public static final int STREAK_MAX_LEVEL = 6;
    private static final double[] EARLY = {1.5D, 2.0D, 2.5D, 3.0D, 4.0D, 5.0D, 6.0D};
    private static final double SLOW_STEP = 0.2D;

    private Fraying() {
    }

    public static double multiplierForAmplifier(int amplifier) {
        int level = Math.min(Math.max(amplifier, 0), MAX_AMPLIFIER) + 1;
        if (level <= EARLY.length) {
            return EARLY[level - 1];
        }
        if (level <= SLOW_UNTIL_LEVEL) {
            return EARLY[EARLY.length - 1] + SLOW_STEP * (level - EARLY.length);
        }
        return rampToMax(level);
    }

    /**
     * {@code t} is 0 at {@link #SLOW_UNTIL_LEVEL} and 1 at {@link #MAX_LEVEL}.
     * {@code t²} in the exponent keeps mid-levels tame and 255 huge.
     */
    private static double rampToMax(int level) {
        double start = EARLY[EARLY.length - 1] + SLOW_STEP * (SLOW_UNTIL_LEVEL - EARLY.length);
        double t = (level - SLOW_UNTIL_LEVEL) / (double) (MAX_LEVEL - SLOW_UNTIL_LEVEL);
        return start * Math.exp(Math.log(MAX_MULTIPLIER / start) * t * t);
    }

    public static int amplifierForStreak(int deathStreak) {
        int level = Math.min(Math.max(deathStreak, 1), STREAK_MAX_LEVEL);
        return level - 1;
    }

    /**
     * @param roll unit interval [0, 1) used for the fractional extra-damage chance
     */
    public static int applyMultiplier(int amount, double multiplier, double roll) {
        if (amount <= 0) {
            return amount;
        }
        double scaled = amount * Math.max(multiplier, 1.0D);
        if (scaled >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        double extraExact = scaled - amount;
        int extra = (int) Math.floor(extraExact);
        double remainder = extraExact - extra;
        if (remainder > 0.0D && roll < remainder) {
            extra++;
        }
        long total = (long) amount + extra;
        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    public static boolean shouldAccelerate(@Nullable PlayerEntity player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) {
            return false;
        }
        if (player.isCreative() || player.isSpectator()) {
            return false;
        }
        if (!stack.isDamageable()) {
            return false;
        }
        return player.hasStatusEffect(ModEffects.FRAYING);
    }

    public static int scaleAfterUnbreaking(@Nullable ServerPlayerEntity player, ItemStack stack, int amount, double roll) {
        if (amount <= 0 || !shouldAccelerate(player, stack)) {
            return amount;
        }
        StatusEffectInstance instance = player.getStatusEffect(ModEffects.FRAYING);
        if (instance == null) {
            return amount;
        }
        return applyMultiplier(amount, multiplierForAmplifier(instance.getAmplifier()), roll);
    }

    public static void refresh(ServerPlayerEntity player, DeathPenaltyState state) {
        if (Sanctuary.isActive(state)) {
            remove(player);
            state.frayingReapplyCooldown = 0;
            return;
        }

        int amplifier = amplifierForStreak(state.deathStreak);
        if (!needsReapply(player, amplifier) && state.frayingReapplyCooldown > 0) {
            state.frayingReapplyCooldown--;
            return;
        }

        apply(player, amplifier, DeathPenaltyConfig.FRAYING_ACTIVE_DURATION_TICKS);
        state.frayingReapplyCooldown = DeathPenaltyConfig.FRAYING_REAPPLY_INTERVAL;
    }

    public static void applyLinger(ServerPlayerEntity player) {
        apply(player, 0, DeathPenaltyConfig.FRAYING_LINGER_TICKS);
    }

    public static void remove(ServerPlayerEntity player) {
        player.removeStatusEffect(ModEffects.FRAYING);
    }

    private static void apply(ServerPlayerEntity player, int amplifier, int durationTicks) {
        StatusEffectInstance current = player.getStatusEffect(ModEffects.FRAYING);
        if (current != null && current.getAmplifier() != amplifier) {
            player.removeStatusEffect(ModEffects.FRAYING);
        }
        player.addStatusEffect(new StatusEffectInstance(
                ModEffects.FRAYING,
                durationTicks,
                amplifier,
                false,
                false,
                true
        ));
    }

    private static boolean needsReapply(ServerPlayerEntity player, int amplifier) {
        StatusEffectInstance instance = player.getStatusEffect(ModEffects.FRAYING);
        if (instance == null) {
            return true;
        }
        if (instance.getAmplifier() != amplifier) {
            return true;
        }
        return instance.getDuration() < 20 * 5;
    }
}
