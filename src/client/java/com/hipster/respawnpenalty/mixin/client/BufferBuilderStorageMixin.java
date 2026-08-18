package com.hipster.respawnpenalty.mixin.client;

import com.hipster.respawnpenalty.client.FrayingGlintLayers;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.BufferAllocator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Vanilla glint layers get dedicated buffers and a fixed draw order. Custom layers
 * that skip this map are flushed too late for EQUAL depth, so item foil vanishes.
 */
@Mixin(BufferBuilderStorage.class)
public abstract class BufferBuilderStorageMixin {
    @WrapOperation(
            method = "method_54639",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/BufferBuilderStorage;assignBufferBuilder(Lit/unimi/dsi/fastutil/objects/Object2ObjectLinkedOpenHashMap;Lnet/minecraft/client/render/RenderLayer;)V"
            )
    )
    private void hrp$assignFrayingGlint(
            Object2ObjectLinkedOpenHashMap<RenderLayer, BufferAllocator> map,
            RenderLayer layer,
            Operation<Void> original
    ) {
        original.call(map, layer);
        RenderLayer fraying = FrayingGlintLayers.companion(layer);
        if (fraying != null) {
            original.call(map, fraying);
        }
    }
}
