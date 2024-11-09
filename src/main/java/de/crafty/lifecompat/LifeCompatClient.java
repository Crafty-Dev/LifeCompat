package de.crafty.lifecompat;

import de.crafty.lifecompat.energy.blockentity.renderer.SimpleEnergyBlockRenderer;
import de.crafty.lifecompat.init.LifeCompatModelLayers;
import de.crafty.lifecompat.network.LifeCompatNetworkClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;

public class LifeCompatClient implements ClientModInitializer {


    @Override
    public void onInitializeClient() {

        EntityModelLayerRegistry.registerModelLayer(LifeCompatModelLayers.ENERGY_IO_NONE, SimpleEnergyBlockRenderer::createIONoneLayer);
        EntityModelLayerRegistry.registerModelLayer(LifeCompatModelLayers.ENERGY_IO_INPUT, SimpleEnergyBlockRenderer::createInputLayer);
        EntityModelLayerRegistry.registerModelLayer(LifeCompatModelLayers.ENERGY_IO_OUTPUT, SimpleEnergyBlockRenderer::createOutputLayer);
        EntityModelLayerRegistry.registerModelLayer(LifeCompatModelLayers.ENERGY_IO_IO, SimpleEnergyBlockRenderer::createIOLayer);


        LifeCompatNetworkClient.registerClientReceivers();
    }

}
