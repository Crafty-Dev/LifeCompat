package de.crafty.lifecompat.mixin.world.level.block;

import de.crafty.lifecompat.api.fluid.IFluidProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BubbleColumnBlock;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;


@Mixin(BubbleColumnBlock.class)
public abstract class MixinBubbleColumnBlock extends Block implements BucketPickup, IFluidProvider {
    public MixinBubbleColumnBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Fluid lifeCompat$provideFluid(LevelAccessor level, BlockPos blockPos, BlockState state) {
        return Fluids.WATER;
    }
}
