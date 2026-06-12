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

    public HyperdriveBlock(final Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(HAS_SHULKER, false));
    }

    @Override
    public VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        final BlockEntity blockentity = level.getBlockEntity(pos);
        return blockentity instanceof HyperdriveBlockEntity ? Shapes.create(((HyperdriveBlockEntity) blockentity).getBoundingBox(state)) : Shapes.block();
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HAS_SHULKER);
        super.createBlockStateDefinition(builder);
    }


    @Override
    public @Nullable BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        if (!state.getValue(HAS_SHULKER))
            return null;
        return IBE.super.newBlockEntity(pos, state);
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        final ItemStack stack = context.getItemInHand();
        final Item item = stack.getItem();
        final BlockState defaultState = defaultBlockState();
        if (!(item instanceof HyperdriveBlockItem))
            return defaultState;

        return Objects.requireNonNull(super.getStateForPlacement(context))
                .setValue(HAS_SHULKER, HyperdriveBlockItem.hasShulker(stack));
    }


    @Override
    public Direction.Axis getRotationAxis(final BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public boolean hasShaftTowards(final LevelReader world, final BlockPos pos, final BlockState state, final Direction face) {
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

    protected ItemInteractionResult useItemOn(final ItemStack stack, final BlockState state, final Level level, final BlockPos pos, final Player player, final InteractionHand hand, final BlockHitResult hitResult) {
        if (!state.getValue(HAS_SHULKER)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        final BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof final HyperdriveBlockEntity hyperdrive))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        if (level.isClientSide())
            return ItemInteractionResult.SUCCESS;

        if (stack.getItem() == Items.DRAGON_BREATH && stack.getCount() >= 1) {
            if (hyperdrive.infuse()) {
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
    public ItemStack getCloneItemStack(final BlockState state, final HitResult target, final LevelReader level, final BlockPos pos, final Player player) {
        final BlockEntity be = level.getBlockEntity(pos);

        if (be instanceof final HyperdriveBlockEntity hyperdrive) {
            return hyperdrive.getItemStackWithData();
        } else {
            return AllBlocks.HYPERDRIVE.asStack();
        }
    }

    @Override
    public List<ItemStack> getDrops(final BlockState state, final LootParams.Builder builder) {
        final List<ItemStack> drops = new ArrayList<>();

        final BlockEntity be = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);

        final ItemStack stack;
        if (be instanceof final HyperdriveBlockEntity hyperdrive) {
            stack = hyperdrive.getItemStackWithData();
        } else {
            stack = HyperdriveBlockItem.emptyStack();
        }

        drops.add(stack);
        return drops;
    }

    @Override
    public boolean isSignalSource(final BlockState state) {
        return true;
    }

    @Override
    public int getSignal(final BlockState state, final BlockGetter level, final BlockPos pos, final Direction direction) {
        final BlockEntity be = level.getBlockEntity(pos);

        if (be instanceof final HyperdriveBlockEntity hyperdrive && !(hyperdrive.getPhase() instanceof
                HyperdriveBlockEntity.HyperdriveStateMachine.Phase.Active)) { // we check for active to make sure the redstone updates BEFORE we teleport. Sable won't automatically update neighbors after the tp
            return hyperdrive.redstonePower();
        } else {
            return 0;
        }
    }
}