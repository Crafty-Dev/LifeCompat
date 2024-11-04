package de.crafty.lifecompat.api.fluid;

import de.crafty.lifecompat.LifeCompat;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SolidBucketItem;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.*;

public class FluidCompatibility {

    /**
     * Map of fluids/blocks that can be stored in a cauldron
     */
    private static final LinkedHashMap<Fluid, CauldronInfo> CAULDRON_SUPPORT = new LinkedHashMap<>();
    private static final LinkedHashMap<Block, CauldronInfo> SOLID_CAULDRON_SUPPORT = new LinkedHashMap<>();
    /**
     * Static Map for more efficient fluid/block query
     */
    private static final LinkedHashMap<AbstractCauldronBlock, Fluid> CAULDRON_FLUID_MAP = new LinkedHashMap<>();
    private static final LinkedHashMap<AbstractCauldronBlock, Block> CAULDRON_BLOCK_MAP = new LinkedHashMap<>();

    /**
     * All Interaction Maps for all Cauldrons that support fluids mapped with its fluid type
     */
    private static final LinkedHashMap<Fluid, CauldronInteraction.InteractionMap> CAULDRON_INTERACTION_MAPS = new LinkedHashMap<>();
    private static final LinkedHashMap<Block, CauldronInteraction.InteractionMap> SOLID_CAULDRON_INTERACTION_MAPS = new LinkedHashMap<>();

    /**
     * When a mod would like to have cauldron support for their liquid they have to call this function
     *
     * @param fluid             The fluid that should be supported
     * @param cauldron          The cauldron block that represents a cauldron filled with the desired liquid
     * @param fillCauldronSound The sound that should be played when a cauldron is filled with the liquid
     * @param fillBucketSound   The sound that should be played when a cauldron with the liquid is emptied
     */
    public static void addCauldronSupport(Fluid fluid, Block cauldron, SoundEvent fillCauldronSound, SoundEvent fillBucketSound) {
        if (!CAULDRON_SUPPORT.containsKey(fluid) && cauldron instanceof AbstractCauldronBlock cauldronBlock) {
            CAULDRON_SUPPORT.put(fluid, new CauldronInfo(cauldronBlock, fillCauldronSound, fillBucketSound));
            CAULDRON_FLUID_MAP.put(cauldronBlock, fluid);
        }
    }

    public static void addCauldronSupport(Block block, Block cauldron, SoundEvent fillCauldronSound, SoundEvent fillBucketSound) {
        if (!SOLID_CAULDRON_SUPPORT.containsKey(block) && cauldron instanceof AbstractCauldronBlock cauldronBlock) {
            SOLID_CAULDRON_SUPPORT.put(block, new CauldronInfo(cauldronBlock, fillCauldronSound, fillBucketSound));
            CAULDRON_BLOCK_MAP.put(cauldronBlock, block);
        }
    }


    public static CauldronInteraction.InteractionMap getCauldronInteractionMap(Fluid fluid) {
        if (CAULDRON_INTERACTION_MAPS.containsKey(fluid))
            return CAULDRON_INTERACTION_MAPS.get(fluid);

        CAULDRON_INTERACTION_MAPS.put(fluid, CauldronInteraction.newInteractionMap(BuiltInRegistries.FLUID.getKey(fluid).toString()));
        return CAULDRON_INTERACTION_MAPS.get(fluid);
    }

    public static CauldronInteraction.InteractionMap getCauldronInteractionMap(Block block) {
        if (SOLID_CAULDRON_INTERACTION_MAPS.containsKey(block))
            return SOLID_CAULDRON_INTERACTION_MAPS.get(block);

        SOLID_CAULDRON_INTERACTION_MAPS.put(block, CauldronInteraction.newInteractionMap(BuiltInRegistries.BLOCK.getKey(block).toString()));
        return SOLID_CAULDRON_INTERACTION_MAPS.get(block);
    }

