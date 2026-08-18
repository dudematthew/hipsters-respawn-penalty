package com.hipster.respawnpenalty;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

/**
 * Short vanilla cues paired with penalty feedback.
 */
public final class PenaltySounds {
    private PenaltySounds() {
    }

    public static void penaltyApplied(ServerPlayerEntity player) {
        play(player, SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, 0.55F, 0.75F);
    }

    public static void protectionStarted(ServerPlayerEntity player) {
        play(player, SoundEvents.BLOCK_BEACON_ACTIVATE, 0.5F, 1.1F);
    }

    public static void protectionEnded(ServerPlayerEntity player) {
        play(player, SoundEvents.BLOCK_BEACON_DEACTIVATE, 0.5F, 1.0F);
    }

    public static void enteredDeathSite(ServerPlayerEntity player) {
        play(player, SoundEvents.AMBIENT_CAVE.value(), 0.45F, 0.85F);
    }

    public static void blocked(ServerPlayerEntity player) {
        play(player, SoundEvents.ENTITY_VILLAGER_NO, 0.6F, 0.9F);
    }

    public static void deathSiteFaded(ServerPlayerEntity player) {
        play(player, SoundEvents.BLOCK_FIRE_EXTINGUISH, 0.45F, 1.0F);
    }

    public static void recovered(ServerPlayerEntity player) {
        play(player, SoundEvents.ENTITY_PLAYER_LEVELUP, 0.45F, 1.2F);
    }

    public static void steppedDown(ServerPlayerEntity player) {
        play(player, SoundEvents.ENTITY_PLAYER_LEVELUP, 0.3F, 1.5F);
    }

    public static void flaskDrink(ServerPlayerEntity player) {
        play(player, SoundEvents.ENTITY_GENERIC_DRINK, 0.5F, 1.0F);
    }

    /** After a successful holy flask drink. */
    public static void flaskFinished(ServerPlayerEntity player) {
        play(player, SoundEvents.ITEM_BOTTLE_EMPTY, 0.55F, 1.0F);
        play(player, SoundEvents.BLOCK_BEACON_POWER_SELECT, 0.45F, 1.35F);
    }

    /** After drinking with nothing to clear. */
    public static void flaskNoEffect(ServerPlayerEntity player) {
        play(player, SoundEvents.BLOCK_FIRE_EXTINGUISH, 0.5F, 1.2F);
        play(player, SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 0.35F, 0.5F);
    }

    private static void play(ServerPlayerEntity player, SoundEvent sound, float volume, float pitch) {
        player.playSoundToPlayer(sound, SoundCategory.PLAYERS, volume, pitch);
    }
}
