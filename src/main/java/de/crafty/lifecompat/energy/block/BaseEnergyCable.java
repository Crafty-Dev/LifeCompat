package de.crafty.lifecompat.energy.block;

import de.crafty.lifecompat.api.energy.IEnergyProvider;
import de.crafty.lifecompat.api.energy.cable.AbstractEnergyCableBlockEntity;
import de.crafty.lifecompat.energy.block.BaseEnergyBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class BaseEnergyCable extends BaseEntityBlock {

    public static final EnumProperty<ConnectionState> NORTH = EnumProperty.create("north", ConnectionState.class);
    public static final EnumProperty<ConnectionState> EAST = EnumProperty.create("east", ConnectionState.class);
    public static final EnumProperty<ConnectionState> SOUTH = EnumProperty.create("south", ConnectionState.class);
    public static final EnumProperty<ConnectionState> WEST = EnumProperty.create("west", ConnectionState.class);
    public static final EnumProperty<ConnectionState> UP = EnumProperty.create("up", ConnectionState.class);
    public static final EnumProperty<ConnectionState> DOWN = EnumProperty.create("down", ConnectionState.class);

    public static final BooleanProperty ENERGY = BooleanProperty.create("energy");

    protected BaseEnergyCable(Properties properties) {
        super(properties);

        this.registerDefaultState(this.getStateDefinition().any()
                .setValue(NORTH, ConnectionState.NONE)
                .setValue(EAST, ConnectionState.NONE)
                .setValue(SOUTH, ConnectionState.NONE)
                .setValue(WEST, ConnectionState.NONE)
                .setValue(UP, ConnectionState.NONE)
                .setValue(DOWN, ConnectionState.NONE)
                .setValue(ENERGY, false)
        );
    }

    @Override
    protected @NotNull RenderShape getRenderShape(BlockState blockState) {
        return RenderShape.MODEL;
    }


    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN, ENERGY);
    }

    @Override
    protected @NotNull BlockState updateShape(BlockState blockState, Direction direction, BlockState neighborState, LevelAccessor levelAccessor, BlockPos blockPos, BlockPos neighborPos) {
        if (neighborState.getBlock() instanceof BaseEnergyCable)
            blockState = blockState.setValue(ENERGY, neighborState.getValue(ENERGY));

        if (direction == Direction.NORTH)
            return blockState.setValue(NORTH, BaseEnergyCable.getConnectionStateForNeighbor(levelAccessor, blockPos, direction));
        if (direction == Direction.EAST)
            return blockState.setValue(EAST, BaseEnergyCable.getConnectionStateForNeighbor(levelAccessor, blockPos, direction));
        if (direction == Direction.SOUTH)
            return blockState.setValue(SOUTH, BaseEnergyCable.getConnectionStateForNeighbor(levelAccessor, blockPos, direction));
        if (direction == Direction.WEST)
            return blockState.setValue(WEST, BaseEnergyCable.getConnectionStateForNeighbor(levelAccessor, blockPos, direction));
        if (direction == Direction.UP)
            return blockState.setValue(UP, BaseEnergyCable.getConnectionStateForNeighbor(levelAccessor, blockPos, direction));
        if (direction == Direction.DOWN)
            return blockState.setValue(DOWN, BaseEnergyCable.getConnectionStateForNeighbor(levelAccessor, blockPos, direction));

        return blockState;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext blockPlaceContext) {
        return this.defaultBlockState()
                .setValue(NORTH, BaseEnergyCable.getConnectionStateForNeighbor(blockPlaceContext.getLevel(), blockPlaceContext.getClickedPos(), Direction.NORTH))
                .setValue(EAST, BaseEnergyCable.getConnectionStateForNeighbor(blockPlaceContext.getLevel(), blockPlaceContext.getClickedPos(), Direction.EAST))
                .setValue(SOUTH, BaseEnergyCable.getConnectionStateForNeighbor(blockPlaceContext.getLevel(), blockPlaceContext.getClickedPos(), Direction.SOUTH))
                .setValue(WEST, BaseEnergyCable.getConnectionStateForNeighbor(blockPlaceContext.getLevel(), blockPlaceContext.getClickedPos(), Direction.WEST))
                .setValue(UP, BaseEnergyCable.getConnectionStateForNeighbor(blockPlaceContext.getLevel(), blockPlaceContext.getClickedPos(), Direction.UP))
                .setValue(DOWN, BaseEnergyCable.getConnectionStateForNeighbor(blockPlaceContext.getLevel(), blockPlaceContext.getClickedPos(), Direction.DOWN))
                .setValue(ENERGY, this.isNeighborCharged(blockPlaceContext.getLevel(), blockPlaceContext.getClickedPos()));

    }

    private boolean isNeighborCharged(LevelAccessor levelAccessor, BlockPos blockPos) {
        for (Direction direction : Direction.values()) {
            BlockState state = levelAccessor.getBlockState(blockPos.relative(direction));
            if (state.getBlock() instanceof BaseEnergyCable && state.getValue(ENERGY))
                return true;
        }
        return false;
    }

    public static ConnectionState getConnectionStateForNeighbor(LevelAccessor level, BlockPos pos, Direction direction) {
        if (level.getBlockState(pos.relative(direction)).getBlock() instanceof BaseEnergyCable)
            return ConnectionState.TRANSFER;

        BaseEnergyBlock.IOMode ioMode = BaseEnergyBlock.getIOMode(level.getBlockState(pos.relative(direction)), direction.getOpposite());
        return ioMode == BaseEnergyBlock.IOMode.NONE ? ConnectionState.NONE : ConnectionState.CONNECTED;
    }

    @Override
    protected void onPlace(BlockState blockState, Level level, BlockPos blockPos, BlockState oldState, boolean bl) {
        if (level.isClientSide())
            return;

        if (level.getBlockEntity(blockPos) instanceof AbstractEnergyCableBlockEntity cable) {
            boolean connectedToDevice = false;

            for (Direction direction : Direction.values()) {
                ConnectionState state = BaseEnergyCable.getConnectionStateForNeighbor(level, blockPos, direction);
                if (state == ConnectionState.CONNECTED && level.getBlockEntity(blockPos.relative(direction)) instanceof IEnergyProvider)
                    connectedToDevice = true;
            }

            cable.validateBuffer(connectedToDevice);

            if (oldState.is(blockState.getBlock()) && (oldState.getValue(ENERGY) == blockState.getValue(ENERGY)))
                cable.validateNetwork();
        }


        if (oldState.is(blockState.getBlock()))
            return;

        if (level.getBlockEntity(blockPos) instanceof AbstractEnergyCableBlockEntity cable)
            cable.onPlaced();

        super.onPlace(blockState, level, blockPos, oldState, bl);
    }

    @Override
    protected void onRemove(BlockState oldState, Level level, BlockPos blockPos, BlockState newState, boolean bl) {
        if (oldState.is(newState.getBlock()) || level.isClientSide()) {
            super.onRemove(oldState, level, blockPos, newState, bl);
            return;
        }

        if (level.getBlockEntity(blockPos) instanceof AbstractEnergyCableBlockEntity cable)
            cable.onDestroyed();

        super.onRemove(oldState, level, blockPos, newState, bl);

    }

    public enum ConnectionState implements StringRepresentable {
        NONE,
        TRANSFER,
        CONNECTED;

        @Override
        public @NotNull String getSerializedName() {
            return this.name().toLowerCase();
        }
    }
}
