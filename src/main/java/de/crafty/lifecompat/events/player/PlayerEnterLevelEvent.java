package de.crafty.lifecompat.events.player;

import de.crafty.lifecompat.LifeCompat;
import de.crafty.lifecompat.api.event.Event;
import de.crafty.lifecompat.api.event.EventCallback;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class PlayerEnterLevelEvent extends Event<PlayerEnterLevelEvent.Callback> {


    public PlayerEnterLevelEvent() {
        super(ResourceLocation.fromNamespaceAndPath(LifeCompat.MODID, "player_enter_level"));
    }

    public record Callback(ServerPlayer player, ServerLevel level) implements EventCallback {

    }
}
