package de.crafty.lifecompat.events.blockentity;

import de.crafty.lifecompat.LifeCompat;
import de.crafty.lifecompat.api.event.Event;
import de.crafty.lifecompat.api.event.EventCallback;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

public class BlockEntityLoadEvent extends Event<BlockEntityLoadEvent.Callback> {


    public BlockEntityLoadEvent() {
        super(ResourceLocation.fromNamespaceAndPath(LifeCompat.MODID, "blockentity_load"));
    }

    public record Callback(ServerLevel level, BlockEntity blockEntity) implements EventCallback {

    }
}
