package de.crafty.lifecompat.mixin.world.item;

import de.crafty.lifecompat.api.fluid.BucketCompatibility;
import de.crafty.lifecompat.api.fluid.IFluidProvider;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BucketItem.class)
public abstract class MixinBucketItem extends Item implements DispensibleContainerItem {

    @Shadow public abstract InteractionResult use(Level level, Player player, InteractionHand interactionHand);

    public MixinBucketItem(Properties properties) {
        super(properties);
    }

    @Inject(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;", ordinal = 0, shift = At.Shift.AFTER), cancellable = true)
    private void injectCompat(Level level, Player player, InteractionHand interactionHand, CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack used = player.getItemInHand(interactionHand);
        BlockHitResult blockHitResult = getPlayerPOVHitResult(
                level, player, ((BucketItem) (Object) this).content == Fluids.EMPTY ? net.minecraft.world.level.ClipContext.Fluid.SOURCE_ONLY : net.minecraft.world.level.ClipContext.Fluid.NONE
        );

        BlockPos blockPos = blockHitResult.getBlockPos();

        BlockState blockState = level.getBlockState(blockPos);

        if (blockState.getBlock() instanceof BucketPickup bucketPickup) {

            ItemStack filled;

            if (blockState.getBlock() instanceof IFluidProvider fluidProvider && BucketCompatibility.getFilledBucket((BucketItem) used.getItem(), fluidProvider.lifeCompat$provideFluid(level, blockPos, blockState)) == ItemStack.EMPTY)
                filled = ItemStack.EMPTY;
            else if (!(blockState.getBlock() instanceof IFluidProvider) && BucketCompatibility.getFilledBucket((BucketItem) used.getItem(), blockState.getBlock()) == ItemStack.EMPTY)
                filled = ItemStack.EMPTY;
            else {
                filled = bucketPickup.pickupBlock(player, level, blockPos, blockState);

                if (filled.getItem() instanceof BucketItem)
                    filled = BucketCompatibility.getFilledBucket((BucketItem) used.getItem(), ((BucketItem) filled.getItem()).content);

                if (filled.getItem() instanceof SolidBucketItem)
                    filled = BucketCompatibility.getFilledBucket((BucketItem) used.getItem(), ((SolidBucketItem) filled.getItem()).getBlock());

            }

            if (!filled.isEmpty()) {
                player.awardStat(Stats.ITEM_USED.get(this));
                bucketPickup.getPickupSound().ifPresent(soundEvent -> player.playSound(soundEvent, 1.0F, 1.0F));
                level.gameEvent(player, GameEvent.FLUID_PICKUP, blockPos);
                ItemStack itemStack3 = ItemUtils.createFilledResult(used, player, filled);
                if (!level.isClientSide) {
                    CriteriaTriggers.FILLED_BUCKET.trigger((ServerPlayer) player, filled);
                }

                cir.setReturnValue(InteractionResult.SUCCESS.heldItemTransformedTo(itemStack3));
            }else
                cir.setReturnValue(InteractionResult.FAIL);

        } else
            cir.setReturnValue(InteractionResult.FAIL);
    }


    @Inject(method = "getEmptySuccessItem", at = @At("HEAD"), cancellable = true)
    private static void injectCompatibility(ItemStack stack, Player player, CallbackInfoReturnable<ItemStack> cir) {
        cir.setReturnValue(!player.hasInfiniteMaterials() ? BucketCompatibility.getEmptyBucket(stack.getItem()) : stack);
    }
}
