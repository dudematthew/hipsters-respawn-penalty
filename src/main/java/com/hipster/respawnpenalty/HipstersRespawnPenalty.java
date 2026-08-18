package com.hipster.respawnpenalty;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hipster's Respawn Penalty. Derived from Hoaug's Respawn Penalty (MIT).
 */
public final class HipstersRespawnPenalty implements ModInitializer {
    public static final String MOD_ID = "hipsters_respawn_penalty";
    /** World save id used by the original Hoaug build. Loaded once, then rewritten under {@link #MOD_ID}. */
    public static final String LEGACY_STATE_ID = "hoaug_death_penalty";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        DeathPenaltyConfig.load();
        ModItems.register();
        ModEffects.register();

        ServerPlayerEvents.AFTER_RESPAWN.register(DeathPenaltyManager::onRespawn);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                DeathPenaltyManager.onJoin(handler.player));
        ServerTickEvents.END_SERVER_TICK.register(DeathPenaltyManager::tick);

        ServerLivingEntityEvents.AFTER_DEATH.register(DeathLocationTracker::onDeath);

        EntitySleepEvents.ALLOW_SLEEPING.register((entity, sleepingPos) -> {
            if (entity instanceof ServerPlayerEntity player) {
                return DeathPenaltyManager.onTrySleep(player);
            }
            return null;
        });

        EntitySleepEvents.STOP_SLEEPING.register((entity, sleepingPos) -> {
            if (entity instanceof ServerPlayerEntity player) {
                DeathPenaltyManager.onStopSleeping(player);
            }
        });

        ServerLivingEntityEvents.ALLOW_DAMAGE.register(DeathPenaltyManager::allowDamage);

        UseBlockCallback.EVENT.register(Sanctuary::onUseBlock);
        PlayerBlockBreakEvents.BEFORE.register(Sanctuary::onBreakBlock);
        UseEntityCallback.EVENT.register(Sanctuary::onUseEntity);

        LOGGER.info("Hipster's Respawn Penalty initialized.");
    }
}
