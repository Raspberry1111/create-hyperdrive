package com.github.raspberry1111.create_hyperdrive.blocks.hyperdrive;

import com.github.raspberry1111.create_hyperdrive.AllBlockEntityTypes;
import com.github.raspberry1111.create_hyperdrive.AllBlocks;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class HyperdriveBlock extends DirectionalKineticBlock implements IBE<HyperdriveBlockEntity>, IWrenchable {
    public static final BooleanProperty HAS_SHULKER = BooleanProperty.create("shulker");

    public HyperdriveBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(HAS_SHULKER, false));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        BlockEntity blockentity = level.getBlockEntity(pos);
        return blockentity instanceof HyperdriveBlockEntity ? Shapes.create(((HyperdriveBlockEntity) blockentity).getBoundingBox(state)) : Shapes.block();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HAS_SHULKER);
        super.createBlockStateDefinition(builder);
    }


    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (!state.getValue(HAS_SHULKER))
            return null;
        return IBE.super.newBlockEntity(pos, state);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        ItemStack stack = context.getItemInHand();
        Item item = stack.getItem();
        BlockState defaultState = defaultBlockState();
        if (!(item instanceof HyperdriveBlockItem))
            return defaultState;

        return Objects.requireNonNull(super.getStateForPlacement(context))
                .setValue(HAS_SHULKER, HyperdriveBlockItem.hasShulker(stack));
    }


    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(FACING)
                .getOpposite();
    }

    @Override
    public Class<HyperdriveBlockEntity> getBlockEntityClass() {
        return HyperdriveBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends HyperdriveBlockEntity> getBlockEntityType() {
        return AllBlockEntityTypes.HYPERDRIVE.get();
    }

    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!state.getValue(HAS_SHULKER)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof HyperdriveBlockEntity hyperdrive))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        if (stack.getItem() == Items.DRAGON_BREATH && stack.getCount() >= 1) {
            if (hyperdrive.stateMachine.infuse()) {
                if (!player.isCreative())
                    stack.shrink(1);

                level.sendBlockUpdated(pos, state, state, UPDATE_ALL); // Notify clients of the block update
                return ItemInteractionResult.SUCCESS;
            } else {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }


    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        BlockEntity be = level.getBlockEntity(pos);

        if (be instanceof HyperdriveBlockEntity hyperdrive) {
            return hyperdrive.getItemStackWithData();
        } else {
            return AllBlocks.HYPERDRIVE.asStack();
        }
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        List<ItemStack> drops = new ArrayList<>();

        BlockEntity be = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);

        ItemStack stack;
        if (be instanceof HyperdriveBlockEntity hyperdrive) {
            stack = hyperdrive.getItemStackWithData();
        } else {
            stack = HyperdriveBlockItem.emptyStack();
        }

        drops.add(stack);
        return drops;
    }
}