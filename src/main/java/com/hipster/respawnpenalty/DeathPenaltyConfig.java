package com.hipster.respawnpenalty;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Live gameplay numbers. Defaults match current design; {@code config/hipsters_respawn_penalty.json} overrides them.
 */
public final class DeathPenaltyConfig {
    public static boolean IGNORE_CREATIVE_AND_SPECTATOR = true;

    public static int STREAK_WINDOW_TICKS = 20 * 60 * 10;
    /** One Minecraft day of survival steps the penalty down by one. */
    public static int RECOVERY_TICKS = 24000;
    public static int MODERATE_RECOVERY_LOCK_TICKS = 20 * 60 * 5;
    public static int CRITICAL_RECOVERY_LOCK_TICKS = 20 * 60 * 10;

    /** One Minecraft day — death zone lifetime. */
    public static int DEATH_ZONE_LIFETIME_TICKS = 24000;
    public static double DEATH_ZONE_RADIUS = 60.0D;
    public static double DEATH_ZONE_Y_SLACK = 24.0D;

    public static int SANCTUARY_TICKS = 20 * 12;
    /** Sanctuary if death is within this distance of the respawn position. */
    public static double SANCTUARY_RADIUS = 20.0D;

    public static int FRAYING_REAPPLY_INTERVAL = 20 * 8;
    public static int FRAYING_ACTIVE_DURATION_TICKS = 20 * 30;
    /** After sleep/day full recover only. Flask never leaves this behind. */
    public static int FRAYING_LINGER_TICKS = 20 * 60 * 3;

    public static int ZONE_EFFECT_REAPPLY_INTERVAL = 20 * 8;

    /** Multiply total attack damage while inside the death zone. */
    public static double ZONE_ATTACK_DAMAGE_FACTOR = 0.75D;

    public static final int SLEEP_COMPLETE_TIMER = 100;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private DeathPenaltyConfig() {
    }

