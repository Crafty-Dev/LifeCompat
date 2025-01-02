package de.crafty.lifecompat.api.fluid.logistic.container;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface IFluidContainerBlock {

    EnumProperty<Direction> HORIZONTAL_FACING = BlockStateProperties.HORIZONTAL_FACING;
    EnumProperty<Direction> FFACING = BlockStateProperties.FACING;

    Map<Direction, List<Direction>> RELATIVE_DIRECTIONS = createRelativeDirections();


    /*
     * A map that contains all relative Directions for all possible facings of a block
     *
     * To make it more clear, you could assume that the mapped lists tell where the directions: (front, right, back, left, top, bottom) can be found ingame
     * based on the rotation (facing) of the block
     * By default (facing = North) the mapping is: north = front, east = right, south = back, west = left, up = top, down = bottom
     *
     * Default list: Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP, Direction.DOWN
     * E.g.: for Direction.EAST, the Map would return: Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.UP, Direction.DOWN
     */
    static Map<Direction, List<Direction>> createRelativeDirections() {
        Map<Direction, List<Direction>> relativeSides = new HashMap<>();

        relativeSides.put(Direction.NORTH, List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP, Direction.DOWN));
        relativeSides.put(Direction.EAST, List.of(Direction.EAST, Direction.NORTH, Direction.WEST, Direction.SOUTH, Direction.UP, Direction.DOWN));
        relativeSides.put(Direction.SOUTH, List.of(Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST, Direction.UP, Direction.DOWN));
        relativeSides.put(Direction.WEST, List.of(Direction.WEST, Direction.SOUTH, Direction.EAST, Direction.NORTH, Direction.UP, Direction.DOWN));

        relativeSides.put(Direction.UP, List.of(Direction.DOWN, Direction.WEST, Direction.UP, Direction.EAST, Direction.NORTH, Direction.SOUTH));
        relativeSides.put(Direction.DOWN, List.of(Direction.UP, Direction.WEST, Direction.DOWN, Direction.EAST, Direction.NORTH, Direction.SOUTH));

        return relativeSides;
    }

    static Direction resolveFacingRelatedSide(BlockState state, Direction side) {

        if (state.hasProperty(HORIZONTAL_FACING)) {
            Direction facing = state.getValue(HORIZONTAL_FACING);
            return RELATIVE_DIRECTIONS.get(facing).get(RELATIVE_DIRECTIONS.get(Direction.NORTH).indexOf(side));
        }

        if (state.hasProperty(FFACING)) {
            Direction facing = state.getValue(FFACING);

            return RELATIVE_DIRECTIONS.get(facing).get(RELATIVE_DIRECTIONS.get(Direction.NORTH).indexOf(side));
        }

        return side;
    }

    boolean canConnectPipe(BlockState state, Direction side);

}
