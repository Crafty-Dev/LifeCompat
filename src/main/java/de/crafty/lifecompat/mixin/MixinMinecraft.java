package de.crafty.lifecompat.mixin;

import com.mojang.blaze3d.platform.WindowEventHandler;
import de.crafty.lifecompat.api.event.EventManager;
import de.crafty.lifecompat.events.BaseEvents;
import de.crafty.lifecompat.events.game.GamePostInitEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft extends ReentrantBlockableEventLoop<Runnable> implements WindowEventHandler {
    public MixinMinecraft(String string) {
        super(string);
    }

    @Inject(method = "onGameLoadFinished", at = @At("HEAD"))
    private void hookIntoGameLoad(Minecraft.GameLoadCookie gameLoadCookie, CallbackInfo ci){
        EventManager.callEvent(BaseEvents.GAME_POST_INIT, new GamePostInitEvent.Callback());
    }
}
