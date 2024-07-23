package de.crafty.lifecompat.mixin.world.item;

import de.crafty.lifecompat.api.bucket.BucketCompatibility;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DispensibleContainerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SolidBucketItem;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SolidBucketItem.class)
public abstract class MixinSolidBucketItem extends BlockItem implements DispensibleContainerItem {
    public MixinSolidBucketItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Redirect(method = "useOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;setItemInHand(Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/ItemStack;)V"))
    private void injectCompatibility(Player instance, InteractionHand hand, ItemStack stack){
        instance.setItemInHand(hand, BucketCompatibility.getEmptyBucket(this));
    }
}
