package de.crafty.lifecompat.util;

import net.minecraft.util.Mth;

public class EnergyUnitConverter {

    public static int kiloVP(float amount){
        return Mth.floor(amount * 1000);
    }

    public static float megaVP(float amount){
        return Mth.floor(amount * kiloVP(1000));
    }


    public static String format(int energy){
        if(energy / 1000000.0F >= 1)
            return String.format("%.1fMVP", energy / 1000000.0F);
        if(energy / 1000F >= 1)
            return String.format("%.1fkVP", energy / 1000F);

        return String.format(energy + "VP");
    }

    public static String formatRaw(int energy){
        if(energy / 1000000.0F >= 1)
            return String.format("%.1fM", energy / 1000000.0F);
        if(energy / 1000F >= 1)
            return String.format("%.1fk", energy / 1000.0F);

        return String.valueOf(energy);
    }
}
