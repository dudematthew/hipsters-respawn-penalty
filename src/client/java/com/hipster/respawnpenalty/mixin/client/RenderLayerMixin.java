package com.hipster.respawnpenalty.mixin.client;

import com.hipster.respawnpenalty.client.FrayingGlintLayers;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.RenderLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderLayer.class)
public abstract class RenderLayerMixin {
    @Inject(method = "draw", at = @At("HEAD"))
    private void hrp$boostFrayingGlint(BuiltBuffer buffer, CallbackInfo ci) {
        if (FrayingGlintLayers.isOurs((RenderLayer) (Object) this)) {
            RenderSystem.setShaderColor(
                    FrayingGlintLayers.COLOR_R,
                    FrayingGlintLayers.COLOR_G,
                    FrayingGlintLayers.COLOR_B,
                    1.0F
            );
        }
    }

    @Inject(method = "draw", at = @At("RETURN"))
    private void hrp$resetFrayingGlint(BuiltBuffer buffer, CallbackInfo ci) {
        if (FrayingGlintLayers.isOurs((RenderLayer) (Object) this)) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
