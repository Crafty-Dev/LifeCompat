package de.crafty.lifecompat.api.energy.provider;

import de.crafty.lifecompat.api.energy.IEnergyConsumer;
import de.crafty.lifecompat.api.energy.IEnergyHolder;
import de.crafty.lifecompat.api.energy.IEnergyProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractEnergyProvider extends BlockEntity implements IEnergyProvider, IEnergyHolder {


    private final int energyCacheSize;
    private int energy;
    private final List<Direction> lastTransferredDirections = new ArrayList<>();

    public AbstractEnergyProvider(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState, int energyCacheSize) {
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

    protected void energyTick(ServerLevel level, BlockPos pos, BlockState state){
        if(this.isGenerating(level, pos, state)){
            this.energy = Math.min(this.energy + this.getGenerationPerTick(level, pos, state), this.getCapacity(level, pos, state));
            this.setChanged();
        }

        if(!this.isTransferring(level, pos, state))
            return;

        int transferable = Math.min(this.energy, this.getMaxOutput(level, pos, state));

        boolean changed = false;
        for(Direction outputSide : AbstractEnergyProvider.getSortedOutputs(this, level, pos, state, this.lastTransferredDirections)){
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

        if(changed)
            this.setChanged();
    }


    /**
     *
     * @param level The World
     * @param pos The consumer pos
     * @param state The consumer state
     * @param energy The provided energy
     * @param side The side of the consumer
     * @return The amount transferred to the consumer
     */
    public static int transferEnergy(ServerLevel level, BlockPos pos, BlockState state, int energy, Direction side) {
        if(!(level.getBlockEntity(pos) instanceof IEnergyConsumer consumer))
            return 0;

        if(!consumer.getInputDirections(level, pos, state).contains(side) || !consumer.isAccepting(level, pos, state))
            return 0;

        return energy - consumer.receiveEnergy(level, pos, state, energy);
    }

    public static List<Direction> getSortedOutputs(IEnergyProvider provider, ServerLevel level, BlockPos pos, BlockState state, List<Direction> lastTransferredDirections) {
        List<Direction> sorted = new ArrayList<>();
        provider.getOutputDirections(level, pos, state).stream().filter(direction -> !lastTransferredDirections.contains(direction)).forEach(sorted::add);
        provider.getOutputDirections(level, pos, state).stream().filter(direction -> !sorted.contains(direction)).forEach(sorted::add);
        lastTransferredDirections.clear();
        return sorted;
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
