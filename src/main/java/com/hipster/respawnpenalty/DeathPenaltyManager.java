package com.hipster.respawnpenalty;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public final class DeathPenaltyManager {
    private DeathPenaltyManager() {
    }

    public static void onRespawn(ServerPlayerEntity oldPlayer, ServerPlayerEntity newPlayer, boolean alive) {
        if (alive || shouldIgnore(newPlayer)) {
            reapplyActivePenalty(newPlayer);
            return;
        }

        DeathPenaltyState state = readState(newPlayer);
        long now = newPlayer.getWorld().getTime();
        boolean recentDeath = state.lastDeathGameTime > 0L
                && now - state.lastDeathGameTime <= DeathPenaltyConfig.STREAK_WINDOW_TICKS;

        if (!state.penaltyActive) {
            state.originalMaxHealth = safeMaxHealth(getCurrentMaxHealth(newPlayer));
        }

        state.deathStreak = recentDeath ? Math.max(1, state.deathStreak + 1) : 1;
        state.penaltyActive = true;
        state.aliveTicks = 0;
        state.recoveryLockTicks = Math.max(state.recoveryLockTicks,
                DeathPenaltyConfig.recoveryLockForStreak(state.deathStreak));
        state.lastDeathGameTime = now;
        state.sleepSucceeded = false;
        state.effectReapplyCooldown = 0;
        state.frayingReapplyCooldown = 0;

        DeathZone.markDeath(state, newPlayer, now);

        writeState(newPlayer, state);
        applyStructuralPenalty(newPlayer, state, true);

        long zoneTime = newPlayer.getWorld().getTime();
        Sanctuary.startIfNeeded(newPlayer, state);

        DeathZone.PresenceChange change = DeathZone.updatePresence(state, newPlayer, zoneTime);
        if (change == DeathZone.PresenceChange.ENTERED && !Sanctuary.isActive(state)) {
            PenaltyFeedback.spawnedInDeathZone(newPlayer);
        }

        PenaltyEffects.refreshForLocation(
                newPlayer,
                state,
                DeathZone.contains(state, newPlayer, zoneTime),
                true,
                Sanctuary.isActive(state)
        );
        Fraying.refresh(newPlayer, state);
        writeState(newPlayer, state);
    }

    public static void onJoin(ServerPlayerEntity player) {
        reapplyActivePenalty(player);
    }

    public static void tick(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (shouldIgnore(player)) {
                continue;
            }

            DeathPenaltyState state = readState(player);
            if (!state.penaltyActive) {
                if (state.sanctuaryTicks > 0) {
                    state.sanctuaryTicks = 0;
                    writeState(player, state);
                }
                continue;
            }

            state.aliveTicks++;
            if (state.recoveryLockTicks > 0) {
                state.recoveryLockTicks--;
            }

            if (player.isSleeping() && player.getSleepTimer() >= DeathPenaltyConfig.SLEEP_COMPLETE_TIMER) {
                state.sleepSucceeded = true;
            }

            long gameTime = player.getWorld().getTime();

            if (DeathZone.expireIfNeeded(state, player, gameTime)) {
                PenaltyFeedback.deathSiteFaded(player);
            }

            boolean protectionEnded = Sanctuary.tick(player, state);
            if (protectionEnded && DeathZone.contains(state, player, gameTime)) {
                PenaltyFeedback.protectionEndedNearDeathSite(player);
            }

            DeathZone.PresenceChange change = DeathZone.updatePresence(state, player, gameTime);
            if (change == DeathZone.PresenceChange.ENTERED && !Sanctuary.isActive(state)) {
                PenaltyFeedback.enteredDeathZone(player);
            } else if (change == DeathZone.PresenceChange.LEFT) {
                PenaltyFeedback.leftDeathZone(player);
                PenaltyEffects.clear(player);
            }

            boolean inZone = DeathZone.contains(state, player, gameTime);
            PenaltyEffects.refreshForLocation(
                    player,
                    state,
                    inZone,
                    protectionEnded,
                    Sanctuary.isActive(state)
            );
            Fraying.refresh(player, state);

            if (state.aliveTicks >= DeathPenaltyConfig.RECOVERY_TICKS) {
                stepDown(player, state);
                continue;
            }

            writeState(player, state);
        }
    }

    public static PlayerEntity.SleepFailureReason onTrySleep(ServerPlayerEntity player) {
        if (shouldIgnore(player)) {
            return null;
        }

        DeathPenaltyState state = readState(player);
        if (!state.penaltyActive) {
            return null;
        }

        if (state.recoveryLockTicks > 0) {
            PenaltyFeedback.sleepLocked(player, state.recoveryLockTicks);
            return PlayerEntity.SleepFailureReason.OTHER_PROBLEM;
        }

        state.sleepSucceeded = false;
        writeState(player, state);
        return null;
    }

    public static void onStopSleeping(ServerPlayerEntity player) {
        if (shouldIgnore(player)) {
            return;
        }

        DeathPenaltyState state = readState(player);
        if (!state.penaltyActive) {
            return;
        }

        boolean completed = state.sleepSucceeded
                || player.getSleepTimer() >= DeathPenaltyConfig.SLEEP_COMPLETE_TIMER;
        state.sleepSucceeded = false;

        if (!completed) {
            writeState(player, state);
            return;
        }

        if (state.recoveryLockTicks > 0) {
            writeState(player, state);
            return;
        }

        stepDown(player, state);
    }

    public static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
        return Sanctuary.allowDamage(entity, source, DeathPenaltyManager::readState);
    }

    /**
     * @return true if the player currently has an active death penalty
     */
    public static boolean hasActivePenalty(ServerPlayerEntity player) {
        if (shouldIgnore(player)) {
            return false;
        }
        return readState(player).penaltyActive;
    }

    /**
     * Holy flask: full clear if a penalty is active.
     * @return true if the flask did something
     */
    public static boolean tryClearWithFlask(ServerPlayerEntity player) {
        if (!hasActivePenalty(player)) {
            return false;
        }
        recover(player, readState(player), true);
        return true;
    }

    private static void stepDown(ServerPlayerEntity player, DeathPenaltyState state) {
        state.deathStreak = Math.max(0, state.deathStreak - 1);
        state.aliveTicks = 0;
        state.sanctuaryTicks = 0;
        state.effectReapplyCooldown = 0;
        state.frayingReapplyCooldown = 0;
        DeathZone.clear(state, player);
        PenaltyEffects.clear(player);

        if (state.deathStreak <= 0) {
            recover(player, state, false);
            return;
        }

        state.penaltyActive = true;
        applyStructuralPenalty(player, state, false);
        Fraying.refresh(player, state);
        writeState(player, state);
        PenaltyFeedback.penaltySteppedDown(player, state.deathStreak);
    }

    private static void reapplyActivePenalty(ServerPlayerEntity player) {
        if (shouldIgnore(player)) {
            return;
        }

        DeathPenaltyState state = readState(player);
        if (!state.penaltyActive) {
            return;
        }

        applyStructuralPenalty(player, state, false);
        long gameTime = player.getWorld().getTime();
        DeathZone.expireIfNeeded(state, player, gameTime);
        DeathZone.updatePresence(state, player, gameTime);
        boolean inZone = DeathZone.contains(state, player, gameTime);
        PenaltyEffects.refreshForLocation(player, state, inZone, true, Sanctuary.isActive(state));
        Fraying.refresh(player, state);
        writeState(player, state);
    }

    private static void applyStructuralPenalty(
            ServerPlayerEntity player,
            DeathPenaltyState state,
            boolean freshDeath
    ) {
        DeathPenaltyConfig.PenaltyTier tier = DeathPenaltyConfig.tierForStreak(state.deathStreak);

        setMaxHealth(player, tier.maxHealth());
        player.setHealth(Math.min(player.getHealth(), (float) tier.maxHealth()));
        if (player.getHealth() <= 0.0F) {
            player.setHealth(1.0F);
        }

        if (freshDeath) {
            player.getHungerManager().setFoodLevel(tier.foodLevel());
            player.getHungerManager().setSaturationLevel(0.0F);
            PenaltyFeedback.penaltyApplied(player, state.deathStreak, state.recoveryLockTicks);
        }
    }

    private static void recover(ServerPlayerEntity player, DeathPenaltyState state, boolean fromFlask) {
        double restoredMaxHealth = safeMaxHealth(state.originalMaxHealth);
        setMaxHealth(player, restoredMaxHealth);
        player.setHealth((float) Math.min(restoredMaxHealth, Math.max(player.getHealth(), 1.0F)));
        PenaltyEffects.clear(player);
        DeathZone.clear(state, player);

        state.penaltyActive = false;
        state.aliveTicks = 0;
        state.recoveryLockTicks = 0;
        state.deathStreak = 0;
        state.originalMaxHealth = restoredMaxHealth;
        state.sanctuaryTicks = 0;
        state.sleepSucceeded = false;
        state.effectReapplyCooldown = 0;
        state.frayingReapplyCooldown = 0;
        writeState(player, state);

        if (fromFlask) {
            Fraying.remove(player);
            PenaltyFeedback.flaskCleared(player);
        } else {
            Fraying.applyLinger(player);
            PenaltyFeedback.recovered(player);
        }
    }

    private static boolean shouldIgnore(ServerPlayerEntity player) {
        return DeathPenaltyConfig.IGNORE_CREATIVE_AND_SPECTATOR
                && (player.isCreative() || player.isSpectator());
    }

    private static void setMaxHealth(ServerPlayerEntity player, double value) {
        EntityAttributeInstance attribute = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (attribute != null) {
            attribute.setBaseValue(value);
        }
    }

    private static double getCurrentMaxHealth(ServerPlayerEntity player) {
        EntityAttributeInstance attribute = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        return attribute == null ? 20.0D : attribute.getBaseValue();
    }

    private static double safeMaxHealth(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value < 1.0D) {
            return 20.0D;
        }
        return value;
    }

    private static DeathPenaltyState readState(ServerPlayerEntity player) {
        return DeathPenaltyStateStore.get(player.getServer(), player.getUuid());
    }

    private static void writeState(ServerPlayerEntity player, DeathPenaltyState state) {
        DeathPenaltyStateStore.put(player.getServer(), player.getUuid(), state);
    }
}
