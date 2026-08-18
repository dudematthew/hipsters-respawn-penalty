package com.hipster.respawnpenalty;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Short respawn-only window: ignore incoming damage, block outgoing damage,
 * and deny interactions so protection cannot be used to loot.
 */
public final class Sanctuary {
    private Sanctuary() {
    }

    public static void startIfNeeded(ServerPlayerEntity player, DeathPenaltyState state) {
        if (!diedNearRespawn(state, player)) {
            state.sanctuaryTicks = 0;
            return;
        }
        state.sanctuaryTicks = DeathPenaltyConfig.SANCTUARY_TICKS;
        PenaltyFeedback.protectionStarted(player, state.deathStreak);
        PenaltyFeedback.protectionTickHint(player, state.sanctuaryTicks);
    }

    /**
     * Sanctuary is about dying on top of spawn, not about the death-zone cylinder.
     */
    static boolean diedNearRespawn(DeathPenaltyState state, ServerPlayerEntity player) {
        if (!state.hasDeathZoneData()) {
            return false;
        }
        String dimension = player.getWorld().getRegistryKey().getValue().toString();
        if (!dimension.equals(state.deathDimension)) {
            return false;
        }
        BlockPos death = state.deathBlockPos();
        return isWithinRadius(
                death.getX() + 0.5D, death.getY(), death.getZ() + 0.5D,
                player.getX(), player.getY(), player.getZ(),
                DeathPenaltyConfig.SANCTUARY_RADIUS
        );
    }

    static boolean isWithinRadius(
            double deathX, double deathY, double deathZ,
            double playerX, double playerY, double playerZ,
            double radius
    ) {
        double dx = playerX - deathX;
        double dy = playerY - deathY;
        double dz = playerZ - deathZ;
        return dx * dx + dy * dy + dz * dz <= radius * radius;
    }

    /**
     * @return true if protection just ended this tick
     */
    public static boolean tick(ServerPlayerEntity player, DeathPenaltyState state) {
        if (state.sanctuaryTicks <= 0) {
            return false;
        }
        state.sanctuaryTicks--;
        if (state.sanctuaryTicks > 0) {
            PenaltyFeedback.protectionTickHint(player, state.sanctuaryTicks);
            return false;
        }
        return true;
    }

    public static boolean isActive(DeathPenaltyState state) {
        return state.sanctuaryTicks > 0;
    }

    public static boolean isActive(ServerPlayerEntity player) {
        return isActive(DeathPenaltyStateStore.get(player.getServer(), player.getUuid()));
    }

    /**
     * @return true if the interaction should be cancelled
     */
    public static boolean blockInteraction(PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return false;
        }
        if (!isActive(serverPlayer)) {
            return false;
        }
        PenaltySounds.blocked(serverPlayer);
        return true;
    }

    public static ActionResult onUseBlock(PlayerEntity player, World world, Hand hand, BlockHitResult hitResult) {
        return blockInteraction(player) ? ActionResult.FAIL : ActionResult.PASS;
    }

    public static boolean onBreakBlock(World world, PlayerEntity player, net.minecraft.util.math.BlockPos pos,
                                       net.minecraft.block.BlockState state, net.minecraft.block.entity.BlockEntity blockEntity) {
        return !blockInteraction(player);
    }

    public static ActionResult onUseEntity(PlayerEntity player, World world, Hand hand, Entity entity,
                                           EntityHitResult hitResult) {
        if (entity instanceof PlayerEntity) {
            return ActionResult.PASS;
        }
        return blockInteraction(player) ? ActionResult.FAIL : ActionResult.PASS;
    }

    /**
     * @return false to cancel damage
     */
    public static boolean allowDamage(LivingEntity entity, DamageSource source, DeathPenaltyStateLookup lookup) {
        if (entity instanceof ServerPlayerEntity player) {
            DeathPenaltyState state = lookup.get(player);
            if (isActive(state)) {
                return false;
            }
        }

        if (source.getAttacker() instanceof ServerPlayerEntity attacker) {
            DeathPenaltyState state = lookup.get(attacker);
            if (isActive(state)) {
                return false;
            }
        }

        return true;
    }

    @FunctionalInterface
    public interface DeathPenaltyStateLookup {
        DeathPenaltyState get(ServerPlayerEntity player);
    }
}
