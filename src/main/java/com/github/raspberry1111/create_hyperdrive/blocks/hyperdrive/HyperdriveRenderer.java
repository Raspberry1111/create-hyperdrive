package com.github.raspberry1111.create_hyperdrive.blocks.hyperdrive;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class HyperdriveRenderer extends SafeBlockEntityRenderer<HyperdriveBlockEntity> {
    public HyperdriveRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void renderSafe(HyperdriveBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource bufferSource, int light, int overlay) {
    }


}
