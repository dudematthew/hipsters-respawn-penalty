package com.hipster.respawnpenalty;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Captures death coordinates before respawn rewrites the player entity position.
 */
public final class DeathLocationTracker {
    private record Stash(String dimensionId, BlockPos pos) {
    }

    private static final Map<UUID, Stash> PENDING = new ConcurrentHashMap<>();

    private DeathLocationTracker() {
    }

    public static void onDeath(LivingEntity entity, DamageSource damageSource) {
        if (!(entity instanceof ServerPlayerEntity player)) {
            return;
        }
        if (DeathPenaltyConfig.IGNORE_CREATIVE_AND_SPECTATOR
                && (player.isCreative() || player.isSpectator())) {
            return;
        }
        PENDING.put(
                player.getUuid(),
                new Stash(
                        player.getWorld().getRegistryKey().getValue().toString(),
                        player.getBlockPos().toImmutable()
                )
        );
    }

    /**
     * Prefer vanilla last-death location, then the pre-respawn stash, never the respawn bed position.
     */
    public static void applyToState(DeathPenaltyState state, ServerPlayerEntity player, long gameTime) {
        long expire = gameTime + DeathPenaltyConfig.DEATH_ZONE_LIFETIME_TICKS;

        Optional<GlobalPos> lastDeath = player.getLastDeathPos();
        if (lastDeath.isPresent()) {
            GlobalPos globalPos = lastDeath.get();
            state.setDeathLocation(
                    globalPos.dimension().getValue().toString(),
                    globalPos.pos(),
                    expire
            );
            PENDING.remove(player.getUuid());
            return;
        }

        Stash stash = PENDING.remove(player.getUuid());
        if (stash != null) {
            state.setDeathLocation(stash.dimensionId(), stash.pos(), expire);
        }
    }
}
