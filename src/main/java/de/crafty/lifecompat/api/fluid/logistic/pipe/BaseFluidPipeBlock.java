package de.crafty.lifecompat.api.fluid.logistic.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public abstract class BaseFluidPipeBlock extends Block {

    public BaseFluidPipeBlock(Properties properties) {
        super(properties);
    }


    @Override
    protected void onPlace(BlockState blockState, Level level, BlockPos blockPos, BlockState oldState, boolean bl) {
        if(level.isClientSide()) return;

        if(blockState.is(oldState.getBlock()) && level.getBlockEntity(blockPos) instanceof AbstractFluidPipeBlockEntity pipe) {
            pipe.validateNetwork();
        }

        if(!blockState.is(oldState.getBlock()) && level.getBlockEntity(blockPos) instanceof AbstractFluidPipeBlockEntity pipe) {
            pipe.onPlace();
        }

    }


    @Override
    protected void onRemove(BlockState blockState, Level level, BlockPos blockPos, BlockState newState, boolean bl) {
        if(blockState.is(newState.getBlock()) || level.isClientSide()){
            super.onRemove(blockState, level, blockPos, newState, bl);
            return;
        }


        if(level.getBlockEntity(blockPos) instanceof AbstractFluidPipeBlockEntity pipe)
            pipe.onDestroyed();

    }
}
