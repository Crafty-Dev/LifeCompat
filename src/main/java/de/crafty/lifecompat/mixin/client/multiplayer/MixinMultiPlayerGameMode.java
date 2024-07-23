package de.crafty.lifecompat.mixin.client.multiplayer;

import de.crafty.lifecompat.api.event.EventManager;
import de.crafty.lifecompat.events.BaseEvents;
import de.crafty.lifecompat.events.block.BlockBreakEvent;
import de.crafty.lifecompat.events.block.BlockInteractEvent;
import de.crafty.lifecompat.events.item.ItemUseEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class MixinMultiPlayerGameMode {

    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void hookIntoItemUse(Player player, InteractionHand interactionHand, CallbackInfoReturnable<InteractionResult> cir) {
        ItemUseEvent.Callback callback = EventManager.callEvent(BaseEvents.ITEM_USE, new ItemUseEvent.Callback(player, player.level(), player.getItemInHand(interactionHand), interactionHand));
        if (callback.getActionResult() != InteractionResult.PASS)
            cir.setReturnValue(callback.getActionResult());
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void hookIntoBlockInteract(LocalPlayer player, InteractionHand interactionHand, BlockHitResult blockHitResult, CallbackInfoReturnable<InteractionResult> cir) {
        BlockInteractEvent.Callback callback = EventManager.callEvent(BaseEvents.BLOCK_INTERACT, new BlockInteractEvent.Callback(player, player.level(), player.getItemInHand(interactionHand), interactionHand, blockHitResult));
        if (callback.getActionResult() != InteractionResult.PASS)
            cir.setReturnValue(callback.getActionResult());
    }

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void hookIntoBlockBreak(BlockPos blockPos, CallbackInfoReturnable<Boolean> cir) {
        if (this.minecraft.level == null)
            return;

        BlockBreakEvent.Callback callback = EventManager.callEvent(BaseEvents.BLOCK_BREAK, new BlockBreakEvent.Callback(this.minecraft.player, this.minecraft.level, blockPos, this.minecraft.level.getBlockState(blockPos)));
        if (callback.isCancelled())
            cir.setReturnValue(false);
    }
}
