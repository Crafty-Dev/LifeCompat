package de.crafty.lifecompat.init;

import de.crafty.lifecompat.LifeCompat;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;

public class LifeCompatModelLayers {

    public static final ModelLayerLocation ENERGY_IO_NONE = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(LifeCompat.MODID, "energyio"), "none");
    public static final ModelLayerLocation ENERGY_IO_INPUT = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(LifeCompat.MODID, "energyio"), "input");
    public static final ModelLayerLocation ENERGY_IO_OUTPUT = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(LifeCompat.MODID, "energyio"), "output");
    public static final ModelLayerLocation ENERGY_IO_IO = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(LifeCompat.MODID, "energyio"), "io");
}
