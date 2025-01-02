package de.crafty.lifecompat.mixin.server.level;

import de.crafty.lifecompat.api.event.EventManager;
import de.crafty.lifecompat.events.BaseEvents;
import de.crafty.lifecompat.events.block.BlockBreakEvent;
import de.crafty.lifecompat.events.block.BlockInteractEvent;
import de.crafty.lifecompat.events.item.ItemUseEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class MixinServerPlayerGameMode {

    @Shadow @Final protected ServerPlayer player;

    @Shadow protected ServerLevel level;

    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void hookIntoItemUse(ServerPlayer serverPlayer, Level level, ItemStack itemStack, InteractionHand interactionHand, CallbackInfoReturnable<InteractionResult> cir){
        ItemUseEvent.Callback callback = EventManager.callEvent(BaseEvents.ITEM_USE, new ItemUseEvent.Callback(serverPlayer, level, itemStack, interactionHand));
        if(callback.getActionResult().consumesAction())
            cir.setReturnValue(callback.getActionResult());
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void hookIntoBlockInteract(ServerPlayer serverPlayer, Level level, ItemStack itemStack, InteractionHand interactionHand, BlockHitResult blockHitResult, CallbackInfoReturnable<InteractionResult> cir){
        BlockInteractEvent.Callback callback = EventManager.callEvent(BaseEvents.BLOCK_INTERACT, new BlockInteractEvent.Callback(serverPlayer, level, itemStack, interactionHand, blockHitResult));
        if(callback.getActionResult().consumesAction())
            cir.setReturnValue(callback.getActionResult());
    }


    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void hookIntoBlockBreak(BlockPos blockPos, CallbackInfoReturnable<Boolean> cir){
        BlockBreakEvent.Callback callback = EventManager.callEvent(BaseEvents.BLOCK_BREAK, new BlockBreakEvent.Callback(this.player, this.level, blockPos, this.level.getBlockState(blockPos)));
        if(callback.isCancelled())
            cir.setReturnValue(false);
    }
}
