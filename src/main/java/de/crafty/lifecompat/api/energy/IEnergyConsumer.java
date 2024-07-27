package de.crafty.lifecompat.api.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public interface IEnergyConsumer {


    //Whether this Block is currently accepting Energy
    boolean isAccepting(ServerLevel world, BlockPos pos, BlockState state);

    //Whether this Block is currently consuming Energy for actions
    boolean isConsuming(ServerLevel level, BlockPos pos, BlockState state);

    List<Direction> getInputDirections(ServerLevel world, BlockPos pos, BlockState state);

    //The maximum amount of Energy this Block can accept at once (per tick)
    int getMaxInput(ServerLevel world, BlockPos pos, BlockState state);

    /**
     *
     * @param level The world of the provider (Attention!)
     * @param pos The position of the provider
     * @param state The state of the provider
     * @param energy The amount of energy this consumer received (Already in range of its maximum input)
     * @return The overflow that couldn't fit into this block's energy storage
     */
    int receiveEnergy(ServerLevel level, BlockPos pos, BlockState state, int energy);

    //Returns the amount of energy currently consumed by this block
    int getConsumptionPerTick(ServerLevel world, BlockPos pos, BlockState state);
}
