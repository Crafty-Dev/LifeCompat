package de.crafty.lifecompat.mixin.server.level;

import de.crafty.lifecompat.api.event.EventManager;
import de.crafty.lifecompat.events.BaseEvents;
import de.crafty.lifecompat.events.block.BlockChangeEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;


@Mixin(ServerLevel.class)
public abstract class MixinServerLevel extends Level implements WorldGenLevel {
    protected MixinServerLevel(WritableLevelData writableLevelData, ResourceKey<Level> resourceKey, RegistryAccess registryAccess, Holder<DimensionType> holder, Supplier<ProfilerFiller> supplier, boolean bl, boolean bl2, long l, int i) {
        super(writableLevelData, resourceKey, registryAccess, holder, supplier, bl, bl2, l, i);
    }

    @Inject(method = "sendBlockUpdated", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerChunkCache;blockChanged(Lnet/minecraft/core/BlockPos;)V", shift = At.Shift.AFTER))
    private void hookIntoBlockChange(BlockPos blockPos, BlockState oldState, BlockState newState, int i, CallbackInfo ci){
        EventManager.callEvent(BaseEvents.BLOCK_CHANGE, new BlockChangeEvent.Callback(this.getLevel(), blockPos, oldState, newState));
    }
}
