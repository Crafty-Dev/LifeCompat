package de.crafty.lifecompat.events.game;

import de.crafty.lifecompat.LifeCompat;
import de.crafty.lifecompat.api.event.Event;
import de.crafty.lifecompat.api.event.EventCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

public class GamePostInitEvent extends Event<GamePostInitEvent.Callback> {


    public GamePostInitEvent() {
        super(ResourceLocation.fromNamespaceAndPath(LifeCompat.MODID, "game_post_init"));
    }

    public record Callback() implements EventCallback {

    }
}
