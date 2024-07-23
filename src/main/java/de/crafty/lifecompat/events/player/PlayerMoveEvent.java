package de.crafty.lifecompat.events.player;

import de.crafty.lifecompat.LifeCompat;
import de.crafty.lifecompat.api.event.Event;
import de.crafty.lifecompat.api.event.EventCallback;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class PlayerMoveEvent extends Event<PlayerMoveEvent.Callback> {


    public PlayerMoveEvent() {
        super(ResourceLocation.fromNamespaceAndPath(LifeCompat.MODID, "player_move"));
    }

    public record Callback(ServerPlayer player, ServerLevel level, Vec3 prevPos, Vec3 pos) implements EventCallback {

    }
}
