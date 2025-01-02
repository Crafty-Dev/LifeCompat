package de.crafty.lifecompat.fluid.blockentity;

import de.crafty.lifecompat.api.energy.container.AbstractEnergyContainer;
import de.crafty.lifecompat.api.fluid.logistic.container.IFluidContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public abstract class AbstractFluidEnergyContainerBlockEntity extends AbstractEnergyContainer implements IFluidContainer {

    private Fluid fluid;
    private int volume, fluidCapacity;

    public AbstractFluidEnergyContainerBlockEntity(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState, int energyCapacity, int initialFluidCapacity) {
        super(blockEntityType, blockPos, blockState, energyCapacity);

        this.fluid = Fluids.EMPTY;
        this.volume = 0;
        this.fluidCapacity = initialFluidCapacity;
    }


    @Override
    public int getFluidCapacity() {
        return this.fluidCapacity;
    }

    @Override
    public void setFluidCapacity(int fluidCapacity) {
        this.fluidCapacity = fluidCapacity;
        this.setChanged();
    }

    @Override
    public int getVolume() {
        return this.volume;
    }

    @Override
    public void setVolume(int volume) {
        this.volume = volume;
        this.setChanged();
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
        if(!(this.fluid == Fluids.EMPTY || this.fluid == liquid))
            return 0;


        if(amount <= 0)
            return 0;

        int prevVolume = this.volume;
        this.setVolume(Math.min(this.getFluidCapacity(), this.volume + amount));

        if(prevVolume != this.volume){
            this.setChanged();

            if(prevVolume <= 0)
                this.setFluid(liquid);

            level.gameEvent(GameEvent.BLOCK_CHANGE, this.getBlockPos(), GameEvent.Context.of(this.getBlockState()));
            level.sendBlockUpdated(pos, this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
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

            level.gameEvent(GameEvent.BLOCK_CHANGE, this.getBlockPos(), GameEvent.Context.of(this.getBlockState()));
            level.sendBlockUpdated(pos, this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
        }


        return prevVolume - this.volume;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);

        tag.putString("fluid", BuiltInRegistries.FLUID.getKey(this.fluid).toString());

        tag.putInt("volume", this.getVolume());
        tag.putInt("fluidCapacity", this.getFluidCapacity());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);

        this.fluid = BuiltInRegistries.FLUID.getValue(ResourceLocation.parse(tag.getString("fluid")));

        this.volume = tag.getInt("volume");
        this.fluidCapacity = tag.getInt("fluidCapacity");
    }
}
