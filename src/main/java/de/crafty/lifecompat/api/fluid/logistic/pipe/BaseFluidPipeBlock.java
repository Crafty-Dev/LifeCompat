package de.crafty.lifecompat.api.fluid.logistic.pipe;

import de.crafty.lifecompat.api.fluid.logistic.container.IFluidContainer;
import de.crafty.lifecompat.api.fluid.logistic.container.IFluidContainerBlock;
import de.crafty.lifecompat.energy.block.BaseEnergyCable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseFluidPipeBlock extends BaseEntityBlock {

    public static final EnumProperty<ConnectionState> NORTH = EnumProperty.create("north", ConnectionState.class);
    public static final EnumProperty<ConnectionState> EAST = EnumProperty.create("east", ConnectionState.class);
    public static final EnumProperty<ConnectionState> SOUTH = EnumProperty.create("south", ConnectionState.class);
    public static final EnumProperty<ConnectionState> WEST = EnumProperty.create("west", ConnectionState.class);

    public static final EnumProperty<ConnectionState> UP = EnumProperty.create("up", ConnectionState.class);
    public static final EnumProperty<ConnectionState> DOWN = EnumProperty.create("down", ConnectionState.class);

    public BaseFluidPipeBlock(Properties properties) {
        super(properties);

        this.registerDefaultState(this.getStateDefinition().any()
                .setValue(NORTH, ConnectionState.NONE)
                .setValue(EAST, ConnectionState.NONE)
                .setValue(SOUTH, ConnectionState.NONE)
                .setValue(WEST, ConnectionState.NONE)
                .setValue(UP, ConnectionState.NONE)
                .setValue(DOWN, ConnectionState.NONE)
        );
    }

    @Override
    protected @NotNull RenderShape getRenderShape(BlockState blockState) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }


    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext blockPlaceContext) {
        Level level = blockPlaceContext.getLevel();
        BlockPos blockPos = blockPlaceContext.getClickedPos();

        return this.defaultBlockState()
                .setValue(NORTH, BaseFluidPipeBlock.getConnectionStateForNeighbor(level, blockPos, Direction.NORTH))
                .setValue(EAST, BaseFluidPipeBlock.getConnectionStateForNeighbor(level, blockPos, Direction.EAST))
                .setValue(SOUTH, BaseFluidPipeBlock.getConnectionStateForNeighbor(level, blockPos, Direction.SOUTH))
                .setValue(WEST, BaseFluidPipeBlock.getConnectionStateForNeighbor(level, blockPos, Direction.WEST))
                .setValue(UP, BaseFluidPipeBlock.getConnectionStateForNeighbor(level, blockPos, Direction.UP))
                .setValue(DOWN, BaseFluidPipeBlock.getConnectionStateForNeighbor(level, blockPos, Direction.DOWN));
    }

    @Override
    protected @NotNull BlockState updateShape(BlockState blockState, Direction direction, BlockState neighborState, LevelAccessor levelAccessor, BlockPos blockPos, BlockPos neighborPos) {
        if (direction == Direction.NORTH)
            return blockState.setValue(NORTH, BaseFluidPipeBlock.getConnectionStateForNeighbor(levelAccessor, blockPos, direction));
        if (direction == Direction.EAST)
            return blockState.setValue(EAST, BaseFluidPipeBlock.getConnectionStateForNeighbor(levelAccessor, blockPos, direction));
        if (direction == Direction.SOUTH)
            return blockState.setValue(SOUTH, BaseFluidPipeBlock.getConnectionStateForNeighbor(levelAccessor, blockPos, direction));
        if (direction == Direction.WEST)
            return blockState.setValue(WEST, BaseFluidPipeBlock.getConnectionStateForNeighbor(levelAccessor, blockPos, direction));
        if (direction == Direction.UP)
            return blockState.setValue(UP, BaseFluidPipeBlock.getConnectionStateForNeighbor(levelAccessor, blockPos, direction));
        if (direction == Direction.DOWN)
            return blockState.setValue(DOWN, BaseFluidPipeBlock.getConnectionStateForNeighbor(levelAccessor, blockPos, direction));

        return blockState;
    }

    private static boolean wouldHaveLiquidConflict(Level level, BlockPos pos, BlockState state) {
        List<Fluid> fluidsPresent = new ArrayList<>();
        //TODO Check what happens when containers with different fluids exist
        for (Direction direction : Direction.values()) {
            if (level.getBlockEntity(pos.relative(direction)) instanceof AbstractFluidPipeBlockEntity fluidPipe) {
                if (!fluidsPresent.contains(fluidPipe.getBufferedFluid()) && fluidPipe.getBufferedFluid() != Fluids.EMPTY)
                    fluidsPresent.add(fluidPipe.getBufferedFluid());
            }
        }

        return fluidsPresent.size() > 1;
    }

    public static ConnectionState getConnectionStateForNeighbor(LevelAccessor level, BlockPos pos, Direction side) {
        if (level.getBlockState(pos.relative(side)).getBlock() instanceof IFluidContainerBlock fluidContainerBlock && fluidContainerBlock.canConnectPipe(level.getBlockState(pos.relative(side)), side.getOpposite()))
            return ConnectionState.ATTACHED;

        if (level.getBlockEntity(pos.relative(side)) instanceof AbstractFluidPipeBlockEntity)
            return ConnectionState.TRANSFER;

        return ConnectionState.NONE;
    }

    @Override
    protected void onPlace(BlockState blockState, Level level, BlockPos blockPos, BlockState oldState, boolean bl) {
        if (level.isClientSide()) return;

        if (blockState.is(oldState.getBlock()) && level.getBlockEntity(blockPos) instanceof AbstractFluidPipeBlockEntity pipe) {
            pipe.validateNetwork();
        }

        if (!blockState.is(oldState.getBlock()) && level.getBlockEntity(blockPos) instanceof AbstractFluidPipeBlockEntity pipe) {
            if (wouldHaveLiquidConflict(level, blockPos, blockState))
                level.destroyBlock(blockPos, true);
            else
                pipe.onPlace();
        }

    }


    @Override
    protected void onRemove(BlockState blockState, Level level, BlockPos blockPos, BlockState newState, boolean bl) {
        if (blockState.is(newState.getBlock()) || level.isClientSide()) {
            super.onRemove(blockState, level, blockPos, newState, bl);
            return;
        }


        if (level.getBlockEntity(blockPos) instanceof AbstractFluidPipeBlockEntity pipe)
            pipe.onDestroyed();

        super.onRemove(blockState, level, blockPos, newState, bl);
    }


    public enum ConnectionState implements StringRepresentable {
        NONE,
        TRANSFER,
        ATTACHED;

        @Override
        public @NotNull String getSerializedName() {
            return this.name().toLowerCase();
        }
    }
}
