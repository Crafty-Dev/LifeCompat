package de.crafty.lifecompat.mixin.world.level.block;

import de.crafty.lifecompat.api.fluid.IFluidProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SimpleWaterloggedBlock.class)
public interface MixinSimpleWaterloggedBlock extends BucketPickup, LiquidBlockContainer, IFluidProvider {


    @Override
    default Fluid lifeCompat$provideFluid(LevelAccessor level, BlockPos blockPos, BlockState state) {
        return state.getValue(BlockStateProperties.WATERLOGGED) ? Fluids.WATER : Fluids.EMPTY;
    }
}
