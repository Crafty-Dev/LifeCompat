package de.crafty.lifecompat.api.energy.container;

import de.crafty.lifecompat.api.energy.IEnergyConsumer;
import de.crafty.lifecompat.api.energy.IEnergyHolder;
import de.crafty.lifecompat.api.energy.IEnergyProvider;
import de.crafty.lifecompat.api.energy.provider.AbstractEnergyProvider;
import de.crafty.lifecompat.energy.block.BaseEnergyBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractEnergyContainer extends BlockEntity implements IEnergyProvider, IEnergyConsumer, IEnergyHolder {

    private int energyCapacity;
    private int energy;

    public AbstractEnergyContainer(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState, int energyCapacity) {
        super(blockEntityType, blockPos, blockState);

        this.energyCapacity = energyCapacity;
    }

    //Returns the energy capacity depending on the block itself
    //Maybe usefull for upgrades, etc...

    //Returns the standard energy capacity


    @Override
    public int getEnergyCapacity() {
        return this.energyCapacity;
    }

    @Override
    public void setEnergyCapacity(int capacity) {
        this.energyCapacity = capacity;
    }

    @Override
    public int getStoredEnergy() {
        return this.energy;
    }

    @Override
    public void setStoredEnergy(int energy) {
        this.energy = energy;
    }

    @Override
    public int receiveEnergy(ServerLevel level, BlockPos pos, BlockState state, Direction from, int energy) {
        if (!this.isAccepting(level, pos, state))
            return energy;

        int clampedInput = Math.min(energy, this.getMaxInput(level, pos, state));

        int prevEnergy = this.energy;

        int updated = this.energy + clampedInput;
        this.energy = Math.min(updated, this.getEnergyCapacity());

        if(this.energy != prevEnergy){
            this.setChanged();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
            //Block last Input Side as output for the next 6 ticks (0-5), to prevent loops
        }

        return (updated - this.energy) + (energy - clampedInput);
    }

    protected abstract void performAction(ServerLevel level, BlockPos pos, BlockState state);

    protected void energyTick(ServerLevel level, BlockPos pos, BlockState state) {
        if (this.isGenerating(level, pos, state)) {
            this.energy = Math.min(this.energy + this.getGenerationPerTick(level, pos, state), this.getEnergyCapacity());
            this.setChanged();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }

        if (this.isTransferring(level, pos, state)){
            boolean changed = false;
            for (Direction outputSide : this.getOutputDirections(level, pos, state)) {
                int transferable = Math.min(this.energy, this.getMaxOutput(level, pos, state));

                BlockPos consumerPos = pos.relative(outputSide);

                int transferred = AbstractEnergyProvider.transferEnergy(level, consumerPos, level.getBlockState(consumerPos), transferable, outputSide.getOpposite());
                this.energy -= transferred;

                if (transferred > 0 && !changed)
                    changed = true;

            }

            if (changed) {
                this.setChanged();
                level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
            }
        }

        if(!this.isConsuming(level, pos, state))
            return;

        int consumption = this.getConsumptionPerTick(level, pos, state);
        if(this.energy >= consumption){
            this.energy -= consumption;
            this.performAction(level, pos, state);
            this.setChanged();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public List<Direction> getInputDirections(ServerLevel world, BlockPos pos, BlockState state) {
        List<Direction> directions = new ArrayList<>();

        for (Direction side : Direction.values()) {
            EnumProperty<Direction> facingProp = state.hasProperty(BaseEnergyBlock.FACING) ? BaseEnergyBlock.FACING : state.hasProperty(BaseEnergyBlock.HORIZONTAL_FACING) ? BaseEnergyBlock.HORIZONTAL_FACING : null;
            EnumProperty<BaseEnergyBlock.IOMode> sideMode = BaseEnergyBlock.calculateIOSide(facingProp != null ? state.getValue(facingProp) : Direction.NORTH, side);

            if (state.hasProperty(sideMode) && state.getValue(sideMode).isInput())
                directions.add(side);
        }

        return directions;
    }

    @Override
    public List<Direction> getOutputDirections(ServerLevel level, BlockPos pos, BlockState state) {
        List<Direction> directions = new ArrayList<>();

        for (Direction side : Direction.values()) {
            EnumProperty<Direction> facingProp = state.hasProperty(BaseEnergyBlock.FACING) ? BaseEnergyBlock.FACING : state.hasProperty(BaseEnergyBlock.HORIZONTAL_FACING) ? BaseEnergyBlock.HORIZONTAL_FACING : null;
            EnumProperty<BaseEnergyBlock.IOMode> sideMode = BaseEnergyBlock.calculateIOSide(facingProp != null ? state.getValue(facingProp) : Direction.NORTH, side);
            if (state.hasProperty(sideMode) && state.getValue(sideMode).isOutput())
                directions.add(side);
        }

        return directions;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putInt("energy", this.energy);
        tag.putInt("energyCapacity", this.energyCapacity);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        this.energy = tag.getInt("energy");
        this.energyCapacity = tag.getInt("energyCapacity");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = super.getUpdateTag(provider);
        this.saveAdditional(tag, provider);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

}
