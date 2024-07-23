package de.crafty.lifecompat.events.player;

import de.crafty.lifecompat.LifeCompat;
import de.crafty.lifecompat.api.event.Event;
import de.crafty.lifecompat.api.event.EventCallback;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class PlayerToggleSneakEvent extends Event<PlayerToggleSneakEvent.Callback> {


    public PlayerToggleSneakEvent() {
        super(ResourceLocation.fromNamespaceAndPath(LifeCompat.MODID, "player_toggle_sneak"));
    }

    public record Callback(ServerPlayer player, ServerLevel level) implements EventCallback {

    }

}
