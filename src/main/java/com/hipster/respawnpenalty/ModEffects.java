package com.hipster.respawnpenalty;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

public final class ModEffects {
    public static final RegistryEntry<StatusEffect> FRAYING = Registry.registerReference(
            Registries.STATUS_EFFECT,
            Identifier.of(HipstersRespawnPenalty.MOD_ID, "fraying"),
            new FrayingEffect()
    );

    private ModEffects() {
    }

    public static void register() {
        // Static field registration runs on class load.
    }
}
