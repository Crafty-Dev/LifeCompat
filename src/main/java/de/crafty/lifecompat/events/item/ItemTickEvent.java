package de.crafty.lifecompat.events.item;

import de.crafty.lifecompat.LifeCompat;
import de.crafty.lifecompat.api.event.Event;
import de.crafty.lifecompat.api.event.EventCallback;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;

public class ItemTickEvent extends Event<ItemTickEvent.Callback> {


    public ItemTickEvent() {
        super(ResourceLocation.fromNamespaceAndPath(LifeCompat.MODID, "item_tick"));
    }

    public record Callback(ItemEntity itemEntity, Level level, boolean isRemoved) implements EventCallback {

    }
}
