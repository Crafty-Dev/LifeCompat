package de.crafty.lifecompat.util;

import de.crafty.lifecompat.network.LifeCompatNetworkServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.entity.BlockEntity;

public class LifeCompatMenuHelper {


    public static <T extends BlockEntity> void openMenuAndSendPosition(ServerPlayer player, MenuProvider menuProvider){
        player.openMenu(menuProvider);
        if(menuProvider instanceof BlockEntity be){
            LifeCompatNetworkServer.sendMenuPosition(player, be.getBlockPos(), player.containerMenu.containerId);
        }
    }

}
