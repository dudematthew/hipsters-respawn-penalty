package com.hipster.respawnpenalty;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeathPenaltyLogicTest {

    @Test
    void deathZoneContainsCenterAndEdgeButNotOutside() {
        double radius = DeathPenaltyConfig.DEATH_ZONE_RADIUS;
        double ySlack = DeathPenaltyConfig.DEATH_ZONE_Y_SLACK;

        assertEquals(60.0D, radius);
        assertTrue(DeathZone.isInside(0, 64, 0, 0.5, 64, 0.5, radius, ySlack));
        assertTrue(DeathZone.isInside(0, 64, 0, 60.0, 64, 0.0, radius, ySlack));
        assertFalse(DeathZone.isInside(0, 64, 0, 60.1, 64, 0.0, radius, ySlack));
        assertFalse(DeathZone.isInside(0, 64, 0, 0.0, 64 + 24.1, 0.0, radius, ySlack));
        assertTrue(DeathZone.isInside(0, 64, 0, 0.0, 64 + 24.0, 0.0, radius, ySlack));
    }

    @Test
    void sanctuaryUsesTwentyBlockRadiusNotDeathZone() {
        assertEquals(20.0D, DeathPenaltyConfig.SANCTUARY_RADIUS);
        assertTrue(Sanctuary.isWithinRadius(0, 64, 0, 20.0, 64, 0.0, 20.0));
        assertFalse(Sanctuary.isWithinRadius(0, 64, 0, 20.1, 64, 0.0, 20.0));
        assertFalse(Sanctuary.isWithinRadius(0, 64, 0, 0.0, 64 + 20.1, 0.0, 20.0));
        assertTrue(Sanctuary.isWithinRadius(0, 64, 0, 0.0, 84.0, 0.0, 20.0));
    }

    @Test
    void recoveryLockScalesWithStreak() {
        assertEquals(0, DeathPenaltyConfig.recoveryLockForStreak(1));
        assertEquals(0, DeathPenaltyConfig.recoveryLockForStreak(3));
        assertEquals(DeathPenaltyConfig.MODERATE_RECOVERY_LOCK_TICKS,
                DeathPenaltyConfig.recoveryLockForStreak(4));
        assertEquals(DeathPenaltyConfig.CRITICAL_RECOVERY_LOCK_TICKS,
                DeathPenaltyConfig.recoveryLockForStreak(6));
    }

    @Test
    void zoneEffectiveStreakIsOneHigherCapped() {
        assertEquals(2, DeathPenaltyConfig.zoneEffectiveStreak(1));
        assertEquals(4, DeathPenaltyConfig.zoneEffectiveStreak(3));
        assertEquals(6, DeathPenaltyConfig.zoneEffectiveStreak(6));
        assertEquals(6, DeathPenaltyConfig.zoneEffectiveStreak(99));
    }

    @Test
    void zonePackageAtEffectiveStreakIsStrongerThanBaseStreak() {
        DeathPenaltyConfig.EffectPackage base = DeathPenaltyConfig.zoneEffects(2);
        DeathPenaltyConfig.EffectPackage boosted = DeathPenaltyConfig.zoneEffects(
                DeathPenaltyConfig.zoneEffectiveStreak(2));
        assertTrue(boosted.weaknessTicks() >= base.weaknessTicks());
        assertTrue(boosted.slownessTicks() >= base.slownessTicks());
    }

    @Test
    void recoveryTicksIsOneMinecraftDay() {
        assertEquals(24000, DeathPenaltyConfig.RECOVERY_TICKS);
    }

    @Test
    void stateRoundTripsDeathZoneFields() {
        DeathPenaltyState state = new DeathPenaltyState();
        state.penaltyActive = true;
        state.deathStreak = 3;
        state.setDeathLocation("minecraft:overworld",
                new net.minecraft.util.math.BlockPos(10, 70, -4), 12345L);
        state.wasInDeathZone = true;
        state.sanctuaryTicks = 40;

        DeathPenaltyState loaded = DeathPenaltyState.fromNbt(state.toNbt());
        assertTrue(loaded.hasDeathZoneData());
        assertEquals("minecraft:overworld", loaded.deathDimension);
        assertEquals(10, loaded.deathX);
        assertEquals(70, loaded.deathY);
        assertEquals(-4, loaded.deathZ);
        assertEquals(12345L, loaded.zoneExpireGameTime);
        assertTrue(loaded.wasInDeathZone);
        assertEquals(40, loaded.sanctuaryTicks);
        assertEquals(3, loaded.deathStreak);
    }

    @Test
    void formatTicksUsesFullWords() {
        assertEquals("1 minute", PenaltyFeedback.formatTicks(20 * 60));
        assertEquals("5 seconds", PenaltyFeedback.formatTicks(20 * 5));
    }

    @Test
    void frayingMultiplierTableMatchesPlan() {
        assertEquals(1.5D, Fraying.multiplierForAmplifier(0));
        assertEquals(2.0D, Fraying.multiplierForAmplifier(1));
        assertEquals(2.5D, Fraying.multiplierForAmplifier(2));
        assertEquals(3.0D, Fraying.multiplierForAmplifier(3));
        assertEquals(4.0D, Fraying.multiplierForAmplifier(4));
        assertEquals(5.0D, Fraying.multiplierForAmplifier(5));
        assertEquals(6.0D, Fraying.multiplierForAmplifier(6));
        assertEquals(6.2D, Fraying.multiplierForAmplifier(7), 1e-9);
        assertEquals(11.0D, Fraying.multiplierForAmplifier(Fraying.SLOW_UNTIL_LEVEL - 1), 1e-9);
        assertEquals(Fraying.MAX_MULTIPLIER, Fraying.multiplierForAmplifier(Fraying.MAX_AMPLIFIER), 1e-6);
        assertTrue(Fraying.multiplierForAmplifier(20) > Fraying.multiplierForAmplifier(6));
        assertTrue(Fraying.multiplierForAmplifier(98) < Fraying.multiplierForAmplifier(Fraying.MAX_AMPLIFIER));
    }

    @Test
    void frayingMultiplierNeverDecreasesWithAmplifier() {
        double previous = 0.0D;
        for (int amplifier = 0; amplifier <= Fraying.MAX_AMPLIFIER; amplifier++) {
            double next = Fraying.multiplierForAmplifier(amplifier);
            assertTrue(next >= previous - 1e-9, "amp " + amplifier);
            previous = next;
        }
    }

    @Test
    void frayingAmplifierFollowsPenaltyLevel() {
        assertEquals(0, Fraying.amplifierForStreak(1));
        assertEquals(2, Fraying.amplifierForStreak(3));
        assertEquals(5, Fraying.amplifierForStreak(6));
        assertEquals(5, Fraying.amplifierForStreak(99));
        assertEquals(5, Fraying.amplifierForStreak(255));
        assertEquals(Fraying.STREAK_MAX_LEVEL - 1, Fraying.amplifierForStreak(999));
    }

    @Test
    void frayingApplyMultiplierUsesFloorPlusRemainderChance() {
        assertEquals(0, Fraying.applyMultiplier(0, 1.5D, 0.0D));
        assertEquals(2, Fraying.applyMultiplier(1, 1.5D, 0.0D));
        assertEquals(1, Fraying.applyMultiplier(1, 1.5D, 0.5D));
        assertEquals(1, Fraying.applyMultiplier(1, 1.5D, 0.9D));
        assertEquals(4, Fraying.applyMultiplier(2, 2.0D, 0.0D));
        assertEquals(5, Fraying.applyMultiplier(2, 2.5D, 0.0D));
        assertEquals(3, Fraying.applyMultiplier(1, 2.5D, 0.0D));
        assertEquals(2, Fraying.applyMultiplier(1, 2.5D, 0.9D));
        assertEquals(10, Fraying.applyMultiplier(2, 5.0D, 0.0D));
        assertEquals(1000, Fraying.applyMultiplier(1, 1000.0D, 0.0D));
        assertEquals(Integer.MAX_VALUE, Fraying.applyMultiplier(2_000_000, 2000.0D, 0.0D));
    }

    @Test
    void frayingLingerIsThreeMinutes() {
        assertEquals(20 * 60 * 3, DeathPenaltyConfig.FRAYING_LINGER_TICKS);
    }
}
