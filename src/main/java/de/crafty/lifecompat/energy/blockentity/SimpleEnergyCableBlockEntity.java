package de.crafty.lifecompat.energy.blockentity;

import de.crafty.lifecompat.api.energy.container.AbstractEnergyContainer;
import de.crafty.lifecompat.energy.block.BaseEnergyCable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public abstract class SimpleEnergyCableBlockEntity extends AbstractEnergyContainer {


    private boolean charged;

    //Used to cooldown the switch between the energy state values
    //Prevents State from changing too fast
    private int ticksUncharged;

    public SimpleEnergyCableBlockEntity(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState, int energyCapacity) {
        super(blockEntityType, blockPos, blockState, energyCapacity);
    }

    //Energy Consumer methods

    @Override
    public boolean isAccepting(ServerLevel world, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public boolean isConsuming(ServerLevel level, BlockPos pos, BlockState state) {
        return false;
    }

    @Override
    public List<Direction> getInputDirections(ServerLevel world, BlockPos pos, BlockState state) {
        return List.of(Direction.values());
    }

    @Override
    public int getConsumptionPerTick(ServerLevel world, BlockPos pos, BlockState state) {
        return 0;
    }

    //Energy provider Methods


    @Override
    public boolean isTransferring(ServerLevel level, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public boolean isGenerating(ServerLevel level, BlockPos pos, BlockState state) {
        return false;
    }

    @Override
    public List<Direction> getOutputDirections(ServerLevel level, BlockPos pos, BlockState state) {
        return List.of(Direction.values());
    }

    @Override
    public int getGenerationPerTick(ServerLevel level, BlockPos pos, BlockState state) {
        return 0;
    }


    //Cable logic


    @Override
    public int receiveEnergy(ServerLevel level, BlockPos pos, BlockState state, Direction from, int energy) {
        this.setCharged(true);
        this.resetUncharged();

        return super.receiveEnergy(level, pos, state, from, energy);
    }


    protected void setCharged(boolean charged) {
        this.charged = charged;
        this.setChanged();
    }

    protected void resetUncharged() {
        this.ticksUncharged = 0;
        this.setChanged();
    }

    protected void tickUncharged() {
        this.ticksUncharged++;
        this.setChanged();
    }

    protected boolean isCharged() {
        return this.charged;
    }

    protected int getTicksUncharged() {
        return this.ticksUncharged;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);

        tag.putBoolean("charged", this.charged);
        tag.putInt("ticksUncharged", this.ticksUncharged);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);

        this.charged = tag.getBoolean("charged");
        this.ticksUncharged = tag.getInt("ticksUncharged");
    }

    private static int trackSystemEnergy(Level level, BlockPos pos) {

        List<BlockPos> cablePositions = new ArrayList<>();
        SimpleEnergyCableBlockEntity.trackCablePositions(level, pos, cablePositions);
        int energy = 0;
        for (BlockPos cablePos : cablePositions) {
            SimpleEnergyCableBlockEntity cable = (SimpleEnergyCableBlockEntity) level.getBlockEntity(cablePos);
            energy += cable.getStoredEnergy();
        }

        return energy;
    }

    private static void trackCablePositions(Level level, BlockPos pos, List<BlockPos> cablePositions) {
        List<BlockPos> positions = new ArrayList<>();

        for (Direction side : Direction.values()) {
            if (level.getBlockEntity(pos.relative(side)) instanceof SimpleEnergyCableBlockEntity && !cablePositions.contains(pos.relative(side)))
                positions.add(pos.relative(side));
        }
        cablePositions.addAll(positions);

        for(BlockPos blockPos : positions){
            trackCablePositions(level, blockPos, cablePositions);
        }
    }

    private static boolean isSideConnected(Level level, BlockPos pos){
        for(Direction side : Direction.values()){
            if(BaseEnergyCable.getConnectionStateForNeighbor(level, pos, side) == BaseEnergyCable.ConnectionState.CONNECTED)
                return true;
        }

        return false;
    }


    public static void tick(Level level, BlockPos blockPos, BlockState blockState, SimpleEnergyCableBlockEntity blockEntity) {
        if (level.isClientSide())
            return;

        if (blockEntity.isCharged() && blockEntity.getStoredEnergy() == 0)
            blockEntity.tickUncharged();

        if (blockEntity.isCharged() && !blockState.getValue(BaseEnergyCable.ENERGY))
            level.setBlock(blockPos, blockState.setValue(BaseEnergyCable.ENERGY, true), Block.UPDATE_CLIENTS);

        if (blockEntity.getTicksUncharged() >= 6) {
            blockEntity.setCharged(false);
            blockEntity.resetUncharged();
        }


        if (!blockEntity.isCharged() && blockState.getValue(BaseEnergyCable.ENERGY) && SimpleEnergyCableBlockEntity.isSideConnected(level, blockPos)) {
            if (SimpleEnergyCableBlockEntity.trackSystemEnergy(level, blockPos) == 0)
                level.setBlock(blockPos, blockState.setValue(BaseEnergyCable.ENERGY, false), Block.UPDATE_CLIENTS);
        }

        blockEntity.energyTick((ServerLevel) level, blockPos, blockState);
    }

}
