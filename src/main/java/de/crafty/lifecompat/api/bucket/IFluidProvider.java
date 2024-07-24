package de.crafty.lifecompat.api.bucket;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public interface IFluidProvider {



    //Bucket Compat method
    //Return the fluid that your Block contains
    //You can return different Fluids based on e.g. BlockEntities
    default Fluid lifeCompat$provideFluid(LevelAccessor level, BlockPos blockPos, BlockState state){
        return Fluids.EMPTY;
    };
}
