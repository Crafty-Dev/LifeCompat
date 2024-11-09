package de.crafty.lifecompat.network;

import de.crafty.lifecompat.network.payload.LifeCompatSetMenuPositionPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

public class LifeCompatNetworkServer {

    public static void registerServerReceivers(){

    }


    public static void sendMenuPosition(ServerPlayer player, BlockPos pos, int containerid){
        ServerPlayNetworking.send(player, new LifeCompatSetMenuPositionPayload(pos, containerid));
    }
}
