package com.hipster.respawnpenalty.mixin.client;

import com.hipster.respawnpenalty.client.FrayingGlintLayers;
import com.hipster.respawnpenalty.client.FrayingTint;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {
    @Unique
    private static final ThreadLocal<LivingEntity> HRP$RENDER_ENTITY = new ThreadLocal<>();

    @Inject(
            method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/world/World;III)V",
            at = @At("HEAD")
    )
    private void hrp$captureEntity(
            @Nullable LivingEntity entity,
            ItemStack stack,
            ModelTransformationMode renderMode,
            boolean leftHanded,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            @Nullable World world,
            int light,
            int overlay,
            int seed,
            CallbackInfo ci
    ) {
        HRP$RENDER_ENTITY.set(entity);
    }

    @Inject(
            method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/world/World;III)V",
            at = @At("RETURN")
    )
    private void hrp$clearEntity(CallbackInfo ci) {
        HRP$RENDER_ENTITY.remove();
    }

    @WrapOperation(
            method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/client/render/model/BakedModel;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/item/ItemRenderer;getItemGlintConsumer(Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/render/RenderLayer;ZZ)Lnet/minecraft/client/render/VertexConsumer;"
            )
    )
    private VertexConsumer hrp$redItemGlint(
            VertexConsumerProvider provider,
            RenderLayer layer,
            boolean solid,
            boolean glint,
            Operation<VertexConsumer> original,
            ItemStack stack,
            ModelTransformationMode renderMode
    ) {
        return hrp$applyRedGlint(provider, layer, solid, glint, original, stack, renderMode);
    }

    @WrapOperation(
            method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/client/render/model/BakedModel;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/item/ItemRenderer;getDirectItemGlintConsumer(Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/render/RenderLayer;ZZ)Lnet/minecraft/client/render/VertexConsumer;"
            )
    )
    private VertexConsumer hrp$redDirectGlint(
            VertexConsumerProvider provider,
            RenderLayer layer,
            boolean solid,
            boolean glint,
            Operation<VertexConsumer> original,
            ItemStack stack,
            ModelTransformationMode renderMode
    ) {
        return hrp$applyRedGlint(provider, layer, solid, glint, original, stack, renderMode);
    }

    @Unique
    private static VertexConsumer hrp$applyRedGlint(
            VertexConsumerProvider provider,
            RenderLayer layer,
            boolean solid,
            boolean glint,
            Operation<VertexConsumer> original,
            ItemStack stack,
            ModelTransformationMode renderMode
    ) {
        if (renderMode == ModelTransformationMode.GROUND
                || renderMode == ModelTransformationMode.FIXED
                || renderMode == ModelTransformationMode.NONE) {
            return original.call(provider, layer, solid, glint);
        }
        PlayerEntity player = hrp$player();
        if (!FrayingTint.shouldTint(player, stack)) {
            return original.call(provider, layer, solid, glint);
        }
        return original.call(FrayingGlintLayers.wrap(provider), layer, solid, true);
    }

    @Unique
    private static PlayerEntity hrp$player() {
        LivingEntity entity = HRP$RENDER_ENTITY.get();
        if (entity instanceof PlayerEntity player) {
            return player;
        }
        return MinecraftClient.getInstance().player;
    }
}
