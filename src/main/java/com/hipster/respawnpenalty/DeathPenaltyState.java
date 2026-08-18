package com.hipster.respawnpenalty;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;

public final class DeathPenaltyState {
    public int deathStreak;
    public boolean penaltyActive;
    public int aliveTicks;
    public int recoveryLockTicks;
    public long lastDeathGameTime;
    public double originalMaxHealth = 20.0D;

    public String deathDimension = "";
    public int deathX;
    public int deathY;
    public int deathZ;
    public long zoneExpireGameTime;
    public boolean wasInDeathZone;
    public int sanctuaryTicks;
    public boolean sleepSucceeded;
    public int effectReapplyCooldown;
    public int frayingReapplyCooldown;

    public static DeathPenaltyState fromNbt(NbtCompound tag) {
        DeathPenaltyState state = new DeathPenaltyState();
        state.deathStreak = tag.getInt("DeathStreak");
        state.penaltyActive = tag.getBoolean("PenaltyActive");
        state.aliveTicks = tag.getInt("AliveTicks");
        state.recoveryLockTicks = tag.getInt("RecoveryLockTicks");
        state.lastDeathGameTime = tag.getLong("LastDeathGameTime");
        state.originalMaxHealth = tag.contains("OriginalMaxHealth")
                ? tag.getDouble("OriginalMaxHealth")
                : 20.0D;

        state.deathDimension = tag.contains("DeathDimension") ? tag.getString("DeathDimension") : "";
        state.deathX = tag.getInt("DeathX");
        state.deathY = tag.getInt("DeathY");
        state.deathZ = tag.getInt("DeathZ");
        state.zoneExpireGameTime = tag.getLong("ZoneExpireGameTime");
        state.wasInDeathZone = tag.getBoolean("WasInDeathZone");
        state.sanctuaryTicks = tag.getInt("SanctuaryTicks");
        state.sleepSucceeded = tag.getBoolean("SleepSucceeded");
        state.effectReapplyCooldown = tag.getInt("EffectReapplyCooldown");
        state.frayingReapplyCooldown = tag.getInt("FrayingReapplyCooldown");
        return state;
    }

    public NbtCompound toNbt() {
        NbtCompound tag = new NbtCompound();
        tag.putInt("DeathStreak", deathStreak);
        tag.putBoolean("PenaltyActive", penaltyActive);
        tag.putInt("AliveTicks", aliveTicks);
        tag.putInt("RecoveryLockTicks", recoveryLockTicks);
        tag.putLong("LastDeathGameTime", lastDeathGameTime);
        tag.putDouble("OriginalMaxHealth", originalMaxHealth);

        tag.putString("DeathDimension", deathDimension == null ? "" : deathDimension);
        tag.putInt("DeathX", deathX);
        tag.putInt("DeathY", deathY);
        tag.putInt("DeathZ", deathZ);
        tag.putLong("ZoneExpireGameTime", zoneExpireGameTime);
        tag.putBoolean("WasInDeathZone", wasInDeathZone);
        tag.putInt("SanctuaryTicks", sanctuaryTicks);
        tag.putBoolean("SleepSucceeded", sleepSucceeded);
        tag.putInt("EffectReapplyCooldown", effectReapplyCooldown);
        tag.putInt("FrayingReapplyCooldown", frayingReapplyCooldown);
        return tag;
    }

    public BlockPos deathBlockPos() {
        return new BlockPos(deathX, deathY, deathZ);
    }

    public void setDeathLocation(String dimensionId, BlockPos pos, long expireGameTime) {
        this.deathDimension = dimensionId;
        this.deathX = pos.getX();
        this.deathY = pos.getY();
        this.deathZ = pos.getZ();
        this.zoneExpireGameTime = expireGameTime;
        this.wasInDeathZone = false;
    }

    public void clearDeathZone() {
        deathDimension = "";
        deathX = 0;
        deathY = 0;
        deathZ = 0;
        zoneExpireGameTime = 0L;
        wasInDeathZone = false;
    }

    public boolean hasDeathZoneData() {
        return deathDimension != null && !deathDimension.isEmpty() && zoneExpireGameTime > 0L;
    }
}
