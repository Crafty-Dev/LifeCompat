package de.crafty.lifecompat.mixin.world.entity.item;


import de.crafty.lifecompat.api.event.EventManager;
import de.crafty.lifecompat.events.BaseEvents;
import de.crafty.lifecompat.events.item.ItemTickEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class MixinItemEntity extends Entity implements TraceableEntity {

    public MixinItemEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }


    @Inject(method = "tick", at = @At("RETURN"))
    private void hookIntoItemTicking(CallbackInfo ci) {
        EventManager.callEvent(BaseEvents.ITEM_TICK, new ItemTickEvent.Callback((ItemEntity) (Object) this, this.level(), this.isRemoved()));
    }
}
