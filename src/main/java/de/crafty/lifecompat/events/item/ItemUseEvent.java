package de.crafty.lifecompat.events.item;

import de.crafty.lifecompat.LifeCompat;
import de.crafty.lifecompat.api.event.Event;
import de.crafty.lifecompat.api.event.EventCallback;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ItemUseEvent extends Event<ItemUseEvent.Callback> {


    public ItemUseEvent() {
        super(ResourceLocation.fromNamespaceAndPath(LifeCompat.MODID, "item_use"));
    }

    public static class Callback implements EventCallback {

        private final Player player;
        private final Level level;
        private final ItemStack stack;
        private final InteractionHand hand;
        private InteractionResult actionResult = InteractionResult.PASS;


        public Callback(Player player, Level level, ItemStack itemStack, InteractionHand interactionHand){
            this.player = player;
            this.level = level;
            this.stack = itemStack;
            this.hand = interactionHand;
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
