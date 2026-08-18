package com.hipster.respawnpenalty;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

import java.util.UUID;

public final class DeathPenaltyStateStore {
    private DeathPenaltyStateStore() {
    }

    public static DeathPenaltyState get(MinecraftServer server, UUID uuid) {
        return persistentState(server).get(uuid);
    }

    public static void put(MinecraftServer server, UUID uuid, DeathPenaltyState state) {
        persistentState(server).put(uuid, state);
    }

    public static void remove(MinecraftServer server, UUID uuid) {
        persistentState(server).remove(uuid);
    }

    private static DeathPenaltyPersistentState persistentState(MinecraftServer server) {
        ServerWorld overworld = server.getWorld(World.OVERWORLD);
        if (overworld == null) {
            throw new IllegalStateException("Overworld is not loaded; cannot access death penalty state.");
        }
        PersistentStateManager manager = overworld.getPersistentStateManager();
        DeathPenaltyPersistentState current = manager.get(
                DeathPenaltyPersistentState.TYPE,
                HipstersRespawnPenalty.MOD_ID
        );
        if (current != null) {
            return current;
        }
        DeathPenaltyPersistentState legacy = manager.get(
                DeathPenaltyPersistentState.TYPE,
                HipstersRespawnPenalty.LEGACY_STATE_ID
        );
        if (legacy != null) {
            manager.set(HipstersRespawnPenalty.MOD_ID, legacy);
            legacy.markDirty();
            return legacy;
        }
        return manager.getOrCreate(
                DeathPenaltyPersistentState.TYPE,
                HipstersRespawnPenalty.MOD_ID
        );
    }
}
