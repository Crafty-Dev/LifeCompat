package de.crafty.lifecompat.mixin.world.item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SolidBucketItem.class)
public abstract class MixinSolidBucketItem extends BlockItem implements DispensibleContainerItem {

    public MixinSolidBucketItem(Block block, Properties properties) {
        super(block, properties);
    }


    @Redirect(method = "useOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/BucketItem;getEmptySuccessItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack injectCompat(ItemStack itemStack, Player player){
        return BucketItem.getEmptySuccessItem(new ItemStack(this), player);
    }
}
