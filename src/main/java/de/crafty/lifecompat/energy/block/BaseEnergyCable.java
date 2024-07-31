package de.crafty.lifecompat.energy.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
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
        if (neighborState.getBlock() instanceof BaseEnergyCable && neighborState.getValue(ENERGY))
            blockState = blockState.setValue(ENERGY,true);

        if (direction == Direction.NORTH)
            return blockState.setValue(NORTH, this.getConnectionStateForNeighbor(levelAccessor, blockPos, direction));
        if (direction == Direction.EAST)
            return blockState.setValue(EAST, this.getConnectionStateForNeighbor(levelAccessor, blockPos, direction));
        if (direction == Direction.SOUTH)
            return blockState.setValue(SOUTH, this.getConnectionStateForNeighbor(levelAccessor, blockPos, direction));
        if (direction == Direction.WEST)
            return blockState.setValue(WEST, this.getConnectionStateForNeighbor(levelAccessor, blockPos, direction));
        if (direction == Direction.UP)
            return blockState.setValue(UP, this.getConnectionStateForNeighbor(levelAccessor, blockPos, direction));
        if (direction == Direction.DOWN)
            return blockState.setValue(DOWN, this.getConnectionStateForNeighbor(levelAccessor, blockPos, direction));

        return blockState;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext blockPlaceContext) {
        return this.defaultBlockState()
                .setValue(NORTH, this.getConnectionStateForNeighbor(blockPlaceContext.getLevel(), blockPlaceContext.getClickedPos(), Direction.NORTH))
                .setValue(EAST, this.getConnectionStateForNeighbor(blockPlaceContext.getLevel(), blockPlaceContext.getClickedPos(), Direction.EAST))
                .setValue(SOUTH, this.getConnectionStateForNeighbor(blockPlaceContext.getLevel(), blockPlaceContext.getClickedPos(), Direction.SOUTH))
                .setValue(WEST, this.getConnectionStateForNeighbor(blockPlaceContext.getLevel(), blockPlaceContext.getClickedPos(), Direction.WEST))
                .setValue(UP, this.getConnectionStateForNeighbor(blockPlaceContext.getLevel(), blockPlaceContext.getClickedPos(), Direction.UP))
                .setValue(DOWN, this.getConnectionStateForNeighbor(blockPlaceContext.getLevel(), blockPlaceContext.getClickedPos(), Direction.DOWN))
                .setValue(ENERGY, this.isNeighborCharged(blockPlaceContext.getLevel(), blockPlaceContext.getClickedPos()));

    }

    private boolean isNeighborCharged(LevelAccessor levelAccessor, BlockPos blockPos){
        for(Direction direction : Direction.values()){
            BlockState state = levelAccessor.getBlockState(blockPos.relative(direction));
            if(state.getBlock() instanceof BaseEnergyCable && state.getValue(ENERGY))
                return true;
        }
        return false;
    }

    private ConnectionState getConnectionStateForNeighbor(LevelAccessor level, BlockPos pos, Direction direction) {
        if (level.getBlockState(pos.relative(direction)).getBlock() instanceof BaseEnergyCable)
            return ConnectionState.TRANSFER;

        BaseEnergyBlock.IOMode ioMode = BaseEnergyBlock.getIOMode(level.getBlockState(pos.relative(direction)), direction.getOpposite());
        return ioMode == BaseEnergyBlock.IOMode.NONE ? ConnectionState.NONE : ConnectionState.CONNECTED;
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
