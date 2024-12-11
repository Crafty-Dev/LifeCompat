package de.crafty.lifecompat.fluid.block;

import de.crafty.lifecompat.api.fluid.logistic.container.IFluidContainerBlock;
import de.crafty.lifecompat.energy.block.BaseEnergyBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public abstract class BaseFluidEnergyBlock extends BaseEnergyBlock implements IFluidContainerBlock {


    protected BaseFluidEnergyBlock(Properties properties, Type energyBlockType, int capacity) {
        super(properties, energyBlockType, capacity);
    }


    public abstract List<Direction> getFluidCompatableSides();

    @Override
    public boolean canConnectPipe(BlockState state, Direction side) {
        return this.getFluidCompatableSides().contains(IFluidContainerBlock.resolveFacingRelatedSide(state, side));
    }
}
