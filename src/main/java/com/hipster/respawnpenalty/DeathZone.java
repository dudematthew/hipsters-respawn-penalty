package com.hipster.respawnpenalty;

import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * Day-long soft zone around the death point. Spatial pressure only — global attrition lives elsewhere.
 */
public final class DeathZone {
    public enum PresenceChange {
        NONE,
        ENTERED,
        LEFT
    }

    private static final Identifier ZONE_ATTACK_MODIFIER_ID =
            Identifier.of(HipstersRespawnPenalty.MOD_ID, "death_zone_attack");

    private DeathZone() {
    }

    public static void markDeath(DeathPenaltyState state, ServerPlayerEntity player, long gameTime) {
        DeathLocationTracker.applyToState(state, player, gameTime);
    }

    public static boolean isActive(DeathPenaltyState state, long gameTime) {
        return state.hasDeathZoneData() && gameTime < state.zoneExpireGameTime;
    }

    /**
     * Clears an expired death zone.
     * @return true if the zone just faded this call
     */
    public static boolean expireIfNeeded(DeathPenaltyState state, ServerPlayerEntity player, long gameTime) {
        if (!state.hasDeathZoneData()) {
            return false;
        }
        if (isActive(state, gameTime)) {
            return false;
        }
        clear(state, player);
        return true;
    }

    public static boolean contains(DeathPenaltyState state, ServerPlayerEntity player, long gameTime) {
        if (!isActive(state, gameTime)) {
            return false;
        }
        String dimension = player.getWorld().getRegistryKey().getValue().toString();
        if (!dimension.equals(state.deathDimension)) {
            return false;
        }

        BlockPos death = state.deathBlockPos();
        return isInside(
                death.getX() + 0.5D, death.getY(), death.getZ() + 0.5D,
                player.getX(), player.getY(), player.getZ(),
                DeathPenaltyConfig.DEATH_ZONE_RADIUS,
                DeathPenaltyConfig.DEATH_ZONE_Y_SLACK
        );
    }

    /** Pure geometry helper — kept package-visible for unit tests. */
    static boolean isInside(
            double deathX, double deathY, double deathZ,
            double playerX, double playerY, double playerZ,
            double radius, double ySlack
    ) {
        double dx = playerX - deathX;
        double dz = playerZ - deathZ;
        double dy = Math.abs(playerY - deathY);
        return dx * dx + dz * dz <= radius * radius && dy <= ySlack;
    }

    public static PresenceChange updatePresence(DeathPenaltyState state, ServerPlayerEntity player, long gameTime) {
        if (!isActive(state, gameTime)) {
            if (state.wasInDeathZone) {
                state.wasInDeathZone = false;
                clearAttackPenalty(player);
                return PresenceChange.LEFT;
            }
            return PresenceChange.NONE;
        }

        boolean inside = contains(state, player, gameTime);
        boolean entered = inside && !state.wasInDeathZone;
        boolean left = !inside && state.wasInDeathZone;
        state.wasInDeathZone = inside;

        if (entered) {
            applyAttackPenalty(player);
            return PresenceChange.ENTERED;
        }
        if (left) {
            clearAttackPenalty(player);
            return PresenceChange.LEFT;
        }
        if (inside) {
            ensureAttackPenalty(player);
        }
        return PresenceChange.NONE;
    }

    public static void clear(DeathPenaltyState state, ServerPlayerEntity player) {
        state.clearDeathZone();
        clearAttackPenalty(player);
    }

    private static void applyAttackPenalty(ServerPlayerEntity player) {
        clearAttackPenalty(player);
        EntityAttributeInstance attack = player.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (attack == null) {
            return;
        }
        double factor = DeathPenaltyConfig.ZONE_ATTACK_DAMAGE_FACTOR;
        attack.addTemporaryModifier(new EntityAttributeModifier(
                ZONE_ATTACK_MODIFIER_ID,
                factor - 1.0D,
                EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        ));
    }

    private static void ensureAttackPenalty(ServerPlayerEntity player) {
        EntityAttributeInstance attack = player.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (attack == null) {
            return;
        }
        if (attack.getModifier(ZONE_ATTACK_MODIFIER_ID) == null) {
            applyAttackPenalty(player);
        }
    }

    private static void clearAttackPenalty(ServerPlayerEntity player) {
        EntityAttributeInstance attack = player.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (attack != null) {
            attack.removeModifier(ZONE_ATTACK_MODIFIER_ID);
        }
    }
}
