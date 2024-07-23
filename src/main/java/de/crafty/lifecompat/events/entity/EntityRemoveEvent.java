package de.crafty.lifecompat.events.entity;

import de.crafty.lifecompat.LifeCompat;
import de.crafty.lifecompat.api.event.Event;
import de.crafty.lifecompat.api.event.EventCallback;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public class EntityRemoveEvent extends Event<EntityRemoveEvent.Callback> {


    public EntityRemoveEvent() {
        super(ResourceLocation.fromNamespaceAndPath(LifeCompat.MODID, "entity_remove"));
    }

    public record Callback(Entity entity, Level level, Entity.RemovalReason removalReason) implements EventCallback {

    }
}
