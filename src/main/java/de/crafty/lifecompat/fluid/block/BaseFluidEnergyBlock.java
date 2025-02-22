package de.crafty.lifecompat.fluid.block;

import de.crafty.lifecompat.api.fluid.BucketCompatibility;
import de.crafty.lifecompat.api.fluid.logistic.container.IFluidContainer;
import de.crafty.lifecompat.api.fluid.logistic.container.IFluidContainerBlock;
import de.crafty.lifecompat.energy.block.BaseEnergyBlock;
import de.crafty.lifecompat.util.FluidUnitConverter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;
import java.util.Optional;

public abstract class BaseFluidEnergyBlock extends BaseEnergyBlock implements IFluidContainerBlock {


    protected BaseFluidEnergyBlock(Properties properties, Type energyBlockType, int capacity) {
        super(properties, energyBlockType, capacity);
    }


    public abstract List<Direction> getFluidCompatableSides();

    public abstract boolean allowBucketFill(BlockState state);

    public abstract boolean allowBucketEmpty(BlockState state);


    @Override
    public boolean canConnectPipe(BlockState state, Direction side) {
        return this.getFluidCompatableSides().contains(IFluidContainerBlock.resolveFacingRelatedSide(state, side));
    }

    @Override
    protected RenderShape getRenderShape(BlockState blockState) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult) {
        if (!(stack.getItem() instanceof BucketItem bucket) || !(level.getBlockEntity(blockPos) instanceof IFluidContainer fluidContainer))
            return InteractionResult.TRY_WITH_EMPTY_HAND;

        if (bucket.content == Fluids.EMPTY) {
            ItemStack filled = BucketCompatibility.getFilledBucket(bucket, fluidContainer.getFluid());

            if (this.allowBucketFill(blockState)) {
                if (!filled.isEmpty() && fluidContainer.getVolume() >= FluidUnitConverter.buckets(1.0F)) {
                    fluidContainer.getFluid().getPickupSound().ifPresent(soundEvent -> player.playSound(soundEvent, 1.0F, 1.0F));

                    if (!level.isClientSide()){
                        fluidContainer.drainLiquidFrom((ServerLevel) level, blockPos, blockState, fluidContainer.getFluid(), FluidUnitConverter.buckets(1.0F));
                        player.setItemInHand(interactionHand, ItemUtils.createFilledResult(stack, player, filled));
                    }
                }
            }

            return InteractionResult.SUCCESS;
        }

        if (bucket.content == fluidContainer.getFluid() || fluidContainer.getFluid() == Fluids.EMPTY) {
            ItemStack emptyBucket = BucketCompatibility.getEmptyBucket(bucket);
            if (emptyBucket.isEmpty())
                return InteractionResult.TRY_WITH_EMPTY_HAND;

            if (this.allowBucketEmpty(blockState) && fluidContainer.getFluidCapacity() > fluidContainer.getVolume()) {

                this.getBucketEmptySound(bucket.content, level, blockPos, blockState).ifPresent(soundEvent -> player.playSound(soundEvent, 1.0F, 1.0F));

                if (!level.isClientSide()){
                    fluidContainer.fillWithLiquid((ServerLevel) level, blockPos, blockState, bucket.content, FluidUnitConverter.buckets(1.0F));
                    player.setItemInHand(interactionHand, ItemUtils.createFilledResult(stack, player, emptyBucket));
                }


            }

            return InteractionResult.SUCCESS;
        }


        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }


    public abstract Optional<SoundEvent> getBucketEmptySound(Fluid fluid, Level level, BlockPos pos, BlockState state);
}
