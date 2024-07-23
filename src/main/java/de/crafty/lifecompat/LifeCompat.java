package de.crafty.lifecompat;

import de.crafty.lifecompat.api.bucket.BucketCompatibility;
import de.crafty.lifecompat.api.event.EventListener;
import de.crafty.lifecompat.api.event.EventManager;
import de.crafty.lifecompat.events.BaseEvents;
import de.crafty.lifecompat.events.game.GamePostInitEvent;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LifeCompat implements ModInitializer {

	public static final String MODID = "lifecompat";
    public static final Logger LOGGER = LoggerFactory.getLogger("LifeCompat");

	@Override
	public void onInitialize() {
		LOGGER.info("Hello Minecraft!");

		BucketCompatibility.registerFluidBucketGroup(ResourceLocation.withDefaultNamespace("iron"), Items.BUCKET, Items.WATER_BUCKET, Items.LAVA_BUCKET, Items.POWDER_SNOW_BUCKET);

		EventManager.registerListener(BaseEvents.GAME_POST_INIT, new PostInitListener());
	}



	static class PostInitListener implements EventListener<GamePostInitEvent.Callback> {

		@Override
		public void onEventCallback(GamePostInitEvent.Callback callback) {
			BucketCompatibility.bootstrap();
		}
	}
}