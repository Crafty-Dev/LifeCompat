package de.crafty.lifecompat.network;

import de.crafty.lifecompat.LifeCompat;
import de.crafty.lifecompat.network.payload.LifeCompatSetMenuPositionPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.resources.ResourceLocation;

public class LifeCompatNetworkManager {

    public static final ResourceLocation SET_MENU_POSITION_PACKET = ResourceLocation.fromNamespaceAndPath(LifeCompat.MODID, "set_menu_position");

    public static void registerNetworkPackets(){

        PayloadTypeRegistry.playS2C().register(LifeCompatSetMenuPositionPayload.ID, LifeCompatSetMenuPositionPayload.CODEC);

    }

}
