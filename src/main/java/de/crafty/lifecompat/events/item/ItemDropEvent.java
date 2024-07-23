package de.crafty.lifecompat.events.item;

import de.crafty.lifecompat.LifeCompat;
import de.crafty.lifecompat.api.event.CancellableEventCallback;
import de.crafty.lifecompat.api.event.Event;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ItemDropEvent extends Event<ItemDropEvent.Callback> {


    public ItemDropEvent() {
        super(ResourceLocation.fromNamespaceAndPath(LifeCompat.MODID, "item_drop"));
    }

    public static class Callback extends CancellableEventCallback {

        private ItemStack stack;
        private boolean randomSpawn, rememberOwner;
        private final Player player;

        public Callback(Player player, ItemStack itemStack, boolean randomSpawn, boolean rememberOwner) {
            this.player = player;
            this.stack = itemStack;
            this.randomSpawn = randomSpawn;
            this.rememberOwner = rememberOwner;
        }

        public ItemStack getStack() {
            return this.stack;
        }

        public void setStack(ItemStack stack) {
            this.stack = stack;
        }

        public boolean isRandomSpawn() {
            return this.randomSpawn;
        }

        public void setRandomSpawn(boolean randomSpawn) {
            this.randomSpawn = randomSpawn;
        }

        public boolean isRememberOwner() {
            return this.rememberOwner;
        }

        public void setRememberOwner(boolean rememberOwner) {
            this.rememberOwner = rememberOwner;
        }

        public Player getPlayer() {
            return this.player;
        }
    }
}
