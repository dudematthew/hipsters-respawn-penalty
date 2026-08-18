package com.hipster.respawnpenalty;

import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Titles carry short facts. Chat explains in full sentences.
 */
public final class PenaltyFeedback {
    private PenaltyFeedback() {
    }

    public static void penaltyApplied(ServerPlayerEntity player, int streak, int recoveryLockTicks) {
        showTitle(
                player,
                Text.literal("Penalty").formatted(Formatting.DARK_RED),
                Text.literal("Lv. " + streak).formatted(Formatting.GRAY),
                10, 50, 10
        );
        PenaltySounds.penaltyApplied(player);

        chat(player, Text.empty()
                .append(Text.literal("You are weakened after dying. ").formatted(Formatting.RED))
                .append(Text.literal("Sleep through the night to ease it one step, wait a full day, or drink a holy flask to clear it.")
                        .formatted(Formatting.GRAY)));

        if (recoveryLockTicks > 0) {
            sleepLocked(player, recoveryLockTicks);
        }
    }

    public static void protectionStarted(ServerPlayerEntity player, int streak) {
        showTitle(
                player,
                Text.literal("Penalty").formatted(Formatting.DARK_RED),
                Text.literal("Protected").formatted(Formatting.GOLD),
                5, 40, 10
        );
        PenaltySounds.protectionStarted(player);

        chat(player, Text.empty()
                .append(Text.literal("You respawned where you died. ").formatted(Formatting.GOLD))
                .append(Text.literal("You are protected for a short time. ")
                        .formatted(Formatting.YELLOW))
                .append(Text.literal("You cannot take damage or deal damage until it ends. Use that time to get away.")
                        .formatted(Formatting.GRAY)));
    }

    public static void protectionTickHint(ServerPlayerEntity player, int sanctuaryTicks) {
        if (sanctuaryTicks == DeathPenaltyConfig.SANCTUARY_TICKS
                || sanctuaryTicks == 20 * 5
                || sanctuaryTicks == 20) {
            int seconds = Math.max(0, sanctuaryTicks / 20);
            player.sendMessage(
                    Text.literal("Protected " + seconds + "s").formatted(Formatting.GOLD),
                    true
            );
        }
    }

    public static void protectionEndedNearDeathSite(ServerPlayerEntity player) {
        PenaltySounds.protectionEnded(player);
        chat(player, Text.empty()
                .append(Text.literal("Your protection has ended. ").formatted(Formatting.RED))
                .append(Text.literal("You are still near where you died.")
                        .formatted(Formatting.GRAY)));
    }

    /**
     * Respawned already inside the zone: chat only so Penalty / Protected titles stay.
     */
    public static void spawnedInDeathZone(ServerPlayerEntity player) {
        PenaltySounds.enteredDeathSite(player);
        chat(player, Text.empty()
                .append(Text.literal("You are in the death zone. ").formatted(Formatting.RED))
                .append(Text.literal("You are not ready to fight here yet. Leave this area or recover first.")
                        .formatted(Formatting.GRAY)));
    }

    public static void enteredDeathZone(ServerPlayerEntity player) {
        showTitle(
                player,
                Text.literal("Death zone").formatted(Formatting.DARK_RED),
                Text.literal("Not ready").formatted(Formatting.GRAY),
                8, 40, 12
        );
        PenaltySounds.enteredDeathSite(player);

        chat(player, Text.empty()
                .append(Text.literal("You returned to the death zone. ").formatted(Formatting.RED))
                .append(Text.literal("You are not ready to fight here yet. Leave this area or recover first.")
                        .formatted(Formatting.GRAY)));
    }

    public static void leftDeathZone(ServerPlayerEntity player) {
        chat(player, Text.empty()
                .append(Text.literal("You left the death zone. ").formatted(Formatting.GREEN))
                .append(Text.literal("You can recover more easily out here.")
                        .formatted(Formatting.GRAY)));
    }

    public static void deathSiteFaded(ServerPlayerEntity player) {
        PenaltySounds.deathSiteFaded(player);
        chat(player, Text.empty()
                .append(Text.literal("The death zone has faded. ").formatted(Formatting.YELLOW))
                .append(Text.literal("That area no longer weakens you extra, but your death penalty is still active until you sleep, wait a day, or drink a holy flask.")
                        .formatted(Formatting.GRAY)));
    }

    public static void penaltySteppedDown(ServerPlayerEntity player, int streak) {
        showTitle(
                player,
                Text.literal("Penalty").formatted(Formatting.DARK_RED),
                Text.literal("Lv. " + streak).formatted(Formatting.GREEN),
                8, 40, 10
        );
        PenaltySounds.steppedDown(player);

        chat(player, Text.empty()
                .append(Text.literal("Your death penalty eased by one step. ").formatted(Formatting.GREEN))
                .append(Text.literal("Sleep again, wait another day, or drink a holy flask to clear the rest.")
                        .formatted(Formatting.GRAY)));
    }

    public static void sleepLocked(ServerPlayerEntity player, int recoveryLockTicks) {
        PenaltySounds.blocked(player);
        chat(player, Text.empty()
                .append(Text.literal("You cannot sleep yet. ").formatted(Formatting.RED))
                .append(Text.literal("Wait " + formatTicks(recoveryLockTicks) + ".")
                        .formatted(Formatting.GRAY)));
    }

    public static void recovered(ServerPlayerEntity player) {
        showTitle(
                player,
                Text.literal("Recovered").formatted(Formatting.GREEN),
                Text.empty(),
                5, 30, 10
        );
        PenaltySounds.recovered(player);
        chat(player, Text.literal("Your death penalty has lifted.").formatted(Formatting.GREEN));
    }

    public static void flaskCleared(ServerPlayerEntity player) {
        showTitle(
                player,
                Text.literal("Recovered").formatted(Formatting.GREEN),
                Text.literal("You feel better now").formatted(Formatting.GRAY),
                5, 35, 10
        );
        PenaltySounds.flaskFinished(player);
    }

    public static void flaskHadNoEffect(ServerPlayerEntity player) {
        showTitle(
                player,
                Text.literal("No effect").formatted(Formatting.GRAY),
                Text.literal("Nothing to clear").formatted(Formatting.DARK_GRAY),
                5, 30, 8
        );
        PenaltySounds.flaskNoEffect(player);
        chat(player, Text.empty()
                .append(Text.literal("You are not under a death penalty. ").formatted(Formatting.GRAY))
                .append(Text.literal("The flask does nothing.")
                        .formatted(Formatting.DARK_GRAY)));
    }

    private static void chat(ServerPlayerEntity player, Text message) {
        player.sendMessage(
                Text.empty()
                        .append(Text.literal("[DP] ").formatted(Formatting.DARK_RED))
                        .append(message),
                false
        );
    }

    private static void showTitle(
            ServerPlayerEntity player,
            MutableText title,
            Text subtitle,
            int fadeIn,
            int stay,
            int fadeOut
    ) {
        player.networkHandler.sendPacket(new TitleFadeS2CPacket(fadeIn, stay, fadeOut));
        player.networkHandler.sendPacket(new TitleS2CPacket(title));
        player.networkHandler.sendPacket(new SubtitleS2CPacket(subtitle));
    }

    static String formatTicks(int ticks) {
        int totalSeconds = Math.max(0, ticks / 20);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        if (minutes > 0 && seconds > 0) {
            return minutes + " minutes and " + seconds + " seconds";
        }
        if (minutes > 0) {
            return minutes == 1 ? "1 minute" : minutes + " minutes";
        }
        return seconds == 1 ? "1 second" : seconds + " seconds";
    }
}
