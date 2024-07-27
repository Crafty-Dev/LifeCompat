package de.crafty.lifecompat.api.energy.consumer;

import de.crafty.lifecompat.api.energy.IEnergyConsumer;
import de.crafty.lifecompat.api.energy.IEnergyHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractEnergyConsumer extends BlockEntity implements IEnergyConsumer, IEnergyHolder {

    private final int energyCacheSize;
    private int energy;

    public AbstractEnergyConsumer(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState, int energyCacheSize) {
        super(blockEntityType, blockPos, blockState);

        this.energyCacheSize = energyCacheSize;
    }

    @Override
    public int getCapacity(ServerLevel level, BlockPos pos, BlockState state) {
        return this.energyCacheSize;
    }

    @Override
    public int getStoredEnergy() {
        return this.energy;
    }

    @Override
    public int receiveEnergy(ServerLevel level, BlockPos pos, BlockState state, int energy) {
        if(!this.isAccepting(level, pos, state))
            return energy;

        int clampedInput = Math.min(energy, this.getMaxInput(level, pos, state));

        int updated = this.energy + clampedInput;
        this.energy = Math.min(updated, this.getCapacity(level, pos, state));
        this.setChanged();

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
        }
    }

    protected abstract void performAction(ServerLevel level, BlockPos pos, BlockState state);


    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putInt("energy", this.energy);
    }

    @Override
    protected void loadAdditional(CompoundTag compoundTag, HolderLookup.Provider provider) {
        this.energy = compoundTag.getInt("energy");
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
