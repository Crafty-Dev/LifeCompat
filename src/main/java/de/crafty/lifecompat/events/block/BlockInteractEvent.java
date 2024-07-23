package de.crafty.lifecompat.events.block;

import de.crafty.lifecompat.LifeCompat;
import de.crafty.lifecompat.api.event.Event;
import de.crafty.lifecompat.api.event.EventCallback;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

public class BlockInteractEvent extends Event<BlockInteractEvent.Callback> {


    public BlockInteractEvent() {
        super(ResourceLocation.fromNamespaceAndPath(LifeCompat.MODID, "block_interact"));
    }

    public static class Callback implements EventCallback {

        private final Player player;
        private final Level level;
        private final ItemStack stack;
        private final InteractionHand hand;
        private final BlockHitResult blockHitResult;
        private InteractionResult actionResult = InteractionResult.PASS;


        public Callback(Player player, Level level, ItemStack itemStack, InteractionHand interactionHand, BlockHitResult blockHitResult){
            this.player = player;
            this.level = level;
            this.stack = itemStack;
            this.hand = interactionHand;
            this.blockHitResult = blockHitResult;
        }

        public Player getPlayer() {
            return this.player;
        }

        public Level getLevel() {
            return this.level;
        }

        public ItemStack getStack() {
            return this.stack;
        }

        public InteractionHand getHand() {
            return this.hand;
        }

        public BlockHitResult getBlockHitResult() {
            return this.blockHitResult;
        }

        public void setActionResult(InteractionResult actionResult){
            this.actionResult = actionResult;
        }

        public InteractionResult getActionResult(){
            return this.actionResult;
        }

        @Override
        public boolean shouldStopQueue() {
            return this.getActionResult() != InteractionResult.PASS;
        }
    }
}
