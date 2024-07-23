package de.crafty.lifecompat.events.block;

import de.crafty.lifecompat.LifeCompat;
import de.crafty.lifecompat.api.event.CancellableEventCallback;
import de.crafty.lifecompat.api.event.Event;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class BlockBreakEvent extends Event<BlockBreakEvent.Callback> {


    public BlockBreakEvent() {
        super(ResourceLocation.fromNamespaceAndPath(LifeCompat.MODID, "block_break"));
    }

    public static class Callback extends CancellableEventCallback {

        private final Player player;
        private final Level level;
        private final BlockPos pos;
        private final BlockState state;

        public Callback(Player player, Level level, BlockPos pos, BlockState state){
            this.player = player;
            this.level = level;
            this.pos = pos;
            this.state = state;
        }

        public Player getPlayer() {
            return this.player;
        }

        public Level getLevel() {
            return this.level;
        }

        public BlockPos getBlockPos() {
            return this.pos;
        }

        public BlockState getBlockState() {
            return this.state;
        }
    }
}
