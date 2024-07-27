package de.crafty.lifecompat.mixin.world.item;

import de.crafty.lifecompat.api.bucket.BucketCompatibility;
import de.crafty.lifecompat.api.bucket.IFluidProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BucketItem.class)
public abstract class MixinBucketItem extends Item implements DispensibleContainerItem {

    public MixinBucketItem(Properties properties) {
        super(properties);
    }

    @Redirect(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/BucketPickup;pickupBlock(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack injectCompatibility(BucketPickup instance, Player playerEntity, LevelAccessor worldAccess, BlockPos blockPos, BlockState state){
        ItemStack used = playerEntity.getItemInHand(playerEntity.getUsedItemHand());
        if(state.getBlock() instanceof IFluidProvider fluidProvider && BucketCompatibility.getFilledBucket((BucketItem) used.getItem(), fluidProvider.lifeCompat$provideFluid(worldAccess, blockPos, state)) == ItemStack.EMPTY)
            return ItemStack.EMPTY;

        if(!(state.getBlock() instanceof IFluidProvider) && BucketCompatibility.getFilledBucket((BucketItem) used.getItem(), state.getBlock()) == ItemStack.EMPTY)
            return ItemStack.EMPTY;

        ItemStack stack = instance.pickupBlock(playerEntity, worldAccess, blockPos, state);

        if(stack.getItem() instanceof BucketItem)
            return BucketCompatibility.getFilledBucket((BucketItem) used.getItem(), ((BucketItem) stack.getItem()).content);
        if(stack.getItem() instanceof SolidBucketItem)
            return BucketCompatibility.getFilledBucket((BucketItem) used.getItem(), ((SolidBucketItem) stack.getItem()).getBlock());

        return stack;
    }


    @Inject(method = "getEmptySuccessItem", at = @At("HEAD"), cancellable = true)
    private static void injectCompatibility(ItemStack stack, Player player, CallbackInfoReturnable<ItemStack> cir){
        cir.setReturnValue(!player.hasInfiniteMaterials() ? BucketCompatibility.getEmptyBucket(player.getItemInHand(player.getUsedItemHand()).getItem()) : stack);
    }
}
