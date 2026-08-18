package com.hipster.respawnpenalty;

import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.world.PersistentState;
import net.minecraft.registry.RegistryWrapper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class DeathPenaltyPersistentState extends PersistentState {
    private static final String PLAYERS_KEY = "Players";

    public static final PersistentState.Type<DeathPenaltyPersistentState> TYPE = new PersistentState.Type<>(
            DeathPenaltyPersistentState::new,
            (nbt, registryLookup) -> DeathPenaltyPersistentState.fromNbt(nbt),
            DataFixTypes.LEVEL
    );

    private final Map<UUID, DeathPenaltyState> players = new HashMap<>();

    public DeathPenaltyState get(UUID uuid) {
        return players.computeIfAbsent(uuid, ignored -> new DeathPenaltyState());
    }

    public void put(UUID uuid, DeathPenaltyState state) {
        players.put(uuid, state);
        markDirty();
    }

    public void remove(UUID uuid) {
        if (players.remove(uuid) != null) {
            markDirty();
        }
    }

    private static DeathPenaltyPersistentState fromNbt(NbtCompound root) {
        DeathPenaltyPersistentState persistentState = new DeathPenaltyPersistentState();
        NbtCompound playersTag = root.getCompound(PLAYERS_KEY);
        for (String key : playersTag.getKeys()) {
            try {
                UUID uuid = UUID.fromString(key);
                NbtElement element = playersTag.get(key);
                if (element instanceof NbtCompound playerTag) {
                    persistentState.players.put(uuid, DeathPenaltyState.fromNbt(playerTag));
                }
            } catch (IllegalArgumentException ignored) {
                // Ignore corrupt/legacy player keys instead of failing world load.
            }
        }
        return persistentState;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        NbtCompound playersTag = new NbtCompound();
        for (Map.Entry<UUID, DeathPenaltyState> entry : players.entrySet()) {
            playersTag.put(entry.getKey().toString(), entry.getValue().toNbt());
        }
        nbt.put(PLAYERS_KEY, playersTag);
        return nbt;
    }
}
