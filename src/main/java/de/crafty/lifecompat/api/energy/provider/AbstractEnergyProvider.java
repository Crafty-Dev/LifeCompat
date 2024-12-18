package de.crafty.lifecompat.api.energy.provider;

import de.crafty.lifecompat.api.energy.IEnergyConsumer;
import de.crafty.lifecompat.api.energy.IEnergyHolder;
import de.crafty.lifecompat.api.energy.IEnergyProvider;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractEnergyProvider extends BlockEntity implements IEnergyProvider, IEnergyHolder {


    private int energyCacheSize;
    private int energy;
    private final List<Direction> lastTransferredDirections = new ArrayList<>();

    public AbstractEnergyProvider(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState, int energyCacheSize) {
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
    public List<Direction> getOutputDirections(ServerLevel level, BlockPos pos, BlockState state) {
        List<Direction> directions = new ArrayList<>();

        for (Direction side : Direction.values()) {
            DirectionProperty facingProp = state.hasProperty(BaseEnergyBlock.FACING) ? BaseEnergyBlock.FACING : state.hasProperty(BaseEnergyBlock.HORIZONTAL_FACING) ? BaseEnergyBlock.HORIZONTAL_FACING : null;
            EnumProperty<BaseEnergyBlock.IOMode> sideMode = BaseEnergyBlock.calculateIOSide(facingProp != null ? state.getValue(facingProp) : Direction.NORTH, side);
            if (state.hasProperty(sideMode) && state.getValue(sideMode).isOutput())
                directions.add(side);
        }

        return directions;
    }

    protected void energyTick(ServerLevel level, BlockPos pos, BlockState state) {
        if (this.isGenerating(level, pos, state)) {
            this.energy = Math.min(this.energy + this.getGenerationPerTick(level, pos, state), this.getEnergyCapacity());
            this.setChanged();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }

        if (!this.isTransferring(level, pos, state))
            return;

        int transferable = Math.min(this.energy, this.getMaxOutput(level, pos, state));
        if (transferable == 0)
            return;

        boolean changed = false;
        for (Direction outputSide : AbstractEnergyProvider.getSortedOutputs(this, level, pos, state, this.lastTransferredDirections)) {
            BlockPos consumerPos = pos.relative(outputSide);
            int transferred = AbstractEnergyProvider.transferEnergy(level, consumerPos, level.getBlockState(consumerPos), transferable, outputSide.getOpposite());
            this.energy -= transferred;
            if (transferred > 0) {
                this.lastTransferredDirections.add(outputSide);
                changed = true;
            }
            if (transferred == transferable)
                break;

            transferable -= transferred;
        }

        if (changed) {
            this.setChanged();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }


    /**
     * @param level  The World
     * @param pos    The consumer pos
     * @param state  The consumer state
     * @param energy The provided energy
     * @param side   The side of the consumer
     * @return The amount transferred to the consumer
     */
    public static int transferEnergy(ServerLevel level, BlockPos pos, BlockState state, int energy, Direction side) {
        if (!(level.getBlockEntity(pos) instanceof IEnergyConsumer consumer))
            return 0;

        if (!consumer.getInputDirections(level, pos, state).contains(side) || !consumer.isAccepting(level, pos, state))
            return 0;

        return energy - consumer.receiveEnergy(level, pos, state, side, energy);
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
        tag.putInt("energyCapacity", this.energyCacheSize);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        this.energy = tag.getInt("energy");
        this.energyCacheSize = tag.getInt("energyCapacity");
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
