package com.hipster.respawnpenalty.client;

import com.hipster.respawnpenalty.HipstersRespawnPenalty;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * Vanilla glint shaders and UV scroll, sampled from our red foil textures.
 * The glint fragment shader ignores vertex color, so the PNG is the only color lever.
 */
public final class FrayingGlintLayers {
    public static final Identifier ITEM_TEXTURE = Identifier.of(
            HipstersRespawnPenalty.MOD_ID,
            "textures/misc/enchanted_glint_item.png"
    );
    public static final Identifier ENTITY_TEXTURE = Identifier.of(
            HipstersRespawnPenalty.MOD_ID,
            "textures/misc/enchanted_glint_entity.png"
    );

    private static final RenderLayer GLINT = itemGlint(
            "hrp_glint",
            RenderPhase.GLINT_PROGRAM,
            RenderPhase.GLINT_TEXTURING,
            null
    );
    private static final RenderLayer GLINT_TRANSLUCENT = itemGlint(
            "hrp_glint_translucent",
            RenderPhase.TRANSLUCENT_GLINT_PROGRAM,
            RenderPhase.GLINT_TEXTURING,
            RenderPhase.ITEM_ENTITY_TARGET
    );
    private static final RenderLayer ENTITY_GLINT = entityGlint(
            "hrp_entity_glint",
            RenderPhase.ENTITY_GLINT_PROGRAM,
            RenderPhase.ITEM_ENTITY_TARGET,
            false
    );
    private static final RenderLayer DIRECT_ENTITY_GLINT = entityGlint(
            "hrp_entity_glint_direct",
            RenderPhase.DIRECT_ENTITY_GLINT_PROGRAM,
            null,
            false
    );
    private static final RenderLayer ARMOR_ENTITY_GLINT = entityGlint(
            "hrp_armor_entity_glint",
            RenderPhase.ARMOR_ENTITY_GLINT_PROGRAM,
            null,
            true
    );

    private FrayingGlintLayers() {
    }

    /**
     * Vanilla glint blend is dest * src. These PNGs are sparse dark red, so we
     * multiply ColorModulator while our layer draws.
     */
    public static final float COLOR_R = 3.1F;
    public static final float COLOR_G = 1.35F;
    public static final float COLOR_B = 1.35F;

    public static VertexConsumerProvider wrap(VertexConsumerProvider delegate) {
        return layer -> delegate.getBuffer(remap(layer));
    }

    public static RenderLayer companion(RenderLayer vanilla) {
        RenderLayer remapped = remap(vanilla);
        return remapped == vanilla ? null : remapped;
    }

    public static boolean isOurs(RenderLayer layer) {
        return layer == GLINT
                || layer == GLINT_TRANSLUCENT
                || layer == ENTITY_GLINT
                || layer == DIRECT_ENTITY_GLINT
                || layer == ARMOR_ENTITY_GLINT;
    }

    static RenderLayer remap(RenderLayer layer) {
        if (layer == RenderLayer.getGlint()) {
            return GLINT;
        }
        if (layer == RenderLayer.getGlintTranslucent()) {
            return GLINT_TRANSLUCENT;
        }
        if (layer == RenderLayer.getEntityGlint()) {
            return ENTITY_GLINT;
        }
        if (layer == RenderLayer.getDirectEntityGlint()) {
            return DIRECT_ENTITY_GLINT;
        }
        if (layer == RenderLayer.getArmorEntityGlint()) {
            return ARMOR_ENTITY_GLINT;
        }
        return layer;
    }

    private static RenderLayer itemGlint(
            String name,
            RenderPhase.ShaderProgram program,
            RenderPhase.Texturing texturing,
            @Nullable RenderPhase.Target target
    ) {
        return ofGlint(name, program, ITEM_TEXTURE, texturing, target, false);
    }

    private static RenderLayer entityGlint(
            String name,
            RenderPhase.ShaderProgram program,
            @Nullable RenderPhase.Target target,
            boolean viewOffset
    ) {
        return ofGlint(name, program, ENTITY_TEXTURE, RenderPhase.ENTITY_GLINT_TEXTURING, target, viewOffset);
    }

    private static RenderLayer ofGlint(
            String name,
            RenderPhase.ShaderProgram program,
            Identifier texture,
            RenderPhase.Texturing texturing,
            @Nullable RenderPhase.Target target,
            boolean viewOffset
    ) {
        RenderLayer.MultiPhaseParameters.Builder builder = RenderLayer.MultiPhaseParameters.builder()
                .program(program)
                .texture(new RenderPhase.Texture(texture, true, false))
                .writeMaskState(RenderPhase.COLOR_MASK)
                .cull(RenderPhase.DISABLE_CULLING)
                .depthTest(RenderPhase.EQUAL_DEPTH_TEST)
                .transparency(RenderPhase.GLINT_TRANSPARENCY)
                .texturing(texturing);
        if (target != null) {
            builder.target(target);
        }
        if (viewOffset) {
            builder.layering(RenderPhase.VIEW_OFFSET_Z_LAYERING);
        }
        return RenderLayer.of(
                name,
                VertexFormats.POSITION_TEXTURE,
                VertexFormat.DrawMode.QUADS,
                1536,
                builder.build(false)
        );
    }
}
