package de.crafty.lifecompat.events.world;

import de.crafty.lifecompat.LifeCompat;
import de.crafty.lifecompat.api.event.Event;
import de.crafty.lifecompat.api.event.EventCallback;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ServerLevelData;

public class WorldStartupEvent extends Event<WorldStartupEvent.Callback> {


    public WorldStartupEvent() {
        super(ResourceLocation.fromNamespaceAndPath(LifeCompat.MODID, "world_startup"));
    }

    public record Callback(ServerLevel level, ServerLevelData levelData) implements EventCallback {

    }
}
