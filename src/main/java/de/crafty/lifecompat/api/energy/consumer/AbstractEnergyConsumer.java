package de.crafty.lifecompat.api.energy.consumer;

import de.crafty.lifecompat.api.energy.IEnergyConsumer;
import de.crafty.lifecompat.api.energy.IEnergyHolder;
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

public abstract class AbstractEnergyConsumer extends BlockEntity implements IEnergyConsumer, IEnergyHolder {

    private int energyCacheSize;
    private int energy;

    public AbstractEnergyConsumer(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState, int energyCacheSize) {
        super(blockEntityType, blockPos, blockState);

        this.energyCacheSize = energyCacheSize;
    }

    @Override
    public int getEnergyCapacity() {
        return this.energyCacheSize;
    }

    @Override
    public void setEnergyCapacity(int capacity) {
        this.energyCacheSize = capacity;
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
    public int receiveEnergy(ServerLevel level, BlockPos pos, BlockState state, Direction from, int energy) {
        if(!this.isAccepting(level, pos, state))
            return energy;

        int clampedInput = Math.min(energy, this.getMaxInput(level, pos, state));

        int updated = this.energy + clampedInput;
        this.energy = Math.min(updated, this.getEnergyCapacity());
        this.setChanged();
        level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);

        return (updated - this.energy) + (energy - clampedInput);
    }

    protected void energyTick(ServerLevel level, BlockPos pos, BlockState state){
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

    protected abstract void performAction(ServerLevel level, BlockPos pos, BlockState state);


    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putInt("energy", this.energy);
        tag.putInt("energyCapacity", this.energyCacheSize);
    }

    @Override
    protected void loadAdditional(CompoundTag compoundTag, HolderLookup.Provider provider) {
        this.energy = compoundTag.getInt("energy");
        this.energyCacheSize = compoundTag.getInt("energyCapacity");

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
