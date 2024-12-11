package de.crafty.lifecompat.fluid.blockentity;

import de.crafty.lifecompat.api.fluid.logistic.container.IFluidContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractFluidContainerBlockEntity extends BlockEntity implements IFluidContainer {

    private Fluid fluid;
    private int volume, capacity;


    public AbstractFluidContainerBlockEntity(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState, int initialCapacity) {
        super(blockEntityType, blockPos, blockState);

        this.fluid = Fluids.EMPTY;
        this.volume = 0;
        this.capacity = initialCapacity;
    }

    @Override
    public void setCapacity(int capacity) {
        this.capacity = capacity;
        this.setChanged();
    }

    @Override
    public int getCapacity() {
        return this.capacity;
    }

    @Override
    public void setVolume(int volume) {
        this.volume = volume;
        this.setChanged();
    }

    @Override
    public int getVolume() {
        return this.volume;
    }

    @Override
    public Fluid getFluid() {
        return this.fluid;
    }

    @Override
    public void setFluid(Fluid fluid) {
        this.fluid = fluid;
        if(fluid == Fluids.EMPTY)
            this.volume = 0;
        this.setChanged();
    }

    @Override
    public int fillWithLiquid(ServerLevel level, BlockPos pos, BlockState state, Fluid liquid, int amount) {
        if(amount <= 0)
            return 0;

        int prevVolume = this.volume;
        this.setVolume(Math.min(this.getCapacity(), this.volume + amount));

        if(prevVolume != this.volume){
            this.setChanged();

            if(prevVolume <= 0)
                this.setFluid(liquid);

            level.sendBlockUpdated(pos, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }

        return this.volume - prevVolume;
    }

    @Override
    public int drainLiquidFrom(ServerLevel level, BlockPos pos, BlockState state, Fluid liquid, int amount) {
        if(liquid != this.getFluid())
            return 0;


        int prevVolume = this.volume;
        this.volume = Math.max(0, this.volume - amount);

        if(prevVolume != this.volume){
            if(this.volume == 0)
                this.setFluid(Fluids.EMPTY);

            level.sendBlockUpdated(pos, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }


        return prevVolume - this.volume;
    }


    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {

        tag.putString("fluid", BuiltInRegistries.FLUID.getKey(this.fluid).toString());

        tag.putInt("volume", this.getVolume());
        tag.putInt("capacity", this.getCapacity());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {

        this.fluid = BuiltInRegistries.FLUID.get(ResourceLocation.parse(tag.getString("fluid")));

        this.volume = tag.getInt("volume");
        this.capacity = tag.getInt("capacity");
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = super.getUpdateTag(provider);
        this.saveAdditional(tag, provider);
        return tag;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
