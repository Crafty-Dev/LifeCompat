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

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractEnergyContainer extends BlockEntity implements IEnergyProvider, IEnergyConsumer, IEnergyHolder {

    private final int energyCapacity;
    private int energy;
    private final List<Direction> lastTransferredDirections = new ArrayList<>();

    //Blocks the output to the specified when the block recently received energy from there
    //Prevents infinite "push-pull" loops
    //Lasts one tick
    private final List<Direction> blockOutput = new ArrayList<>();

    public AbstractEnergyContainer(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState, int energyCapacity) {
        super(blockEntityType, blockPos, blockState);

        this.energyCapacity = energyCapacity;
    }

    @Override
    public int getCapacity(ServerLevel level, BlockPos pos, BlockState state) {
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

        int updated = this.energy + clampedInput;
        this.energy = Math.min(updated, this.getCapacity(level, pos, state));
        this.setChanged();
        level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        if(!this.blockOutput.contains(from))
            this.blockOutput.add(from);

        return (updated - this.energy) + (energy - clampedInput);
    }

    protected void energyTick(ServerLevel level, BlockPos pos, BlockState state){
        if(!this.isTransferring(level, pos, state))
            return;

        int transferable = Math.min(this.energy, this.getMaxOutput(level, pos, state));

        boolean changed = false;
        for(Direction outputSide : AbstractEnergyProvider.getSortedOutputs(this, level, pos, state, this.lastTransferredDirections)){
            if(this.blockOutput.remove(outputSide)){
                System.out.println("Skipped");
                continue;
            }

            BlockPos consumerPos = pos.relative(outputSide);
            int transferred = AbstractEnergyProvider.transferEnergy(level, consumerPos, level.getBlockState(consumerPos), transferable, outputSide.getOpposite());
            this.energy -= transferred;
            if(transferred > 0){
                this.lastTransferredDirections.add(outputSide);
                changed = true;
            }
            if(transferred == transferable)
                break;

            transferable -= transferred;
        }

        if(changed){
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
