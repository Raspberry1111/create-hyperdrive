package com.github.raspberry1111.create_hyperdrive.blocks.hyperdrive;

import com.github.raspberry1111.create_hyperdrive.AllBlocks;
import com.github.raspberry1111.create_hyperdrive.AllDataComponents;
import com.github.raspberry1111.create_hyperdrive.AllItems;
import com.github.raspberry1111.create_hyperdrive.CreateHyperdrive;
import com.github.raspberry1111.create_hyperdrive.mixin.ShulkerAccessor;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class HyperdriveBlockItem extends BlockItem {
    public HyperdriveBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    public static HyperdriveBlockItem withShulker(Block block, Properties properties) {
        return new HyperdriveBlockItem(
                block,
                properties.component(AllDataComponents.CURRENT_PROGRESS, 0)
                        .component(AllDataComponents.SHULKER_STATUS, HyperdriveStateMachine.ShulkerStatus.NORMAL)
                        .component(AllDataComponents.PHASE, HyperdriveStateMachine.Phase.CHARGING));
    }

    public static HyperdriveBlockItem empty(Properties properties) {
        return new HyperdriveBlockItem(AllBlocks.HYPERDRIVE.get(), properties);
    }

    public static ItemStack emptyStack() {
        return AllItems.EMPTY_HYPERDRIVE.asStack();
    }

    public static ItemStack filledStack() {
        ItemStack stack = AllBlocks.HYPERDRIVE.asItem().getDefaultInstance();
        stack.set(AllDataComponents.PHASE, HyperdriveStateMachine.Phase.CHARGING);
        stack.set(AllDataComponents.SHULKER_STATUS, HyperdriveStateMachine.ShulkerStatus.NORMAL);
        stack.set(AllDataComponents.CURRENT_PROGRESS, 0);

        return stack;
    }

    public static boolean hasShulker(ItemStack item) {
        return item.get(AllDataComponents.SHULKER_STATUS) != null;
    }

    public static float getShulkerProperty(ItemStack stack) {
        if (!stack.is(AllBlocks.HYPERDRIVE.asItem()))
            return 0.0f;

        HyperdriveStateMachine.Phase phase = stack.get(AllDataComponents.PHASE);
        HyperdriveStateMachine.ShulkerStatus shulkerStatus = stack.get(AllDataComponents.SHULKER_STATUS);

        if (phase == null || shulkerStatus == null)
            return 0.0f;

        if (phase == HyperdriveStateMachine.Phase.COOLDOWN)
            return 0.25f;
        if (shulkerStatus == HyperdriveStateMachine.ShulkerStatus.EXHAUSTED)
            return 0.50f;
        if (shulkerStatus == HyperdriveStateMachine.ShulkerStatus.NORMAL)
            return 0.75f;
        if (shulkerStatus == HyperdriveStateMachine.ShulkerStatus.INFUSED)
            return 1.0f;

        return 0.0f;
    }


    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        if (!hasShulker(stack))
            return super.useOn(context);

        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockEntity be = world.getBlockEntity(pos);
        Player player = context.getPlayer();

        if (!(be instanceof ShulkerBoxBlockEntity shulker))
            return super.useOn(context);

        if (shulker.hasAnyMatching((item) -> !item.isEmpty()))
            return super.useOn(context);

        if (world.isClientSide || player == null)
            return InteractionResult.SUCCESS;

        Direction direction = world.getBlockState(pos).getValue(ShulkerBoxBlock.FACING).getOpposite();

        Shulker shulkerEntity = EntityType.SHULKER.create(world);
        if (shulkerEntity == null)
            return super.useOn(context);

        world.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        world.addFreshEntity(shulkerEntity);
        shulkerEntity.moveTo(pos, 0.0f, 0.0f);
        ((ShulkerAccessor) shulkerEntity).invokeSetAttachFace(direction);
        giveHyperdriveItemTo(emptyStack(), player, context.getItemInHand(), context.getHand());


        return InteractionResult.SUCCESS;
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

        giveHyperdriveItemTo(filledStack(), player, heldItem, hand);

        Direction direction = ((Shulker) entity).getAttachFace().getOpposite();
        BlockState state = Blocks.SHULKER_BOX.defaultBlockState().setValue(ShulkerBoxBlock.FACING, direction);
        BlockPos pos = new BlockPos(entity.getBlockX(), entity.getBlockY(), entity.getBlockZ());
        if (world.getBlockState(pos).canBeReplaced()) {
            world.setBlock(pos, state, Block.UPDATE_ALL);
        }

        entity.discard();
        return InteractionResult.FAIL;
    }


    protected void giveHyperdriveItemTo(ItemStack stack, Player player, ItemStack heldItem, InteractionHand hand) {
        if (!player.isCreative())
            heldItem.shrink(1);
        if (heldItem.isEmpty()) {
            player.setItemInHand(hand, stack);
            return;
        }
        player.getInventory()
                .placeItemBackInInventory(stack);
    }

    private void spawnCaptureEffects(Level world, Vec3 vec) {
        if (world.isClientSide) {
            for (int i = 0; i < 40; i++) {
                Vec3 motion = VecHelper.offsetRandomly(Vec3.ZERO, world.random, .125f);
                world.addParticle(ParticleTypes.END_ROD, vec.x, vec.y, vec.z, motion.x * 2, motion.y, motion.z * 2);
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

    @Override
    public Component getName(ItemStack stack) {
        String base = "item." + CreateHyperdrive.MODID + ".hyperdrive";
        return Component.translatable(hasShulker(stack) ? base : base + "_empty");
    }
}