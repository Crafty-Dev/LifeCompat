package de.crafty.lifecompat.util;

import net.minecraft.util.Mth;

public class FluidUnitConverter {


    public static int buckets(float amount){
        return Mth.floor(amount * 1000);
    }

}
