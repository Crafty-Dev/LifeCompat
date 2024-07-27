package de.crafty.lifecompat.api.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public interface IEnergyHolder {

    //Should return the maximun capacity of this block
    int getCapacity(ServerLevel level, BlockPos pos, BlockState state);

    //Should return the current energy amount this block contains
    int getStoredEnergy();
}
