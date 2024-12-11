package de.crafty.lifecompat.api.fluid.logistic.pipe;

import de.crafty.lifecompat.api.fluid.logistic.container.IFluidContainer;
import de.crafty.lifecompat.api.fluid.logistic.container.IFluidContainerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractFluidPipeBlockEntity extends BlockEntity {

    private Fluid fluid;
    private int buffer, bufferCapacity;

    private final Map<Direction, TransferMode> transferModes = new HashMap<>();
    private Map<Direction, Boolean> currentChunkStates = new HashMap<>();

    private List<FluidContainerTarget> fluidExtractionTargets = new ArrayList<>();
    private List<FluidContainerTarget> fluidInsertionTargets = new ArrayList<>();

    private List<FluidContainerTarget> fluidInsertionQueue = new ArrayList<>();

    private boolean initialized;
    private long lastTick;
    private int emptyTick;

    public AbstractFluidPipeBlockEntity(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState, int initialBufferCapacity) {
        super(blockEntityType, blockPos, blockState);

        this.fluid = Fluids.EMPTY;
        this.buffer = 0;
        this.bufferCapacity = initialBufferCapacity;

        this.initialized = false;
        this.lastTick = 0;
        this.emptyTick = -1;
    }


    private void initChunkCache() {

        BlockPos blockPos = this.getBlockPos();

        int x = blockPos.getX() & 15;
        int z = blockPos.getZ() & 15;
        if (x == 0 || x == 15 || z == 0 || z == 15) {
            HashMap<Direction, Boolean> currentChunkStates = new HashMap<>();

            if (x == 0)
                currentChunkStates.put(Direction.WEST, true);
            if (x == 15)
                currentChunkStates.put(Direction.EAST, true);

            if (z == 0)
                currentChunkStates.put(Direction.NORTH, true);
            if (z == 15)
                currentChunkStates.put(Direction.SOUTH, true);

            this.currentChunkStates = currentChunkStates;
            this.setChanged();
        }

    }

    private void initTransferModes() {
        BlockState state = this.getBlockState();

       for(Direction side : Direction.values()){
           this.transferModes.put(side, TransferMode.EXTRACTING);
       }

        this.setChanged();
    }

    private void setBufferCapacity(int bufferCapacity) {
        this.bufferCapacity = bufferCapacity;
        this.setChanged();
    }

    public int getBufferCapacity() {
        return this.bufferCapacity;
    }

    public void setBuffer(int buffer) {
        this.buffer = buffer;
        this.setChanged();
    }

    public abstract int getExtractionRate();

    public abstract int getInsertionRate();

    public int getBuffer() {
        return this.buffer;
    }

    public void setFluid(Fluid fluid) {
        if (fluid == this.getBufferedFluid())
            return;

        this.fluid = fluid;
        this.level.gameEvent(GameEvent.BLOCK_CHANGE, this.getBlockPos(), GameEvent.Context.of(this.getBlockState()));
        this.setChanged();
        this.getLevel().sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
    }

    public Fluid getBufferedFluid() {
        return this.fluid;
    }

    public TransferMode getTransferMode(Direction direction) {
        return this.transferModes.getOrDefault(direction, TransferMode.NONE);
    }

    public void setTransferMode(Direction direction, TransferMode mode) {
        this.transferModes.put(direction, mode);
        this.level.gameEvent(GameEvent.BLOCK_CHANGE, this.getBlockPos(), GameEvent.Context.of(this.getBlockState()));
        this.setChanged();
        this.getLevel().sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
    }


    public void onPlace() {
        this.initialized = true;

        this.initChunkCache();
        this.initTransferModes();
        this.updatePipe((ServerLevel) this.getLevel(), List.of());
    }

    public void onDestroyed() {
        if (this.initialized)
            this.updatePipe((ServerLevel) this.getLevel(), List.of(this.getBlockPos()));
    }

    public void validateNetwork() {
        this.updatePipe((ServerLevel) this.getLevel(), List.of());
    }

    public void fluidTick(ServerLevel level, BlockPos pos, BlockState state) {

        if (!this.initialized)
            return;

        //Extraction
        for (Direction side : Direction.values()) {
            if(BaseFluidPipeBlock.getConnectionStateForNeighbor(level, pos, side) != BaseFluidPipeBlock.ConnectionState.ATTACHED)
                continue;

            if (this.getTransferMode(side).isExtracting()) {
                BlockPos relative = pos.relative(side);
                BlockState fluidContainerState = level.getBlockState(relative);

                if (level.getBlockEntity(relative) instanceof IFluidContainer fluidContainer) {
                    Fluid containerFluid = fluidContainer.getFluid();

                    if (containerFluid == Fluids.EMPTY)
                        continue;

                    int extracted = fluidContainer.drainLiquidFrom(level, relative, fluidContainerState, this.fluid == Fluids.EMPTY ? containerFluid : this.fluid, Math.min(this.getBufferCapacity() - this.getBuffer(), this.getExtractionRate()));
                    if (extracted == 0)
                        continue;

                    if (this.fluid == Fluids.EMPTY) {
                        this.setFluid(containerFluid);
                        this.setBuffer(extracted);
                        this.validateNetwork();
                    } else
                        this.setBuffer(this.getBuffer() + extracted);

                }
            }
        }

        if (this.fluid == Fluids.EMPTY)
            return;


        int targetAmount = 0;
        //Insertion
        for (FluidContainerTarget target : this.fluidInsertionQueue) {

            if (this.getBuffer() <= 0)
                break;

            if (level.getBlockEntity(target.pos()) instanceof IFluidContainer fluidContainer) {
                int transferred = fluidContainer.fillWithLiquid(level, target.pos(), level.getBlockState(target.pos()), this.fluid, Math.min(this.getBuffer(), this.getInsertionRate()));
                if (transferred == 0)
                    continue;

                this.setBuffer(this.getBuffer() - transferred);
                if (this.getBuffer() <= 0)
                    this.emptyTick = 0;


                targetAmount++;
            }
        }

        for (int i = 0; i < targetAmount; i++) {
            this.fluidInsertionQueue.add(this.fluidInsertionQueue.removeFirst());
        }

        this.setChanged();

    }


    private boolean chunkDataChangedToUnloaded(ServerLevel level) {
        if (this.currentChunkStates.isEmpty())
            return false;

        boolean changed = false;

        for (Direction side : this.currentChunkStates.keySet()) {
            boolean loaded = this.currentChunkStates.get(side);

            if (level.getChunkAt(this.getBlockPos().relative(side)).getFullStatus().isOrAfter(FullChunkStatus.BLOCK_TICKING) != loaded) {
                this.currentChunkStates.put(side, !loaded);
                this.setChanged();
                changed = loaded;
            }
        }

        return changed;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, AbstractFluidPipeBlockEntity blockEntity) {
        if (level.isClientSide())
            return;

        boolean changed = false;

        if (blockEntity.lastTick < level.getGameTime() - 1)
            blockEntity.updatePipe((ServerLevel) level, List.of());
        else {
            blockEntity.lastTick = level.getGameTime();
            changed = true;
        }

        if (blockEntity.chunkDataChangedToUnloaded((ServerLevel) level))
            blockEntity.updatePipe((ServerLevel) level, List.of());

        if (blockEntity.emptyTick >= 0) {
            if (blockEntity.getBuffer() > 0) {
                blockEntity.emptyTick = -1;
            } else {
                blockEntity.emptyTick++;
            }
            changed = true;

            if (blockEntity.emptyTick >= 5) {
                blockEntity.emptyTick = -1;
                blockEntity.validateNetwork();
            }
        }

        if (changed)
            blockEntity.setChanged();

        blockEntity.fluidTick((ServerLevel) level, pos, state);
    }

    private void updatePipe(ServerLevel level, List<BlockPos> excludedPositions) {
        List<BlockPos> pipes = new ArrayList<>();
        this.trackPipes(level, this.getBlockPos(), pipes, excludedPositions);

        if (!pipes.contains(this.getBlockPos()) && !excludedPositions.contains(this.getBlockPos()))
            pipes.add(this.getBlockPos());

        List<FluidContainerTarget> extractionTargets = new ArrayList<>();
        List<FluidContainerTarget> insertionTargets = new ArrayList<>();


        Fluid currentFluid = Fluids.EMPTY;

        for (BlockPos pipePos : pipes) {
            for (Direction side : Direction.values()) {
                BlockPos relative = pipePos.relative(side);
                BlockState state = level.getBlockState(relative);
                if (state.getBlock() instanceof IFluidContainerBlock fluidContainer && fluidContainer.canConnectPipe(state, side.getOpposite()) && this.getTransferMode(side).isExtracting())
                    extractionTargets.add(new FluidContainerTarget(relative, side));

                if (state.getBlock() instanceof IFluidContainerBlock fluidContainer && fluidContainer.canConnectPipe(state, side.getOpposite()) && this.getTransferMode(side).isInserting())
                    insertionTargets.add(new FluidContainerTarget(relative, side));
            }

            if (level.getBlockEntity(pipePos) instanceof AbstractFluidPipeBlockEntity fluidPipe && fluidPipe.getBufferedFluid() != Fluids.EMPTY && fluidPipe.getBuffer() > 0)
                currentFluid = fluidPipe.getBufferedFluid();
        }


        for (BlockPos pipePos : pipes) {
            if (level.getBlockEntity(pipePos) instanceof AbstractFluidPipeBlockEntity pipe)
                pipe.updateNetworkInfo(currentFluid, level.getGameTime(), this.currentChunkStates, extractionTargets, insertionTargets);
        }
    }

    private void trackPipes(ServerLevel level, BlockPos current, List<BlockPos> pipes, List<BlockPos> excludedPositions) {
        for (Direction side : Direction.values()) {
            BlockPos relativePos = current.relative(side);

            if (level.isLoaded(relativePos) && !excludedPositions.contains(relativePos) && level.getBlockEntity(relativePos) instanceof AbstractFluidPipeBlockEntity && !pipes.contains(relativePos)) {
                pipes.add(relativePos);
                this.trackPipes(level, relativePos, pipes, excludedPositions);
            }
        }
    }

    private void updateNetworkInfo(Fluid currentFluid, long lastTick, Map<Direction, Boolean> presentChunkStates, List<FluidContainerTarget> fluidExtractionTargets, List<FluidContainerTarget> fluidInsertionTargets) {

        presentChunkStates.forEach((side, currentlyLoaded) -> {
            if (this.currentChunkStates.containsKey(side))
                this.currentChunkStates.put(side, currentlyLoaded);
        });

        this.fluidExtractionTargets = fluidExtractionTargets;

        List<FluidContainerTarget> removedTargets = new ArrayList<>();
        List<FluidContainerTarget> addedTargets = new ArrayList<>();

        fluidInsertionTargets.forEach(target -> {
            if (!this.fluidInsertionTargets.contains(target))
                addedTargets.add(target);
        });
        this.fluidInsertionTargets.forEach(target -> {
            if (!fluidInsertionTargets.contains(target))
                removedTargets.add(target);
        });

        this.fluidInsertionTargets = fluidInsertionTargets;

        this.fluidInsertionQueue.removeAll(removedTargets);
        this.fluidInsertionQueue.addAll(0, addedTargets);


        //For transferring pipes
        if (currentFluid != this.getBufferedFluid())
            this.setFluid(currentFluid);

        this.lastTick = lastTick;
        this.setChanged();
    }


    public void loopThroughTransferMode(Direction direction) {
        TransferMode current = this.getTransferMode(direction);
        this.setTransferMode(direction, current.next());

        if (this.getLevel() instanceof ServerLevel serverLevel)
            this.updatePipe(serverLevel, List.of());
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {

        tag.putString("fluid", BuiltInRegistries.FLUID.getKey(this.fluid).toString());
        tag.putInt("buffer", this.buffer);
        tag.putInt("bufferCapacity", this.bufferCapacity);

        CompoundTag transferModeTag = new CompoundTag();

        for (Direction direction : Direction.values()) {
            transferModeTag.putString(direction.name().toLowerCase(), this.transferModes.getOrDefault(direction, TransferMode.NONE).name());
        }

        tag.put("transferModes", transferModeTag);

        CompoundTag currentChunkStateTag = new CompoundTag();

        this.currentChunkStates.forEach((direction, currentlyLoaded) -> {
            currentChunkStateTag.putBoolean(direction.name().toLowerCase(), currentlyLoaded);
        });

        ListTag extractionTargetTagList = new ListTag();

        this.fluidExtractionTargets.forEach(fluidContainerTarget -> {
            CompoundTag fluidExtractionTag = new CompoundTag();
            fluidExtractionTag.putLong("pos", fluidContainerTarget.pos().asLong());
            fluidExtractionTag.putString("side", fluidContainerTarget.pipeSide().name());
            extractionTargetTagList.add(fluidExtractionTag);
        });
        tag.put("extractionTargets", extractionTargetTagList);

        ListTag insertionTargetTagList = new ListTag();

        this.fluidInsertionTargets.forEach(fluidContainerTarget -> {
            CompoundTag fluidInsertionTag = new CompoundTag();
            fluidInsertionTag.putLong("pos", fluidContainerTarget.pos().asLong());
            fluidInsertionTag.putString("side", fluidContainerTarget.pipeSide().name());
            insertionTargetTagList.add(fluidInsertionTag);
        });
        tag.put("insertionTargets", insertionTargetTagList);

        ListTag insertionQueueTagList = new ListTag();

        this.fluidInsertionQueue.forEach(fluidContainerTarget -> {
            CompoundTag fluidInsertionTag = new CompoundTag();
            fluidInsertionTag.putLong("pos", fluidContainerTarget.pos().asLong());
            fluidInsertionTag.putString("side", fluidContainerTarget.pipeSide().name());
            insertionQueueTagList.add(fluidInsertionTag);
        });
        tag.put("insertionQueue", insertionQueueTagList);

        tag.putBoolean("initialized", this.initialized);
        tag.putLong("lastTick", this.lastTick);
        tag.putInt("emptyTick", this.emptyTick);
    }


    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {

        this.fluid = BuiltInRegistries.FLUID.get(ResourceLocation.parse(tag.getString("fluid")));

        this.buffer = tag.getInt("buffer");
        this.bufferCapacity = tag.getInt("bufferCapacity");


        this.transferModes.clear();
        CompoundTag transferModeTag = tag.getCompound("transferModes");
        transferModeTag.getAllKeys().forEach(side -> {
            this.transferModes.put(Direction.valueOf(side.toUpperCase()), TransferMode.valueOf(transferModeTag.getString(side)));
        });

        this.currentChunkStates.clear();
        CompoundTag currentChunkStateTag = tag.getCompound("currentChunkStates");
        currentChunkStateTag.getAllKeys().forEach(side -> {
            this.currentChunkStates.put(Direction.valueOf(side.toUpperCase()), currentChunkStateTag.getBoolean(side));
        });

        this.fluidExtractionTargets.clear();
        ListTag extractionTargetTagList = tag.getList("extractionTargets", 9);
        extractionTargetTagList.forEach(tag1 -> {
            CompoundTag extractionTag = (CompoundTag) tag1;
            this.fluidExtractionTargets.add(new FluidContainerTarget(BlockPos.of(extractionTag.getLong("pos")), Direction.valueOf(extractionTag.getString("side"))));
        });

        this.fluidInsertionTargets.clear();
        ListTag insertionTargetTagList = tag.getList("extractionTargets", 9);
        insertionTargetTagList.forEach(tag1 -> {
            CompoundTag insertionTag = (CompoundTag) tag1;
            this.fluidInsertionTargets.add(new FluidContainerTarget(BlockPos.of(insertionTag.getLong("pos")), Direction.valueOf(insertionTag.getString("side"))));
        });

        this.fluidInsertionQueue.clear();
        ListTag insertionQueueTagList = tag.getList("insertionQueue", 9);
        insertionQueueTagList.forEach(tag1 -> {
            CompoundTag insertionTag = (CompoundTag) tag1;
            this.fluidInsertionQueue.add(new FluidContainerTarget(BlockPos.of(insertionTag.getLong("pos")), Direction.valueOf(insertionTag.getString("side"))));
        });

        this.initialized = tag.getBoolean("initialized");
        this.lastTick = tag.getLong("lastTick");
        this.emptyTick = tag.getInt("emptyTick");
    }


    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = super.getUpdateTag(provider);
        this.saveAdditional(tag, provider);
        return tag;
    }

    record FluidContainerTarget(BlockPos pos, Direction pipeSide) {

    }

    public enum TransferMode {
        NONE,
        EXTRACTING,
        INSERTING,
        INOUT;


        boolean isInserting() {
            return this == INSERTING || this == INOUT;
        }

        boolean isExtracting() {
            return this == EXTRACTING || this == INOUT;
        }

        TransferMode next() {
            return this.ordinal() < values().length - 1 ? values()[this.ordinal() + 1] : NONE;
        }
    }
}
