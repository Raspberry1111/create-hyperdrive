package com.github.raspberry1111.create_hyperdrive.blocks.hyperdrive;

import com.github.raspberry1111.create_hyperdrive.AllBlocks;
import com.github.raspberry1111.create_hyperdrive.AllDataComponents;
import com.github.raspberry1111.create_hyperdrive.CreateHyperdrive;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HyperdriveBlockItem extends BlockItem {

    public HyperdriveBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    public static boolean hasShulker(ItemStack item) {
        return item.get(AllDataComponents.SHULKER_STATUS) != null;
    }

    @Override
    public @NotNull InteractionResult interactLivingEntity(@NotNull ItemStack heldItem, @NotNull Player player, @NotNull LivingEntity entity,
                                                           @NotNull InteractionHand hand) {
        if (hasShulker(heldItem))
            return InteractionResult.PASS;
        if (entity.getType() != EntityType.SHULKER)
            return InteractionResult.PASS;

        Level world = player.level();
        spawnCaptureEffects(world, entity.position());
        if (world.isClientSide)
            return InteractionResult.FAIL;

        giveHyperdriveItemTo(player, heldItem, hand);
        entity.discard();
        return InteractionResult.FAIL;
    }


    protected void giveHyperdriveItemTo(Player player, ItemStack heldItem, InteractionHand hand) {
        ItemStack filled = AllBlocks.HYPERDRIVE.asStack();
        filled.set(AllDataComponents.PHASE, HyperdriveStateMachine.Phase.CHARGING);
        filled.set(AllDataComponents.SHULKER_STATUS, HyperdriveStateMachine.ShulkerStatus.NORMAL);
        filled.set(AllDataComponents.CURRENT_PROGRESS, 0);

        if (!player.isCreative())
            heldItem.shrink(1);
        if (heldItem.isEmpty()) {
            player.setItemInHand(hand, filled);
            return;
        }
        player.getInventory()
                .placeItemBackInInventory(filled);
    }

    private void spawnCaptureEffects(Level world, Vec3 vec) {
        if (world.isClientSide) {
            for (int i = 0; i < 40; i++) {
                Vec3 motion = VecHelper.offsetRandomly(Vec3.ZERO, world.random, .125f);
                world.addParticle(ParticleTypes.ENCHANT, vec.x, vec.y, vec.z, motion.x, motion.y, motion.z);
                Vec3 circle = motion.multiply(1, 0, 1)
                        .normalize()
                        .scale(.5f);
                world.addParticle(ParticleTypes.END_ROD, circle.x, vec.y, circle.z, 0, -0.125, 0);
            }
            return;
        }

        BlockPos soundPos = BlockPos.containing(vec);
//        world.playSound(null, soundPos, SoundEvents.BLAZE_HURT, SoundSource.HOSTILE, .25f, .75f);
//        world.playSound(null, soundPos, SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, .5f, .75f);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        ItemStack stack = context.getItemInHand();

        HyperdriveStateMachine.Phase phase = stack.getOrDefault(AllDataComponents.PHASE, HyperdriveStateMachine.Phase.CHARGING);
        HyperdriveStateMachine.ShulkerStatus shulkerStatus = stack.getOrDefault(AllDataComponents.SHULKER_STATUS, HyperdriveStateMachine.ShulkerStatus.NORMAL);
        int currentProgress = stack.getOrDefault(AllDataComponents.CURRENT_PROGRESS, 0);

        InteractionResult result = super.place(context);

        if (result == InteractionResult.FAIL) {
            return result;
        }

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        if (level.getBlockEntity(pos) instanceof HyperdriveBlockEntity be) {
            // we should never actually use the defaults from getOrDefault here because if the block entity exists then
            // there was a shulker_status component on the item stack (see HyperdriveBlock.newBlockEntity)
            be.stateMachine.phase = phase;
            be.stateMachine.shulkerStatus = shulkerStatus; // default is NORMAL because if the block entity was placed, it cant be empty
            be.stateMachine.currentProgress = currentProgress;
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}