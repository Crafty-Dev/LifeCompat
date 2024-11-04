package de.crafty.lifecompat.mixin.server;

import de.crafty.lifecompat.api.event.EventManager;
import de.crafty.lifecompat.events.BaseEvents;
import de.crafty.lifecompat.events.game.GamePostInitEvent;
import de.crafty.lifecompat.events.world.WorldStartupEvent;
import net.minecraft.commands.CommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerInfo;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;
import net.minecraft.world.level.chunk.storage.ChunkIOErrorReporter;
import net.minecraft.world.level.storage.ServerLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class   MixinMinecraftServer extends ReentrantBlockableEventLoop<TickTask> implements ServerInfo, ChunkIOErrorReporter, CommandSource, AutoCloseable {
    @Shadow public abstract boolean isDedicatedServer();

    public MixinMinecraftServer(String string) {
        super(string);
    }


    @Inject(method = "setInitialSpawn", at = @At("RETURN"))
    private static void hookIntoSpawnGen(ServerLevel world, ServerLevelData levelData, boolean bonusChest, boolean debugWorld, CallbackInfo ci){
        EventManager.callEvent(BaseEvents.WORLD_STARTUP, new WorldStartupEvent.Callback(world, levelData));
    }

    @Inject(method = "runServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;buildServerStatus()Lnet/minecraft/network/protocol/status/ServerStatus;", shift = At.Shift.AFTER))
    private void hookIntoGameLoad(CallbackInfo ci){
        if(this.isDedicatedServer())
            EventManager.callEvent(BaseEvents.GAME_POST_INIT, new GamePostInitEvent.Callback());
    }
}
