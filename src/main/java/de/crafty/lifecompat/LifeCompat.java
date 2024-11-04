package de.crafty.lifecompat;

import de.crafty.lifecompat.api.fluid.BucketCompatibility;
import de.crafty.lifecompat.api.event.EventListener;
import de.crafty.lifecompat.api.event.EventManager;
import de.crafty.lifecompat.api.fluid.FluidCompatibility;
import de.crafty.lifecompat.events.BaseEvents;
import de.crafty.lifecompat.events.game.GamePostInitEvent;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LifeCompat implements ModInitializer {

	public static final String MODID = "lifecompat";
    public static final Logger LOGGER = LoggerFactory.getLogger("LifeCompat");

	@Override
	public void onInitialize() {
		LOGGER.info("Hello Minecraft!");

		BucketCompatibility.registerBucketGroup(ResourceLocation.withDefaultNamespace("iron"), Items.BUCKET, Items.WATER_BUCKET, Items.LAVA_BUCKET, Items.POWDER_SNOW_BUCKET);

		FluidCompatibility.addCauldronSupport(Fluids.WATER, Blocks.WATER_CAULDRON, SoundEvents.BUCKET_FILL, SoundEvents.BUCKET_EMPTY);
		FluidCompatibility.addCauldronSupport(Fluids.LAVA, Blocks.LAVA_CAULDRON, SoundEvents.BUCKET_FILL_LAVA, SoundEvents.BUCKET_EMPTY_LAVA);

		FluidCompatibility.addCauldronSupport(Blocks.POWDER_SNOW, Blocks.POWDER_SNOW_CAULDRON, SoundEvents.BUCKET_FILL_POWDER_SNOW, SoundEvents.BUCKET_EMPTY_POWDER_SNOW);

		//TODO Entity Buckets?

		EventManager.registerListener(BaseEvents.GAME_POST_INIT, new PostInitListener());
	}



	static class PostInitListener implements EventListener<GamePostInitEvent.Callback> {

		@Override
		public void onEventCallback(GamePostInitEvent.Callback callback) {
			BucketCompatibility.bootstrap();
			FluidCompatibility.bootstrap();
		}
	}
}