    public static Fluid getFluidInCauldron(AbstractCauldronBlock cauldron) {
        return CAULDRON_FLUID_MAP.getOrDefault(cauldron, Fluids.EMPTY);
    }

    public static Block getBlockInCauldron(AbstractCauldronBlock cauldron) {
        return CAULDRON_BLOCK_MAP.getOrDefault(cauldron, Blocks.AIR);
    }

    public static AbstractCauldronBlock getCauldronForFluid(Fluid fluid) {
        return CAULDRON_SUPPORT.containsKey(fluid) ? CAULDRON_SUPPORT.get(fluid).cauldron() : null;
    }

    public static AbstractCauldronBlock getCauldronForBlock(Block block) {
        return SOLID_CAULDRON_SUPPORT.containsKey(block) ? SOLID_CAULDRON_SUPPORT.get(block).cauldron() : null;
    }

    public static void bootstrap() {

        CAULDRON_INTERACTION_MAPS.put(Fluids.LAVA, CauldronInteraction.LAVA);
        CAULDRON_INTERACTION_MAPS.put(Fluids.WATER, CauldronInteraction.WATER);
        CAULDRON_INTERACTION_MAPS.put(Fluids.EMPTY, CauldronInteraction.EMPTY);
        SOLID_CAULDRON_INTERACTION_MAPS.put(Blocks.POWDER_SNOW, CauldronInteraction.POWDER_SNOW);


        HashMap<Fluid, List<BucketItem>> fluidBuckets = new HashMap<>();
        HashMap<Block, List<SolidBucketItem>> solidBuckets = new HashMap<>();

        BucketCompatibility.BUCKET_GROUPS.forEach((groupId, bucketGroup) -> {

            bucketGroup.getFluidCompat().forEach((fluid, bucketItem) -> {
                List<BucketItem> buckets = fluidBuckets.getOrDefault(fluid, new ArrayList<>());
                buckets.add(bucketItem);
                fluidBuckets.put(fluid, buckets);
            });

            bucketGroup.getSolidCompat().forEach((block, solidBucketItem) -> {
                List<SolidBucketItem> buckets = solidBuckets.getOrDefault(block, new ArrayList<>());
                buckets.add(solidBucketItem);
                solidBuckets.put(block, buckets);
            });
        });

        //-------- Fluid Cauldron Support --------

        CAULDRON_SUPPORT.forEach((fluid, cauldronInfo) -> {
            CauldronInteraction fill_cauldron = (blockState, level, blockPos, player, interactionHand, itemStack) ->
                    BucketCompatibility.emptyBucket(
                            level,
                            blockPos,
                            player,
                            interactionHand,
                            itemStack,
                            cauldronInfo.cauldron() instanceof LayeredCauldronBlock ? cauldronInfo.cauldron().defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, LayeredCauldronBlock.MAX_FILL_LEVEL) : cauldronInfo.cauldron().defaultBlockState(),
                            cauldronInfo.fillCauldronSound()
                    );

            CAULDRON_INTERACTION_MAPS.forEach((cauldronFluid, interaction) -> {
                fluidBuckets.getOrDefault(fluid, new ArrayList<>()).forEach(bucket -> {
                    if (BucketCompatibility.isVanillaBucket(bucket) && FluidCompatibility.isVanillaFluid(cauldronFluid))
                        return;

                    interaction.map().put(bucket, fill_cauldron);
                });
            });

            SOLID_CAULDRON_INTERACTION_MAPS.forEach((cauldronBlock, interaction) -> {
                fluidBuckets.getOrDefault(fluid, new ArrayList<>()).forEach(bucket -> {
                    if(BucketCompatibility.isVanillaBucket(bucket) && FluidCompatibility.isVanillaBlock(cauldronBlock))
                        return;

                    interaction.map().put(bucket, fill_cauldron);
                });
            });


            BucketCompatibility.BUCKET_GROUPS.forEach((groupId, bucketGroup) -> {
                if (bucketGroup.getFilledBucket(fluid) == ItemStack.EMPTY)
                    return;

                CauldronInteraction fill_bucket = (blockState, level, blockPos, player, interactionHand, itemStack) ->
                        BucketCompatibility.fillBucket(
                                blockState,
                                level,
                                blockPos,
                                player,
                                interactionHand,
                                itemStack,
                                bucketGroup.getFilledBucket(fluid),
                                blockStatex -> true,
                                cauldronInfo.fillBucketSound()

                        );

                CAULDRON_INTERACTION_MAPS.get(fluid).map().put(bucketGroup.getEmptyBucket().getItem(), fill_bucket);
            });

            LifeCompat.LOGGER.info("Registered Cauldron support for fluid: {}", BuiltInRegistries.FLUID.getKey(fluid));
        });

