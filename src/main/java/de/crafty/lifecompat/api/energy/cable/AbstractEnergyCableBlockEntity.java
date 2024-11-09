package de.crafty.lifecompat.api.energy.cable;

import de.crafty.lifecompat.api.energy.IEnergyConsumer;
import de.crafty.lifecompat.api.energy.IEnergyHolder;
import de.crafty.lifecompat.api.energy.IEnergyProvider;
import de.crafty.lifecompat.api.energy.provider.AbstractEnergyProvider;
import de.crafty.lifecompat.energy.block.BaseEnergyCable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class AbstractEnergyCableBlockEntity extends BlockEntity implements IEnergyConsumer, IEnergyHolder, IEnergyProvider {


    private int energyCapacity;
    private int energy;

    private HashMap<Direction, Boolean> attachedChunkData = new HashMap<>();

    private long lastTick = -1;

    private boolean bufferUnlocked = false;

    //Device Lists
    private List<DeviceData> connectedConsumers = new ArrayList<>();
    private List<DeviceData> connectedContainers = new ArrayList<>();
    private List<DeviceData> connectedProviders = new ArrayList<>();

    private List<DeviceData> transferQueue = new ArrayList<>();


    private int ticksUncharged = -1;

    public AbstractEnergyCableBlockEntity(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState, int energyCapacity) {
        super(blockEntityType, blockPos, blockState);

        this.energyCapacity = energyCapacity;
        this.energy = 0;

    }

    @Override
    public int getCapacity() {
        return this.energyCapacity;
    }

    @Override
    public int getStoredEnergy() {
        return this.energy;
    }

    @Override
    public boolean isAccepting(ServerLevel serverLevel, BlockPos blockPos, BlockState blockState) {
        return this.isBufferUnlocked();
    }

    @Override
    public boolean isConsuming(ServerLevel serverLevel, BlockPos blockPos, BlockState blockState) {
        return false;
    }

    @Override
    public List<Direction> getInputDirections(ServerLevel serverLevel, BlockPos blockPos, BlockState blockState) {
        return List.of(Direction.values());
    }


    @Override
    public int receiveEnergy(ServerLevel serverLevel, BlockPos blockPos, BlockState blockState, Direction direction, int incoming) {
        if (!this.isAccepting(serverLevel, blockPos, blockState))
            return incoming;

        int clampedInput = Math.min(incoming, this.getMaxInput(serverLevel, blockPos, blockState));
        int updated = this.energy + clampedInput;

        this.setEnergy(Math.min(updated, this.getCapacity()));
        return (updated - this.energy) + (incoming - clampedInput);
    }

    @Override
    public int getConsumptionPerTick(ServerLevel serverLevel, BlockPos blockPos, BlockState blockState) {
        return 0;
    }

    @Override
    public boolean isTransferring(ServerLevel serverLevel, BlockPos blockPos, BlockState blockState) {
        return this.isBufferUnlocked();
    }

    @Override
    public boolean isGenerating(ServerLevel serverLevel, BlockPos blockPos, BlockState blockState) {
        return false;
    }

    @Override
    public List<Direction> getOutputDirections(ServerLevel serverLevel, BlockPos blockPos, BlockState blockState) {
        return List.of(Direction.values());
    }

    @Override
    public int getGenerationPerTick(ServerLevel serverLevel, BlockPos blockPos, BlockState blockState) {
        return 0;
    }


    public void onPlaced() {

        BlockPos blockPos = this.getBlockPos();

        int x = blockPos.getX() & 15;
        int z = blockPos.getZ() & 15;
        if (x == 0 || x == 15 || z == 0 || z == 15) {
            HashMap<Direction, Boolean> attachedChunks = new HashMap<>();

            if (x == 0)
                attachedChunks.put(Direction.WEST, true);
            if (x == 15)
                attachedChunks.put(Direction.EAST, true);

            if (z == 0)
                attachedChunks.put(Direction.NORTH, true);
            if (z == 15)
                attachedChunks.put(Direction.SOUTH, true);

            this.initChunkData(attachedChunks);
        }

        this.updateNetworkCables((ServerLevel) this.getLevel(), List.of());
    }

    public void onDestroyed() {
        this.updateNetworkCables((ServerLevel) this.getLevel(), List.of(this.getBlockPos()));
    }

    private void initChunkData(HashMap<Direction, Boolean> data) {
        this.attachedChunkData = data;
        this.bufferUnlocked = true;
        this.setChanged();
    }

    public boolean chunkDataChangedToUnloaded(ServerLevel level) {
        if (this.attachedChunkData.isEmpty())
            return false;

        boolean changed = false;

        for (Direction side : this.attachedChunkData.keySet()) {
            boolean loaded = this.attachedChunkData.get(side);

            BlockPos attached = this.getBlockPos().relative(side);
            if (level.getChunkAt(attached).getFullStatus().isOrAfter(FullChunkStatus.BLOCK_TICKING) != loaded) {
                this.attachedChunkData.put(side, !loaded);
                this.setChanged();
                changed = loaded;
            }
        }
        return changed;
    }

    private void updateNetworkCables(ServerLevel level, List<BlockPos> exclude) {
        HashMap<BlockPos, List<BlockPos>> trackedNetworks = new HashMap<>();

        for (Direction side : Direction.values()) {
            BlockPos sidePos = this.getBlockPos().relative(side);
            if (!(level.getBlockEntity(sidePos) instanceof AbstractEnergyCableBlockEntity))
                continue;

            List<BlockPos> cables = this.trackNetworkCables(level, sidePos, new ArrayList<>(), exclude);
            if (cables.stream().anyMatch(trackedNetworks::containsKey))
                continue;

            trackedNetworks.put(sidePos, cables);
        }

        if (trackedNetworks.isEmpty() && !exclude.contains(this.getBlockPos()))
            trackedNetworks.put(this.getBlockPos(), List.of(this.getBlockPos()));

        ChunkPos chunkPos = new ChunkPos(this.getBlockPos());

        trackedNetworks.forEach((pos, cableList) -> {
            DeviceData[][] devices = this.trackDevices(level, cableList);

            BlockState state = level.getBlockState(pos);

            if (level.getBlockEntity(pos) instanceof AbstractEnergyCableBlockEntity cable) {

                //TODO && no cable through unloaded chunk
                if (!cable.isNetworkActive(level, cableList) && state.getBlock() instanceof BaseEnergyCable && state.getValue(BaseEnergyCable.ENERGY))
                    level.setBlock(pos, state.setValue(BaseEnergyCable.ENERGY, false), Block.UPDATE_CLIENTS);
            }

            cableList.forEach(cablePos -> {
                if (level.getBlockEntity(cablePos) instanceof AbstractEnergyCableBlockEntity cable)
                    cable.updateNetworkInfo(chunkPos, level.getGameTime(), this.attachedChunkData, List.of(devices[0]), List.of(devices[1]), List.of(devices[2]));
            });
        });
    }


    private DeviceData[][] trackDevices(ServerLevel level, List<BlockPos> cableList) {

        DeviceData[][] devices = new DeviceData[3][];

        List<DeviceData> consumers = new ArrayList<>();
        List<DeviceData> containers = new ArrayList<>();
        List<DeviceData> providers = new ArrayList<>();

        cableList.forEach(pos -> {
            for (Direction side : Direction.values()) {
                BlockPos sidePos = pos.relative(side);

                BlockEntity be = level.getBlockEntity(sidePos);

                if (be == null || be instanceof AbstractEnergyCableBlockEntity cable)
                    continue;

                if (be instanceof IEnergyConsumer && be instanceof IEnergyProvider)
                    containers.add(new DeviceData(sidePos, side.getOpposite()));
                else if (be instanceof IEnergyConsumer)
                    consumers.add(new DeviceData(sidePos, side.getOpposite()));
                else if (be instanceof IEnergyProvider)
                    providers.add(new DeviceData(sidePos, side.getOpposite()));
            }
        });

        devices[0] = consumers.toArray(new DeviceData[0]);
        devices[1] = containers.toArray(new DeviceData[0]);
        devices[2] = providers.toArray(new DeviceData[0]);

        return devices;
    }

    //Exclude is used to simulate the absence of a cable, although there is one at the moment
    //Used when a cable is destroyed to reload new networks
    private List<BlockPos> trackNetworkCables(ServerLevel level, BlockPos currentPos, List<BlockPos> tracked, List<BlockPos> exclude) {
        for (Direction side : Direction.values()) {
            BlockPos sidePos = currentPos.relative(side);

            if (exclude.contains(sidePos) || !level.isLoaded(sidePos) || !level.getChunkAt(sidePos).getFullStatus().isOrAfter(FullChunkStatus.BLOCK_TICKING))
                continue;

            if (tracked.contains(sidePos) || !(level.getBlockEntity(sidePos) instanceof AbstractEnergyCableBlockEntity))
                continue;

            tracked.add(sidePos);
            this.trackNetworkCables(level, sidePos, tracked, exclude);

        }

        return tracked;
    }

    private void updateNetworkInfo(ChunkPos srcChunk, long lastTick, HashMap<Direction, Boolean> attachedChunkData, List<DeviceData> consumers, List<DeviceData> containers, List<DeviceData> providers) {
        this.setLastTick(lastTick);

        //Update chunkData for cables in same chunk as sender
        if (new ChunkPos(this.getBlockPos()).equals(srcChunk)) {

            //Only update attached Chunk Data when it's relevant for the cable
            attachedChunkData.forEach((side, loaded) -> {
                if (this.attachedChunkData.containsKey(side))
                    this.attachedChunkData.put(side, loaded);
            });
        }

        List<DeviceData> newConsumers = new ArrayList<>();
        List<DeviceData> newContainers = new ArrayList<>();

        List<DeviceData> removedConsumers = new ArrayList<>();
        List<DeviceData> removedContainers = new ArrayList<>();

        if (!this.connectedConsumers.equals(consumers)) {
            consumers.forEach(consumerPos -> {
                if (!this.connectedConsumers.contains(consumerPos))
                    newConsumers.add(consumerPos);
            });

            this.connectedConsumers.forEach(consumerPos -> {
                if (!consumers.contains(consumerPos))
                    removedConsumers.add(consumerPos);
            });

            this.connectedConsumers = consumers;
        }

        if (!this.connectedContainers.equals(containers)) {
            containers.forEach(containerPos -> {
                if (!this.connectedContainers.contains(containerPos))
                    newContainers.add(containerPos);
            });

            this.connectedContainers.forEach(containerPos -> {
                if (!containers.contains(containerPos))
                    removedContainers.add(containerPos);
            });

            this.connectedContainers = containers;
        }

        //Update queue
        this.transferQueue.removeAll(removedConsumers);
        this.transferQueue.removeAll(removedContainers);

        this.transferQueue.addAll(newConsumers);
        this.transferQueue.addAll(newContainers);


        this.connectedProviders = providers;
        this.setChanged();
    }

    public void validateNetwork() {
        this.updateNetworkCables((ServerLevel) this.getLevel(), List.of());
    }

    public void validateBuffer(boolean shouldBeUnlocked) {
        if (this.bufferUnlocked != shouldBeUnlocked)
            this.setBufferState(shouldBeUnlocked);
    }


    public boolean isNetworkActive(ServerLevel level, List<BlockPos> cables) {

        boolean active = false;

        for (BlockPos pos : cables) {
            if (level.getBlockEntity(pos) instanceof AbstractEnergyCableBlockEntity cable && (cable.getStoredEnergy() > 0 || cable.ticksUncharged >= 0)) {
                active = true;
                break;
            }
        }

        return active;
    }

    private void setLastTick(long gameTime) {
        this.lastTick = gameTime;
        this.setChanged();
    }

    public long getLastTick() {
        return this.lastTick;
    }

    private void setBufferState(boolean unlocked) {
        this.bufferUnlocked = unlocked;
        this.setChanged();
    }

    public boolean isBufferUnlocked() {
        return this.bufferUnlocked;
    }

    public void setEnergy(int energy) {

        if (this.energy == 0 && energy > 0)
            this.ticksUncharged = -1;

        if (this.energy > 0 && energy == 0)
            this.ticksUncharged = 0;

        this.energy = energy;
        this.setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);

        tag.putInt("energy", this.energy);
        tag.putInt("capacity", this.energyCapacity);
        tag.putInt("ticksUncharged", this.ticksUncharged);

        tag.putBoolean("bufferUnlocked", this.bufferUnlocked);

        tag.putLong("lastTick", this.getLastTick());
        tag.putString("attachedChunkData", this.encodeChunkData());

        tag.putString("connectedConsumers", this.encodeDeviceData(this.connectedConsumers));
        tag.putString("connectedContainers", this.encodeDeviceData(this.connectedContainers));
        tag.putString("connectedProviders", this.encodeDeviceData(this.connectedProviders));

        tag.putString("transferQueue", this.encodeDeviceData(this.transferQueue));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);

        this.energy = tag.getInt("energy");
        this.energyCapacity = tag.getInt("capacity");
        this.ticksUncharged = tag.getInt("ticksUncharged");

        this.bufferUnlocked = tag.getBoolean("bufferUnlocked");

        this.lastTick = tag.getLong("lastTick");
        this.attachedChunkData = this.decodeChunkData(tag.getString("attachedChunkData"));

        this.connectedConsumers = this.decodeDeviceData(tag.getString("connectedConsumers"));
        this.connectedContainers = this.decodeDeviceData(tag.getString("connectedContainers"));
        this.connectedProviders = this.decodeDeviceData(tag.getString("connectedProviders"));

        this.transferQueue = this.decodeDeviceData(tag.getString("transferQueue"));
    }


    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = super.getUpdateTag(provider);
        this.saveAdditional(tag, provider);
        return tag;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public static void tick(Level level, BlockPos blockPos, BlockState blockState, AbstractEnergyCableBlockEntity cable) {

        if (level.isClientSide())
            return;

        if (cable.chunkDataChangedToUnloaded((ServerLevel) level))
            cable.updateNetworkCables((ServerLevel) level, List.of());


        if (cable.getLastTick() >= 0 && cable.getLastTick() < level.getGameTime() - 1)
            cable.updateNetworkCables((ServerLevel) level, List.of());
        else
            cable.setLastTick(level.getGameTime());


        boolean changed = false;

        //Block State Updates
        if (cable.getStoredEnergy() > 0 && !blockState.getValue(BaseEnergyCable.ENERGY))
            level.setBlock(blockPos, blockState.setValue(BaseEnergyCable.ENERGY, true), Block.UPDATE_CLIENTS);

        if (cable.ticksUncharged >= 0) {
            cable.ticksUncharged++;
            changed = true;
        }

        if (cable.ticksUncharged == 5) {
            cable.ticksUncharged = -1;
            if (blockState.getValue(BaseEnergyCable.ENERGY) && !cable.isNetworkActive((ServerLevel) level, cable.trackNetworkCables((ServerLevel) level, blockPos, new ArrayList<>(), List.of())))
                level.setBlock(blockPos, blockState.setValue(BaseEnergyCable.ENERGY, false), Block.UPDATE_CLIENTS);
        }

        int devicesProvided = 0;

        for (DeviceData destData : cable.transferQueue) {
            if(cable.getStoredEnergy() == 0)
                break;;

            int transferred = AbstractEnergyProvider.transferEnergy((ServerLevel) level, destData.devicePos(), level.getBlockState(destData.devicePos()), Math.min(cable.getStoredEnergy(), cable.getMaxOutput((ServerLevel) level, blockPos, blockState)), destData.deviceSide());
            if (transferred > 0) {
                cable.setEnergy(cable.energy - transferred);
            }
            devicesProvided++;
        }

        for (int i = 0; i < devicesProvided; i++) {
            cable.transferQueue.add(cable.transferQueue.removeFirst());
        }

        if (devicesProvided > 0)
            changed = true;


        if (changed)
            cable.setChanged();

    }


    private HashMap<Direction, Boolean> decodeChunkData(String chunkData) {
        HashMap<Direction, Boolean> attachedChunkData = new HashMap<>();

        String[] attachedData = chunkData.split(";");

        for (String data : attachedData) {
            if (data.isEmpty())
                return attachedChunkData;

            Direction side = Direction.valueOf(data.split(":")[0]);
            boolean loaded = Boolean.parseBoolean(data.split(":")[1]);
            attachedChunkData.put(side, loaded);
        }

        return attachedChunkData;
    }

    private String encodeChunkData() {
        StringBuilder chunkData = new StringBuilder();

        this.attachedChunkData.forEach((side, loaded) -> {
            chunkData.append(side.name()).append(":").append(loaded).append(";");
        });

        return chunkData.toString();
    }


    private List<DeviceData> decodeDeviceData(String encodedData) {
        List<DeviceData> blockList = new ArrayList<>();

        String[] listData = encodedData.split(";");
        for (String data : listData) {
            if (data.isEmpty())
                return blockList;

            String[] split = data.split("_");
            String[] coords = split[0].split(":");

            int x = Integer.parseInt(coords[0]);
            int y = Integer.parseInt(coords[1]);
            int z = Integer.parseInt(coords[2]);

            Direction side = Direction.valueOf(split[1]);

            blockList.add(new DeviceData(new BlockPos(x, y, z), side));
        }

        return blockList;
    }

    private String encodeDeviceData(List<DeviceData> list) {
        StringBuilder blockList = new StringBuilder();
        list.forEach(data -> {
            int x = data.devicePos().getX();
            int y = data.devicePos().getY();
            int z = data.devicePos().getZ();

            blockList.append(x).append(":").append(y).append(":").append(z).append("_").append(data.deviceSide().name()).append(";");
        });

        return blockList.toString();
    }


    record DeviceData(BlockPos devicePos, Direction deviceSide) {

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof DeviceData deviceData))
                return false;

            return deviceData.devicePos().equals(this.devicePos) && deviceData.deviceSide().equals(this.deviceSide);
        }
    }
}
