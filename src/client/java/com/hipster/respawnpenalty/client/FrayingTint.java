package com.hipster.respawnpenalty.client;

import com.hipster.respawnpenalty.ModEffects;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Equipment;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Red glint for Fraying: worn body gear, and tools/weapons actually in hand.
 * Armor sitting in the hotbar or held to move it does not glint.
 */
public final class FrayingTint {
    private FrayingTint() {
    }

    public static boolean shouldTint(@Nullable PlayerEntity player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) {
            return false;
        }
        if (!stack.isDamageable()) {
            return false;
        }
        if (!player.hasStatusEffect(ModEffects.FRAYING)) {
            return false;
        }
        if (isBodyWear(stack)) {
            Equipment equipment = Equipment.fromStack(stack);
            return equipment != null && player.getEquippedStack(equipment.getSlotType()) == stack;
        }
        return player.getEquippedStack(EquipmentSlot.MAINHAND) == stack
                || player.getEquippedStack(EquipmentSlot.OFFHAND) == stack;
    }

    static boolean isBodyWear(ItemStack stack) {
        Equipment equipment = Equipment.fromStack(stack);
        if (equipment == null) {
            return false;
        }
        EquipmentSlot slot = equipment.getSlotType();
        return slot == EquipmentSlot.HEAD
                || slot == EquipmentSlot.CHEST
                || slot == EquipmentSlot.LEGS
                || slot == EquipmentSlot.FEET;
    }
}
