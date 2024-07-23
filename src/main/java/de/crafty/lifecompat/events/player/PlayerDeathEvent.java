package de.crafty.lifecompat.events.player;

import de.crafty.lifecompat.LifeCompat;
import de.crafty.lifecompat.api.event.Event;
import de.crafty.lifecompat.api.event.EventCallback;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class PlayerDeathEvent extends Event<PlayerDeathEvent.Callback> {


    public PlayerDeathEvent() {
        super(ResourceLocation.fromNamespaceAndPath(LifeCompat.MODID, "player_death"));
    }

    public record Callback(ServerPlayer player) implements EventCallback {

    }
}
