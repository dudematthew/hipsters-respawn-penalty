package com.hipster.respawnpenalty;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

/**
 * Harmful wear-accelerant. Durability math lives in {@link Fraying} + ItemStack mixin.
 */
public final class FrayingEffect extends StatusEffect {
    public FrayingEffect() {
        super(StatusEffectCategory.HARMFUL, 0x8B0000);
    }
}
