package de.crafty.lifecompat.events.block;

import de.crafty.lifecompat.LifeCompat;
import de.crafty.lifecompat.api.event.Event;
import de.crafty.lifecompat.api.event.EventCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public class BlockChangeEvent extends Event<BlockChangeEvent.Callback> {


    public BlockChangeEvent() {
        super(ResourceLocation.fromNamespaceAndPath(LifeCompat.MODID, "block_change"));
    }

    public record Callback(ServerLevel level, BlockPos pos, BlockState oldState, BlockState newState) implements EventCallback {

    }
}
