package de.crafty.lifecompat.api.fluid.logistic.container;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

public interface IFluidContainer {


    void setFluidCapacity(int capacity);

    int getFluidCapacity();

    void setVolume(int volume);

    int getVolume();

    Fluid getFluid();

    void setFluid(Fluid fluid);

    int fillWithLiquid(ServerLevel level, BlockPos pos, BlockState state, Fluid liquid, int amount);

    int drainLiquidFrom(ServerLevel level, BlockPos pos, BlockState state, Fluid liquid, int amount);

    boolean canDrainLiquid(ServerLevel level, BlockPos pos, BlockState state);

    boolean canFillLiquid(ServerLevel level, BlockPos pos, BlockState state);
}