        //-------- Solid Cauldron Support --------

        SOLID_CAULDRON_SUPPORT.forEach((block, cauldronInfo) -> {
            CauldronInteraction fill_cauldron = (blockState, level, blockPos, player, interactionHand, itemStack) ->
                    BucketCompatibility.emptyBucket(
                            level,
                            blockPos,
                            player,
                            interactionHand,
                            itemStack,
                            cauldronInfo.cauldron() instanceof LayeredCauldronBlock ? cauldronInfo.cauldron().defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, LayeredCauldronBlock.MAX_FILL_LEVEL) : cauldronInfo.cauldron().defaultBlockState(),
                            cauldronInfo.fillCauldronSound()
                    );

            SOLID_CAULDRON_INTERACTION_MAPS.forEach((cauldronBlock, interaction) -> {
                solidBuckets.getOrDefault(block, new ArrayList<>()).forEach(bucket -> {
                    if (BucketCompatibility.isVanillaBucket(bucket) && FluidCompatibility.isVanillaBlock(cauldronBlock))
                        return;

                    interaction.map().put(bucket, fill_cauldron);
                });
            });

            CAULDRON_INTERACTION_MAPS.forEach((cauldronFluid, interaction) -> {
                solidBuckets.getOrDefault(block, new ArrayList<>()).forEach(bucket -> {
                   if(BucketCompatibility.isVanillaBucket(bucket) && FluidCompatibility.isVanillaFluid(cauldronFluid))
                       return;

                   interaction.map().put(bucket, fill_cauldron);
                });
            });

            BucketCompatibility.BUCKET_GROUPS.forEach((groupId, bucketGroup) -> {
                if (bucketGroup.getFilledBucket(block) == ItemStack.EMPTY)
                    return;

                CauldronInteraction fill_bucket = (blockState, level, blockPos, player, interactionHand, itemStack) ->
                        BucketCompatibility.fillBucket(
                                blockState,
                                level,
                                blockPos,
                                player,
                                interactionHand,
                                itemStack,
                                bucketGroup.getFilledBucket(block),
                                blockStatex -> true,
                                cauldronInfo.fillBucketSound()

                        );

                SOLID_CAULDRON_INTERACTION_MAPS.get(block).map().put(bucketGroup.getEmptyBucket().getItem(), fill_bucket);
            });

            LifeCompat.LOGGER.info("Registered Cauldron support for block: {}", BuiltInRegistries.BLOCK.getKey(block));
        });
    }


    private static boolean isVanillaFluid(Fluid fluid) {
        return FluidCompatibility.getCauldronForFluid(fluid) != null && BuiltInRegistries.BLOCK.getKey(FluidCompatibility.getCauldronForFluid(fluid)).getNamespace().equals("minecraft");
    }

    private static boolean isVanillaBlock(Block block) {
        return FluidCompatibility.getCauldronForBlock(block) != null && BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals("minecraft");
    }

    record CauldronInfo(AbstractCauldronBlock cauldron, SoundEvent fillCauldronSound, SoundEvent fillBucketSound) {
    }

}
