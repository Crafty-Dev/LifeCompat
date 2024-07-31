package de.crafty.lifecompat.energy.blockentity;

import de.crafty.lifecompat.api.energy.container.AbstractEnergyContainer;
import de.crafty.lifecompat.energy.block.BaseEnergyBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public abstract class SimpleEnergyStorageBlockEntity extends AbstractEnergyContainer {

    public SimpleEnergyStorageBlockEntity(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState, int energyCapacity) {
        super(blockEntityType, blockPos, blockState, energyCapacity);
    }

    @Override
    public boolean isAccepting(ServerLevel world, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public boolean isConsuming(ServerLevel level, BlockPos pos, BlockState state) {
        return false;
    }

    @Override
    public int getConsumptionPerTick(ServerLevel world, BlockPos pos, BlockState state) {
        return 0;
    }


    @Override
    public boolean isTransferring(ServerLevel level, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public boolean isGenerating(ServerLevel level, BlockPos pos, BlockState state) {
        return false;
    }

    @Override
    public int getGenerationPerTick(ServerLevel level, BlockPos pos, BlockState state) {
        return 0;
    }

    @Override
    public List<Direction> getOutputDirections(ServerLevel world, BlockPos pos, BlockState state) {
        List<Direction> directions = new ArrayList<>();

        for (Direction side : Direction.values()) {
            BaseEnergyBlock.IOMode ioMode = BaseEnergyBlock.getIOMode(state, side);
            if (ioMode.isOutput())
                directions.add(side);
        }

        return directions;
    }

    @Override
    public List<Direction> getInputDirections(ServerLevel level, BlockPos pos, BlockState state) {
        List<Direction> directions = new ArrayList<>();

        for (Direction side : Direction.values()) {
            BaseEnergyBlock.IOMode ioMode = BaseEnergyBlock.getIOMode(state, side);
            if (ioMode.isInput())
                directions.add(side);
        }

        return directions;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SimpleEnergyStorageBlockEntity blockEntity){
        if(level.isClientSide())
            return;

        blockEntity.energyTick((ServerLevel) level, pos, state);
    }
}
