package com.github.raspberry1111.create_hyperdrive;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.createmod.catnip.gui.element.DelegatedStencilElement;
import net.createmod.catnip.theme.Color;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AllIcons extends com.simibubi.create.foundation.gui.AllIcons {

    public static final ResourceLocation ICON_ATLAS = CreateHyperdrive.asResource("textures/gui/icons.png");
    public static final int ICON_ATLAS_SIZE = 64;

    private static int x = 0, y = -1;


    public static final AllIcons
            I_OVERWORLD_ISLANDS = newRow();
    public static final AllIcons
            I_NETHER_PORTAL = newRow(),
            I_NETHER_FORTRESS = next(),
            I_NETHER_ISLANDS = next();
    public static final AllIcons
            I_END_PEARL = newRow(),
            I_END_GATEWAY = next(),
            I_END_SHIP = next(),
            I_END_EYE = next();


    private final int iconX;
    private final int iconY;

    public AllIcons(final int x, final int y) {
        super(x, y);
        iconX = x * 16;
        iconY = y * 16;
    }

    private static AllIcons next() {
        return new AllIcons(++x, y);
    }

    private static AllIcons newRow() {
        return new AllIcons(x = 0, ++y);
    }

    @OnlyIn(Dist.CLIENT)
    public void bind() {
        RenderSystem.setShaderTexture(0, ICON_ATLAS);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void render(final GuiGraphics graphics, final int x, final int y) {
        graphics.blit(ICON_ATLAS, x, y, 0, iconX, iconY, 16, 16, ICON_ATLAS_SIZE, ICON_ATLAS_SIZE);
    }

    @OnlyIn(Dist.CLIENT)
    public void render(final PoseStack ms, final MultiBufferSource buffer, final int color) {
        final VertexConsumer builder = buffer.getBuffer(RenderType.text(ICON_ATLAS));
        final Matrix4f matrix = ms.last().pose();
        final Color rgb = new Color(color);
        final int light = LightTexture.FULL_BRIGHT;

        final Vec3 vec1 = new Vec3(0, 0, 0);
        final Vec3 vec2 = new Vec3(0, 1, 0);
        final Vec3 vec3 = new Vec3(1, 1, 0);
        final Vec3 vec4 = new Vec3(1, 0, 0);

        final float u1 = iconX * 1f / ICON_ATLAS_SIZE;
        final float u2 = (iconX + 16) * 1f / ICON_ATLAS_SIZE;
        final float v1 = iconY * 1f / ICON_ATLAS_SIZE;
        final float v2 = (iconY + 16) * 1f / ICON_ATLAS_SIZE;

        vertex(builder, matrix, vec1, rgb, u1, v1, light);
        vertex(builder, matrix, vec2, rgb, u1, v2, light);
        vertex(builder, matrix, vec3, rgb, u2, v2, light);
        vertex(builder, matrix, vec4, rgb, u2, v1, light);
    }

    @OnlyIn(Dist.CLIENT)
    private void vertex(final VertexConsumer builder, final Matrix4f matrix, final Vec3 vec, final Color rgb, final float u, final float v, final int light) {
        builder.addVertex(matrix, (float) vec.x, (float) vec.y, (float) vec.z)
                .setColor(rgb.getRed(), rgb.getGreen(), rgb.getBlue(), 255)
                .setUv(u, v)
                .setLight(light);
    }

    @OnlyIn(Dist.CLIENT)
    public DelegatedStencilElement asStencil() {
        return new DelegatedStencilElement().withStencilRenderer((ms, w, h, alpha) -> this.render(ms, 0, 0)).withBounds(16, 16);
    }
}
