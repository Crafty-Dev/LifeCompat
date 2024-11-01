package de.crafty.lifecompat.api.energy.container;

import de.crafty.lifecompat.api.energy.IEnergyConsumer;
import de.crafty.lifecompat.api.energy.IEnergyHolder;
import de.crafty.lifecompat.api.energy.IEnergyProvider;
import de.crafty.lifecompat.api.energy.provider.AbstractEnergyProvider;
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
import org.jetbrains.annotations.Nullable;

public abstract class AbstractEnergyContainer extends BlockEntity implements IEnergyProvider, IEnergyConsumer, IEnergyHolder {

    private final int energyCapacity;
    private int energy;

    public AbstractEnergyContainer(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState, int energyCapacity) {
        super(blockEntityType, blockPos, blockState);

        this.energyCapacity = energyCapacity;
    }

    //Returns the energy capacity depending on the block itself
    //Maybe usefull for upgrades, etc...
    @Override
    public int getCapacity(ServerLevel level, BlockPos pos, BlockState state) {
        return this.energyCapacity;
    }

    //Returns the standard energy capacity
    public int getStandardEnergyCapacity() {
        return this.energyCapacity;
    }

    @Override
    public int getStoredEnergy() {
        return this.energy;
    }

    @Override
    public int receiveEnergy(ServerLevel level, BlockPos pos, BlockState state, Direction from, int energy) {
        if (!this.isAccepting(level, pos, state))
            return energy;

        int clampedInput = Math.min(energy, this.getMaxInput(level, pos, state));

        int prevEnergy = this.energy;

        int updated = this.energy + clampedInput;
        this.energy = Math.min(updated, this.getCapacity(level, pos, state));

        if(this.energy != prevEnergy){
            this.setChanged();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
            //Block last Input Side as output for the next 6 ticks (0-5), to prevent loops
        }

        return (updated - this.energy) + (energy - clampedInput);
    }

    protected void energyTick(ServerLevel level, BlockPos pos, BlockState state) {
        if (!this.isTransferring(level, pos, state))
            return;


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

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putInt("energy", this.energy);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        this.energy = tag.getInt("energy");
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
