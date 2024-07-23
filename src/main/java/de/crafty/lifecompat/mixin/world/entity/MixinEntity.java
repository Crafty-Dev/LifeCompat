package de.crafty.lifecompat.mixin.world.entity;

import de.crafty.lifecompat.api.event.EventManager;
import de.crafty.lifecompat.events.BaseEvents;
import de.crafty.lifecompat.events.entity.EntityRemoveEvent;
import de.crafty.lifecompat.events.player.PlayerMoveEvent;
import de.crafty.lifecompat.events.player.PlayerToggleSneakEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class MixinEntity {

    @Shadow @Final private static int FLAG_SHIFT_KEY_DOWN;

    @Shadow protected abstract void setSharedFlag(int i, boolean bl);

    @Shadow private Level level;

    @Inject(method = "setShiftKeyDown", at = @At("HEAD"), cancellable = true)
    private void hookIntoSneaking(boolean sneaking, CallbackInfo ci) {
        if (!((Object) this instanceof ServerPlayer player))
            return;

        if (!player.isShiftKeyDown() && !sneaking || player.isShiftKeyDown() && sneaking)
            return;

        this.setSharedFlag(FLAG_SHIFT_KEY_DOWN, sneaking);
        EventManager.callEvent(BaseEvents.PLAYER_TOGGLE_SNEAK, new PlayerToggleSneakEvent.Callback(player, player.serverLevel()));
        ci.cancel();
    }

    @Redirect(method = "setPos(DDD)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setPosRaw(DDD)V"))
    private void hookIntoMovement(Entity instance, double x, double y, double z){
        if (!(instance instanceof ServerPlayer player)){
            instance.setPosRaw(x, y, z);
            return;
        }

        player.setPosRaw(x, y, z);
        EventManager.callEvent(BaseEvents.PLAYER_MOVE, new PlayerMoveEvent.Callback(player, player.serverLevel(), new Vec3(player.xo, player.yo, player.zo), player.position()));
    }

    @Inject(method = "remove", at = @At("HEAD"))
    private void hookIntoEntityRemoving(Entity.RemovalReason reason, CallbackInfo ci){
        EventManager.callEvent(BaseEvents.ENTITY_REMOVE, new EntityRemoveEvent.Callback((Entity) (Object) this, this.level, reason));
    }
}
