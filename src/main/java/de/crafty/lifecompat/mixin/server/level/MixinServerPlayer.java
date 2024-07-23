package de.crafty.lifecompat.mixin.server.level;

import com.mojang.authlib.GameProfile;
import de.crafty.lifecompat.api.event.EventManager;
import de.crafty.lifecompat.events.BaseEvents;
import de.crafty.lifecompat.events.item.ItemDropEvent;
import de.crafty.lifecompat.events.player.PlayerDeathEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class MixinServerPlayer extends Player {
    public MixinServerPlayer(Level level, BlockPos blockPos, float f, GameProfile gameProfile) {
        super(level, blockPos, f, gameProfile);
    }

    @Inject(method = "die", at = @At("HEAD"))
    private void hookIntoPlayerDeath(DamageSource damageSource, CallbackInfo ci){
        EventManager.callEvent(BaseEvents.PLAYER_DEATH, new PlayerDeathEvent.Callback((ServerPlayer) (Object) this));
    }

    @Redirect(method = "drop(Z)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private ItemEntity hookIntoItemDropping(ServerPlayer instance, ItemStack itemStack, boolean spawnRandomly, boolean rememberOwner){
        ItemDropEvent.Callback callback = EventManager.callEvent(BaseEvents.ITEM_DROP, new ItemDropEvent.Callback(this, itemStack, spawnRandomly, rememberOwner));
        return instance.drop(callback.isCancelled() ? ItemStack.EMPTY : callback.getStack(), callback.isRandomSpawn(), callback.isRememberOwner());
    }

}
