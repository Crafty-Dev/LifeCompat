package de.crafty.lifecompat.mixin.world.entity;

import de.crafty.lifecompat.api.event.EventManager;
import de.crafty.lifecompat.events.BaseEvents;
import de.crafty.lifecompat.events.item.ItemDropEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Player.class)
public abstract class MixinPlayer extends LivingEntity {
    @Shadow @Nullable public abstract ItemEntity drop(ItemStack itemStack, boolean bl, boolean bl2);

    protected MixinPlayer(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Redirect(method = "drop(Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/entity/item/ItemEntity;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private ItemEntity hookIntoItemDrop(Player instance, ItemStack itemStack, boolean spawnRandomly, boolean rememberOwner){
        ItemDropEvent.Callback callback = EventManager.callEvent(BaseEvents.ITEM_DROP, new ItemDropEvent.Callback((Player) (Object)this, itemStack, spawnRandomly, rememberOwner));
        return this.drop(callback.isCancelled() ? ItemStack.EMPTY : callback.getStack(), callback.isRandomSpawn(), callback.isRememberOwner());
    }
}