    public static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(HipstersRespawnPenalty.MOD_ID + ".json");
    }

    public static void load() {
        Path path = configPath();
        try {
            if (Files.exists(path)) {
                try (Reader reader = Files.newBufferedReader(path)) {
                    FileData data = GSON.fromJson(reader, FileData.class);
                    if (data != null) {
                        data.apply();
                    }
                }
            } else {
                save();
            }
        } catch (IOException e) {
            HipstersRespawnPenalty.LOGGER.warn("Could not load config from {}; using defaults.", path, e);
        }
    }

    public static void save() {
        Path path = configPath();
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(FileData.capture(), writer);
            }
        } catch (IOException e) {
            HipstersRespawnPenalty.LOGGER.warn("Could not save config to {}.", path, e);
        }
    }

    public static int recoveryLockForStreak(int deathStreak) {
        if (deathStreak >= 6) {
            return CRITICAL_RECOVERY_LOCK_TICKS;
        }
        if (deathStreak >= 4) {
            return MODERATE_RECOVERY_LOCK_TICKS;
        }
        return 0;
    }

    public static int zoneEffectiveStreak(int deathStreak) {
        return Math.min(deathStreak + 1, 6);
    }

    /** Structural max-health / hunger only. */
    public static PenaltyTier tierForStreak(int deathStreak) {
        if (deathStreak <= 1) {
            return new PenaltyTier(15.0D, 9);
        }
        if (deathStreak == 2) {
            return new PenaltyTier(14.0D, 8);
        }
        if (deathStreak == 3) {
            return new PenaltyTier(12.0D, 7);
        }
        if (deathStreak <= 5) {
            return new PenaltyTier(10.0D, 6);
        }
        return new PenaltyTier(10.0D, 6);
    }

    /**
     * Stronger package inside the death zone. Call with zoneEffectiveStreak(deathStreak).
     */
    public static EffectPackage zoneEffects(int effectiveStreak) {
        if (effectiveStreak <= 2) {
            return new EffectPackage(20 * 35, 0, 0, 0, 0, 0, 0, 0);
        }
        if (effectiveStreak == 3) {
            return new EffectPackage(20 * 45, 0, 20 * 25, 0, 20 * 25, 0, 0, 0);
        }
        int slownessAmp = effectiveStreak >= 6 ? 1 : 0;
        int weaknessAmp = effectiveStreak >= 4 ? 1 : 0;
        return new EffectPackage(
                20 * 50, weaknessAmp,
                20 * 30, slownessAmp,
                20 * 30, 0,
                20 * 8, 0
        );
    }

    public record PenaltyTier(double maxHealth, int foodLevel) {
    }

    public record EffectPackage(
            int weaknessTicks,
            int weaknessAmplifier,
            int slownessTicks,
            int slownessAmplifier,
            int miningFatigueTicks,
            int miningFatigueAmplifier,
            int darknessTicks,
            int darknessAmplifier
    ) {
        public static final EffectPackage NONE = new EffectPackage(0, 0, 0, 0, 0, 0, 0, 0);

        public boolean hasMiningFatigue() {
            return miningFatigueTicks > 0;
        }
    }

    /** JSON shape — human units where possible. */
    public static final class FileData {
        public boolean ignoreCreativeAndSpectator = true;
        public int streakWindowSeconds = 600;
        public int recoveryMinecraftDays = 1;
        public int moderateRecoveryLockSeconds = 300;
        public int criticalRecoveryLockSeconds = 600;
        public int deathZoneLifetimeMinecraftDays = 1;
        public double deathZoneRadius = 60.0D;
        public double deathZoneYSlack = 24.0D;
        public int sanctuarySeconds = 12;
        public double sanctuaryRadius = 20.0D;
        public int frayingReapplySeconds = 8;
        public int frayingEffectDurationSeconds = 30;
        public int frayingLingerSeconds = 180;
        public int zoneEffectReapplySeconds = 8;
        public double zoneAttackDamageFactor = 0.75D;

        public static FileData capture() {
            FileData data = new FileData();
            data.ignoreCreativeAndSpectator = IGNORE_CREATIVE_AND_SPECTATOR;
            data.streakWindowSeconds = Math.max(1, STREAK_WINDOW_TICKS / 20);
            data.recoveryMinecraftDays = Math.max(1, RECOVERY_TICKS / 24000);
            data.moderateRecoveryLockSeconds = Math.max(0, MODERATE_RECOVERY_LOCK_TICKS / 20);
            data.criticalRecoveryLockSeconds = Math.max(0, CRITICAL_RECOVERY_LOCK_TICKS / 20);
            data.deathZoneLifetimeMinecraftDays = Math.max(1, DEATH_ZONE_LIFETIME_TICKS / 24000);
            data.deathZoneRadius = DEATH_ZONE_RADIUS;
            data.deathZoneYSlack = DEATH_ZONE_Y_SLACK;
            data.sanctuarySeconds = Math.max(1, SANCTUARY_TICKS / 20);
            data.sanctuaryRadius = SANCTUARY_RADIUS;
            data.frayingReapplySeconds = Math.max(1, FRAYING_REAPPLY_INTERVAL / 20);
            data.frayingEffectDurationSeconds = Math.max(1, FRAYING_ACTIVE_DURATION_TICKS / 20);
            data.frayingLingerSeconds = Math.max(0, FRAYING_LINGER_TICKS / 20);
            data.zoneEffectReapplySeconds = Math.max(1, ZONE_EFFECT_REAPPLY_INTERVAL / 20);
            data.zoneAttackDamageFactor = ZONE_ATTACK_DAMAGE_FACTOR;
            return data;
        }

        void apply() {
            IGNORE_CREATIVE_AND_SPECTATOR = ignoreCreativeAndSpectator;
            STREAK_WINDOW_TICKS = Math.max(1, streakWindowSeconds) * 20;
            RECOVERY_TICKS = Math.max(1, recoveryMinecraftDays) * 24000;
            MODERATE_RECOVERY_LOCK_TICKS = Math.max(0, moderateRecoveryLockSeconds) * 20;
            CRITICAL_RECOVERY_LOCK_TICKS = Math.max(0, criticalRecoveryLockSeconds) * 20;
            DEATH_ZONE_LIFETIME_TICKS = Math.max(1, deathZoneLifetimeMinecraftDays) * 24000;
            DEATH_ZONE_RADIUS = Math.max(1.0D, deathZoneRadius);
            DEATH_ZONE_Y_SLACK = Math.max(0.0D, deathZoneYSlack);
            SANCTUARY_TICKS = Math.max(1, sanctuarySeconds) * 20;
            SANCTUARY_RADIUS = Math.max(0.0D, sanctuaryRadius);
            FRAYING_REAPPLY_INTERVAL = Math.max(1, frayingReapplySeconds) * 20;
            FRAYING_ACTIVE_DURATION_TICKS = Math.max(1, frayingEffectDurationSeconds) * 20;
            FRAYING_LINGER_TICKS = Math.max(0, frayingLingerSeconds) * 20;
            ZONE_EFFECT_REAPPLY_INTERVAL = Math.max(1, zoneEffectReapplySeconds) * 20;
            ZONE_ATTACK_DAMAGE_FACTOR = Math.min(1.0D, Math.max(0.05D, zoneAttackDamageFactor));
        }
    }
}
