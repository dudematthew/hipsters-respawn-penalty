package com.hipster.respawnpenalty.mixin.client;

import com.hipster.respawnpenalty.client.FrayingGlintLayers;
import com.hipster.respawnpenalty.client.FrayingTint;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArmorFeatureRenderer.class)
public abstract class ArmorFeatureRendererMixin {
    @Unique
    private static final ThreadLocal<LivingEntity> HRP$ARMOR_ENTITY = new ThreadLocal<>();
    @Unique
    private static final ThreadLocal<ItemStack> HRP$ARMOR_STACK = new ThreadLocal<>();

    @Inject(method = "renderArmor", at = @At("HEAD"))
    private void hrp$captureArmor(
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            LivingEntity entity,
            EquipmentSlot slot,
            int light,
            BipedEntityModel<?> model,
            CallbackInfo ci
    ) {
        HRP$ARMOR_ENTITY.set(entity);
        HRP$ARMOR_STACK.set(entity.getEquippedStack(slot));
    }

    @Inject(method = "renderArmor", at = @At("RETURN"))
    private void hrp$clearArmor(CallbackInfo ci) {
        HRP$ARMOR_ENTITY.remove();
        HRP$ARMOR_STACK.remove();
    }

    /**
     * 1.21.1 draws armor foil in {@code renderGlint} only when {@link ItemStack#hasGlint()} is true.
     * Fraying should still show red foil on unenchanted worn armor.
     */
    @WrapOperation(
            method = "renderArmor",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;hasGlint()Z")
    )
    private boolean hrp$forceFrayingGlint(ItemStack stack, Operation<Boolean> original) {
        LivingEntity entity = HRP$ARMOR_ENTITY.get();
        if (entity instanceof PlayerEntity player && FrayingTint.shouldTint(player, stack)) {
            return true;
        }
        return original.call(stack);
    }

    @WrapOperation(
            method = "renderGlint",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/VertexConsumerProvider;getBuffer(Lnet/minecraft/client/render/RenderLayer;)Lnet/minecraft/client/render/VertexConsumer;"
            )
    )
    private VertexConsumer hrp$redArmorGlint(
            VertexConsumerProvider provider,
            RenderLayer layer,
            Operation<VertexConsumer> original
    ) {
        LivingEntity entity = HRP$ARMOR_ENTITY.get();
        ItemStack stack = HRP$ARMOR_STACK.get();
        if (!(entity instanceof PlayerEntity player) || stack == null || !FrayingTint.shouldTint(player, stack)) {
            return original.call(provider, layer);
        }
        return original.call(FrayingGlintLayers.wrap(provider), layer);
    }
}
