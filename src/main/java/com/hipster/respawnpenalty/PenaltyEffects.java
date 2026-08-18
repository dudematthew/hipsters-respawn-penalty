package com.hipster.respawnpenalty;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Zone-only potion pressure. Outside the death zone, structural HP/hunger is enough.
 */
public final class PenaltyEffects {
    private PenaltyEffects() {
    }

    public static void applyPackage(ServerPlayerEntity player, DeathPenaltyConfig.EffectPackage pack) {
        setOrClear(player, StatusEffects.WEAKNESS, pack.weaknessTicks(), pack.weaknessAmplifier());
        setOrClear(player, StatusEffects.SLOWNESS, pack.slownessTicks(), pack.slownessAmplifier());
        setOrClear(player, StatusEffects.MINING_FATIGUE, pack.miningFatigueTicks(), pack.miningFatigueAmplifier());
        setOrClear(player, StatusEffects.DARKNESS, pack.darknessTicks(), pack.darknessAmplifier());
    }

    public static void clear(ServerPlayerEntity player) {
        player.removeStatusEffect(StatusEffects.WEAKNESS);
        player.removeStatusEffect(StatusEffects.SLOWNESS);
        player.removeStatusEffect(StatusEffects.MINING_FATIGUE);
        player.removeStatusEffect(StatusEffects.DARKNESS);
    }

    public static void refreshForLocation(
            ServerPlayerEntity player,
            DeathPenaltyState state,
            boolean inZone,
            boolean force,
            boolean sanctuaryActive
    ) {
        if (sanctuaryActive) {
            clear(player);
            state.effectReapplyCooldown = 0;
            return;
        }

        if (!inZone) {
            // Outside: let potions fade. No global cadence.
            if (force) {
                clear(player);
            }
            state.effectReapplyCooldown = 0;
            return;
        }

        int effectiveStreak = DeathPenaltyConfig.zoneEffectiveStreak(state.deathStreak);

        if (!force) {
            boolean intervalElapsed = state.effectReapplyCooldown <= 0;
            if (state.effectReapplyCooldown > 0) {
                state.effectReapplyCooldown--;
            }
            if (!intervalElapsed && !needsRefresh(player, effectiveStreak)) {
                return;
            }
        }

        applyPackage(player, DeathPenaltyConfig.zoneEffects(effectiveStreak));
        state.effectReapplyCooldown = DeathPenaltyConfig.ZONE_EFFECT_REAPPLY_INTERVAL;
    }

    private static void setOrClear(
            ServerPlayerEntity player,
            RegistryEntry<StatusEffect> effect,
            int ticks,
            int amplifier
    ) {
        if (ticks <= 0) {
            player.removeStatusEffect(effect);
            return;
        }
        player.addStatusEffect(new StatusEffectInstance(effect, ticks, amplifier, false, false, true));
    }

    private static boolean needsRefresh(ServerPlayerEntity player, int effectiveStreak) {
        DeathPenaltyConfig.EffectPackage expected = DeathPenaltyConfig.zoneEffects(effectiveStreak);
        if (expected.weaknessTicks() > 0) {
            StatusEffectInstance weakness = player.getStatusEffect(StatusEffects.WEAKNESS);
            if (weakness == null || weakness.getDuration() < 20 * 5) {
                return true;
            }
        }
        if (expected.hasMiningFatigue()) {
            StatusEffectInstance fatigue = player.getStatusEffect(StatusEffects.MINING_FATIGUE);
            return fatigue == null || fatigue.getDuration() < 20 * 5;
        }
        return false;
    }
}
