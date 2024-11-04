package de.crafty.lifecompat.mixin.world.level.block;

import de.crafty.lifecompat.api.fluid.IFluidProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;


@Mixin(LiquidBlock.class)
public abstract class MixinLiquidBlock extends Block implements BucketPickup, IFluidProvider {
    @Shadow @Final protected FlowingFluid fluid;

    public MixinLiquidBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Fluid lifeCompat$provideFluid(LevelAccessor level, BlockPos blockPos, BlockState state) {
        return this.fluid;
    }
}
