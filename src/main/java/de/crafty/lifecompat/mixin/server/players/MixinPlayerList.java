package de.crafty.lifecompat.mixin.server.players;

import de.crafty.lifecompat.api.event.EventManager;
import de.crafty.lifecompat.events.BaseEvents;
import de.crafty.lifecompat.events.player.PlayerEnterLevelEvent;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public abstract class MixinPlayerList {

    @Inject(method = "placeNewPlayer", at = @At("RETURN"))
    private void hookIntoPlayerJoin(Connection connection, ServerPlayer player, CommonListenerCookie clientData, CallbackInfo ci){
        EventManager.callEvent(BaseEvents.PLAYER_ENTER_LEVEL, new PlayerEnterLevelEvent.Callback(player, player.serverLevel()));
    }
}
