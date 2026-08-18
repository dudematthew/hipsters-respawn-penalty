package com.hipster.respawnpenalty.mixin;

import com.hipster.respawnpenalty.Fraying;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.function.Consumer;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    /**
     * Scale durability after Unbreaking (EnchantmentHelper.getItemDamage) has already run.
     */
    @ModifyVariable(
            method = "damage(ILnet/minecraft/server/world/ServerWorld;Lnet/minecraft/server/network/ServerPlayerEntity;Ljava/util/function/Consumer;)V",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Lnet/minecraft/enchantment/EnchantmentHelper;getItemDamage(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/item/ItemStack;I)I"
            ),
            argsOnly = true,
            ordinal = 0
    )
    private int hrp$frayingAfterUnbreaking(
            int amount,
            int originalAmount,
            ServerWorld world,
            @Nullable ServerPlayerEntity player,
            Consumer<Item> breakCallback
    ) {
        ItemStack stack = (ItemStack) (Object) this;
        return Fraying.scaleAfterUnbreaking(player, stack, amount, world.getRandom().nextDouble());
    }
}
