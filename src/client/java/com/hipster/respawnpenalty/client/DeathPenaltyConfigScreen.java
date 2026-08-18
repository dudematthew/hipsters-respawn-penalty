package com.hipster.respawnpenalty.client;

import com.hipster.respawnpenalty.DeathPenaltyConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public final class DeathPenaltyConfigScreen {
    private DeathPenaltyConfigScreen() {
    }

    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.translatable("text.hipsters_respawn_penalty.config.title"))
                .setSavingRunnable(DeathPenaltyConfig::save);

        ConfigEntryBuilder entry = builder.entryBuilder();
        DeathPenaltyConfig.FileData current = DeathPenaltyConfig.FileData.capture();
        DeathPenaltyConfig.FileData defaults = new DeathPenaltyConfig.FileData();

        ConfigCategory general = builder.getOrCreateCategory(
                Text.translatable("text.hipsters_respawn_penalty.config.category.general"));
        general.addEntry(entry.startBooleanToggle(
                        Text.translatable("text.hipsters_respawn_penalty.config.ignoreCreative"),
                        current.ignoreCreativeAndSpectator)
                .setDefaultValue(defaults.ignoreCreativeAndSpectator)
                .setTooltip(Text.translatable("text.hipsters_respawn_penalty.config.ignoreCreative.tooltip"))
                .setSaveConsumer(v -> DeathPenaltyConfig.IGNORE_CREATIVE_AND_SPECTATOR = v)
                .build());
        general.addEntry(entry.startIntField(
                        Text.translatable("text.hipsters_respawn_penalty.config.streakWindowSeconds"),
                        current.streakWindowSeconds)
                .setDefaultValue(defaults.streakWindowSeconds)
                .setMin(1)
                .setMax(3600)
                .setTooltip(Text.translatable("text.hipsters_respawn_penalty.config.streakWindowSeconds.tooltip"))
                .setSaveConsumer(v -> DeathPenaltyConfig.STREAK_WINDOW_TICKS = v * 20)
                .build());
        general.addEntry(entry.startIntField(
                        Text.translatable("text.hipsters_respawn_penalty.config.recoveryDays"),
                        current.recoveryMinecraftDays)
                .setDefaultValue(defaults.recoveryMinecraftDays)
                .setMin(1)
                .setMax(10)
                .setTooltip(Text.translatable("text.hipsters_respawn_penalty.config.recoveryDays.tooltip"))
                .setSaveConsumer(v -> DeathPenaltyConfig.RECOVERY_TICKS = v * 24000)
                .build());
        general.addEntry(entry.startIntField(
                        Text.translatable("text.hipsters_respawn_penalty.config.moderateLockSeconds"),
                        current.moderateRecoveryLockSeconds)
                .setDefaultValue(defaults.moderateRecoveryLockSeconds)
                .setMin(0)
                .setMax(3600)
                .setSaveConsumer(v -> DeathPenaltyConfig.MODERATE_RECOVERY_LOCK_TICKS = v * 20)
                .build());
        general.addEntry(entry.startIntField(
                        Text.translatable("text.hipsters_respawn_penalty.config.criticalLockSeconds"),
                        current.criticalRecoveryLockSeconds)
                .setDefaultValue(defaults.criticalRecoveryLockSeconds)
                .setMin(0)
                .setMax(3600)
                .setSaveConsumer(v -> DeathPenaltyConfig.CRITICAL_RECOVERY_LOCK_TICKS = v * 20)
                .build());

        ConfigCategory zone = builder.getOrCreateCategory(
                Text.translatable("text.hipsters_respawn_penalty.config.category.zone"));
        zone.addEntry(entry.startDoubleField(
                        Text.translatable("text.hipsters_respawn_penalty.config.zoneRadius"),
                        current.deathZoneRadius)
                .setDefaultValue(defaults.deathZoneRadius)
                .setMin(1.0)
                .setMax(256.0)
                .setTooltip(Text.translatable("text.hipsters_respawn_penalty.config.zoneRadius.tooltip"))
                .setSaveConsumer(v -> DeathPenaltyConfig.DEATH_ZONE_RADIUS = v)
                .build());
        zone.addEntry(entry.startDoubleField(
                        Text.translatable("text.hipsters_respawn_penalty.config.zoneYSlack"),
                        current.deathZoneYSlack)
                .setDefaultValue(defaults.deathZoneYSlack)
                .setMin(0.0)
                .setMax(128.0)
                .setSaveConsumer(v -> DeathPenaltyConfig.DEATH_ZONE_Y_SLACK = v)
                .build());
        zone.addEntry(entry.startIntField(
                        Text.translatable("text.hipsters_respawn_penalty.config.zoneLifetimeDays"),
                        current.deathZoneLifetimeMinecraftDays)
                .setDefaultValue(defaults.deathZoneLifetimeMinecraftDays)
                .setMin(1)
                .setMax(10)
                .setSaveConsumer(v -> DeathPenaltyConfig.DEATH_ZONE_LIFETIME_TICKS = v * 24000)
                .build());
        zone.addEntry(entry.startDoubleField(
                        Text.translatable("text.hipsters_respawn_penalty.config.zoneAttackFactor"),
                        current.zoneAttackDamageFactor)
                .setDefaultValue(defaults.zoneAttackDamageFactor)
                .setMin(0.05)
                .setMax(1.0)
                .setTooltip(Text.translatable("text.hipsters_respawn_penalty.config.zoneAttackFactor.tooltip"))
                .setSaveConsumer(v -> DeathPenaltyConfig.ZONE_ATTACK_DAMAGE_FACTOR = v)
                .build());
        zone.addEntry(entry.startIntField(
                        Text.translatable("text.hipsters_respawn_penalty.config.zoneReapplySeconds"),
                        current.zoneEffectReapplySeconds)
                .setDefaultValue(defaults.zoneEffectReapplySeconds)
                .setMin(1)
                .setMax(60)
                .setSaveConsumer(v -> DeathPenaltyConfig.ZONE_EFFECT_REAPPLY_INTERVAL = v * 20)
                .build());

        ConfigCategory sanctuary = builder.getOrCreateCategory(
                Text.translatable("text.hipsters_respawn_penalty.config.category.sanctuary"));
        sanctuary.addEntry(entry.startDoubleField(
                        Text.translatable("text.hipsters_respawn_penalty.config.sanctuaryRadius"),
                        current.sanctuaryRadius)
                .setDefaultValue(defaults.sanctuaryRadius)
                .setMin(0.0)
                .setMax(128.0)
                .setTooltip(Text.translatable("text.hipsters_respawn_penalty.config.sanctuaryRadius.tooltip"))
                .setSaveConsumer(v -> DeathPenaltyConfig.SANCTUARY_RADIUS = v)
                .build());
        sanctuary.addEntry(entry.startIntField(
                        Text.translatable("text.hipsters_respawn_penalty.config.sanctuarySeconds"),
                        current.sanctuarySeconds)
                .setDefaultValue(defaults.sanctuarySeconds)
                .setMin(1)
                .setMax(120)
                .setSaveConsumer(v -> DeathPenaltyConfig.SANCTUARY_TICKS = v * 20)
                .build());

        ConfigCategory fraying = builder.getOrCreateCategory(
                Text.translatable("text.hipsters_respawn_penalty.config.category.fraying"));
        fraying.addEntry(entry.startIntField(
                        Text.translatable("text.hipsters_respawn_penalty.config.frayingLingerSeconds"),
                        current.frayingLingerSeconds)
                .setDefaultValue(defaults.frayingLingerSeconds)
                .setMin(0)
                .setMax(3600)
                .setTooltip(Text.translatable("text.hipsters_respawn_penalty.config.frayingLingerSeconds.tooltip"))
                .setSaveConsumer(v -> DeathPenaltyConfig.FRAYING_LINGER_TICKS = v * 20)
                .build());
        fraying.addEntry(entry.startIntField(
                        Text.translatable("text.hipsters_respawn_penalty.config.frayingDurationSeconds"),
                        current.frayingEffectDurationSeconds)
                .setDefaultValue(defaults.frayingEffectDurationSeconds)
                .setMin(1)
                .setMax(300)
                .setSaveConsumer(v -> DeathPenaltyConfig.FRAYING_ACTIVE_DURATION_TICKS = v * 20)
                .build());
        fraying.addEntry(entry.startIntField(
                        Text.translatable("text.hipsters_respawn_penalty.config.frayingReapplySeconds"),
                        current.frayingReapplySeconds)
                .setDefaultValue(defaults.frayingReapplySeconds)
                .setMin(1)
                .setMax(60)
                .setSaveConsumer(v -> DeathPenaltyConfig.FRAYING_REAPPLY_INTERVAL = v * 20)
                .build());

        return builder.build();
    }
}
