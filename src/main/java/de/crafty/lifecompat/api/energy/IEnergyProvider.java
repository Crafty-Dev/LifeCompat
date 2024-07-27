package de.crafty.lifecompat.api.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public interface IEnergyProvider {

    //Whether this Block can currently transfer Energy to other consumers
    boolean isTransferring(ServerLevel level, BlockPos pos, BlockState state);

    //Whether this Block is currently generating Energy
    boolean isGenerating(ServerLevel level, BlockPos pos, BlockState state);

    List<Direction> getOutputDirections(ServerLevel level, BlockPos pos, BlockState state);

    //The maximum amount of energy this block can transfer at once (per tick)
    int getMaxOutput(ServerLevel level, BlockPos pos, BlockState state);


    //Returns the amount of energy currelty produced by this block
    int getGenerationPerTick(ServerLevel level, BlockPos pos, BlockState state);
}
