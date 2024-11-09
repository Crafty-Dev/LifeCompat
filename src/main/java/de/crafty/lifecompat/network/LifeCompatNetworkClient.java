package de.crafty.lifecompat.network;

import de.crafty.lifecompat.energy.menu.AbstractPositionedMenu;
import de.crafty.lifecompat.network.payload.LifeCompatSetMenuPositionPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

@Environment(EnvType.CLIENT)
public class LifeCompatNetworkClient {


    public static void registerClientReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(LifeCompatSetMenuPositionPayload.ID, (payload, context) -> {
           context.client().execute(() -> {
               if(context.player().containerMenu instanceof AbstractPositionedMenu menu && menu.containerId == payload.containerId())
                   menu.setMenuPos(payload.pos());
           });
        });
    }
}
