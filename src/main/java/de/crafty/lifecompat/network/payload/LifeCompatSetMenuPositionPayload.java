package de.crafty.lifecompat.network.payload;

import de.crafty.lifecompat.network.LifeCompatNetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record LifeCompatSetMenuPositionPayload(BlockPos pos, int containerId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<LifeCompatSetMenuPositionPayload> ID = new CustomPacketPayload.Type<LifeCompatSetMenuPositionPayload>(LifeCompatNetworkManager.SET_MENU_POSITION_PACKET);
    public static final StreamCodec<RegistryFriendlyByteBuf, LifeCompatSetMenuPositionPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            LifeCompatSetMenuPositionPayload::pos,
            ByteBufCodecs.INT,
            LifeCompatSetMenuPositionPayload::containerId,
            LifeCompatSetMenuPositionPayload::new
    );


    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
