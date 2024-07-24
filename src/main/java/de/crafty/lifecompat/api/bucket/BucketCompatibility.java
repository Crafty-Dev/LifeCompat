package de.crafty.lifecompat.api.bucket;

import de.crafty.lifecompat.LifeCompat;
import net.fabricmc.fabric.mixin.transfer.BucketItemAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.*;
import java.util.function.Predicate;

/**
 * Currently only supports fluid & solid Buckets
 * TODO: Add EntityBucket Compat
 */
public class BucketCompatibility {

    private static final LinkedHashMap<ResourceLocation, BucketGroup> BUCKET_GROUPS = new LinkedHashMap<>();
    private static final LinkedHashMap<ResourceLocation, List<Item>> ADDITIONAL_BUCKETS = new LinkedHashMap<>();

    //---------- API Methods ----------

    public static void registerBucketGroup(ResourceLocation id, Item... buckets) {
        if (BUCKET_GROUPS.containsKey(id)) {
            LifeCompat.LOGGER.warn("Failed to register BucketGroup {}:{} as it already exists", id.getNamespace(), id.getPath());
            return;
        }

        BUCKET_GROUPS.put(id, createBucketGroup(buckets));

    }

    public static void addBucketsToGroup(ResourceLocation groupId, Item... buckets) {
        if (ADDITIONAL_BUCKETS.containsKey(groupId)) Collections.addAll(ADDITIONAL_BUCKETS.get(groupId), buckets);

        if (!ADDITIONAL_BUCKETS.containsKey(groupId)) ADDITIONAL_BUCKETS.put(groupId, Arrays.stream(buckets).toList());

    }

    public static void addBucketToGroup(ResourceLocation groupId, Item bucket) {
        addBucketsToGroup(groupId, bucket);
    }

    //---------- Helper Methods for BucketCompat (don't call by yourself if you don't know what you're doing) ----------

    public static ItemStack getFilledBucket(BucketItem emptyBucket, Fluid fluid) {
        for (BucketGroup type : BUCKET_GROUPS.values()) {
            if (type.contains(emptyBucket)) return type.getFilledBucket(fluid);

        }
        return ItemStack.EMPTY;
    }

    public static ItemStack getFilledBucket(BucketItem emptyBucket, Block block) {
        for (BucketGroup type : BUCKET_GROUPS.values()) {
            if (type.contains(emptyBucket)) return type.getFilledBucket(block);

        }
        return ItemStack.EMPTY;
    }

    public static ItemStack getEmptyBucket(Item bucket) {
        for (BucketGroup type : BUCKET_GROUPS.values()) {
            if (type.contains(bucket)) return type.getEmptyBucket();
        }
        return new ItemStack(Items.BUCKET);
    }

    private static boolean isVanillaBucket(Item item) {
        return item == Items.BUCKET || item == Items.WATER_BUCKET || item == Items.LAVA_BUCKET || item == Items.POWDER_SNOW_BUCKET;
    }

    private static BucketGroup createBucketGroup(Item... buckets) {
        HashMap<Fluid, BucketItem> fluidCompat = new HashMap<>();
        HashMap<Block, SolidBucketItem> solidCompat = new HashMap<>();
        for (Item bucket : buckets) {
            if (bucket instanceof BucketItem) {
                Fluid fluid = ((BucketItemAccessor) bucket).fabric_getFluid();
                fluidCompat.put(fluid, (BucketItem) bucket);
            }
            if (bucket instanceof SolidBucketItem solidBucket) {
                Block block = solidBucket.getBlock();
                solidCompat.put(block, solidBucket);
            }
        }
        return new BucketGroup(fluidCompat, solidCompat);
    }


    //---------- BucketGroup Representation ----------
    static class BucketGroup {

        private final HashMap<Fluid, BucketItem> fluidCompat;
        private final HashMap<Block, SolidBucketItem> solidCompat;

        private BucketGroup(HashMap<Fluid, BucketItem> fluidCompat, HashMap<Block, SolidBucketItem> solidCompat) {
            this.fluidCompat = fluidCompat;
            this.solidCompat = solidCompat;
        }

        public List<BucketItem> getBuckets() {
            return this.fluidCompat.values().stream().toList();
        }

        private HashMap<Fluid, BucketItem> getFluidCompat() {
            return this.fluidCompat;
        }

        public HashMap<Block, SolidBucketItem> getSolidCompat() {
            return this.solidCompat;
        }

        private ItemStack getFilledBucket(Fluid fluid) {
            return this.fluidCompat.containsKey(fluid) ? new ItemStack(this.fluidCompat.get(fluid)) : ItemStack.EMPTY;
        }

        private ItemStack getFilledBucket(Block block) {
            return this.solidCompat.containsKey(block) ? new ItemStack(this.solidCompat.get(block)) : ItemStack.EMPTY;
        }

        private ItemStack getEmptyBucket() {
            return this.fluidCompat.containsKey(Fluids.EMPTY) ? new ItemStack(this.fluidCompat.get(Fluids.EMPTY)) : ItemStack.EMPTY;
        }

        private boolean contains(Item bucket) {
            return !this.fluidCompat.values().stream().filter(bucket::equals).toList().isEmpty() || !this.solidCompat.values().stream().filter(bucket::equals).toList().isEmpty();
        }

        //Merges 2 BucketGroups together
        //Used for additional buckets
        private void merge(BucketGroup otherGroup) {
            this.fluidCompat.putAll(otherGroup.fluidCompat);
            this.solidCompat.putAll(otherGroup.solidCompat);
        }

    }

    //---------- Init ----------

    public static void bootstrap() {
        BUCKET_GROUPS.forEach((id, bucketGroup) -> {
            if (ADDITIONAL_BUCKETS.containsKey(id))
                bucketGroup.merge(createBucketGroup(ADDITIONAL_BUCKETS.get(id).toArray(Item[]::new)));

            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < bucketGroup.getBuckets().size(); i++) {
                builder.append(bucketGroup.getBuckets().get(i).builtInRegistryHolder().getRegisteredName());
                if (i < bucketGroup.getBuckets().size() - 1) builder.append(", ");
            }
            LifeCompat.LOGGER.info("Bucket Group {}:{} present; containing: {}", id.getNamespace(), id.getPath(), builder);

            bucketGroup.getFluidCompat().forEach((fluid, bucketItem) -> {
                if (isVanillaBucket(bucketItem)) return;

                if (!fluid.equals(Fluids.EMPTY)) {
                    DispenserBlock.registerBehavior(bucketItem, BucketCompatibility.getEmptyBehaviour(bucketGroup));

                    if (fluid.isSame(Fluids.WATER)) BucketCompatibility.registerCauldronWater(bucketItem);
                    if (fluid.isSame(Fluids.LAVA)) BucketCompatibility.registerCauldronLava(bucketItem);

                } else {
                    DispenserBlock.registerBehavior(bucketItem, BucketCompatibility.getFillBehaviour(bucketGroup));
                    if (bucketGroup.getFilledBucket(Fluids.WATER) != ItemStack.EMPTY)
                        CauldronInteraction.WATER.map().put(bucketItem, (blockState, level, blockPos, player, interactionHand, itemStack) ->
                                fillBucket(blockState, level, blockPos, player, interactionHand, itemStack,
                                        bucketGroup.getFilledBucket(Fluids.WATER),
                                        blockStatex -> blockStatex.getValue(LayeredCauldronBlock.LEVEL) == 3,
                                        SoundEvents.BUCKET_FILL
                                ));
                    if (bucketGroup.getFilledBucket(Fluids.LAVA) != ItemStack.EMPTY) {
                        CauldronInteraction.LAVA.map().put(bucketItem, (blockState, level, blockPos, player, interactionHand, itemStack) ->
                                fillBucket(
                                        blockState, level, blockPos, player, interactionHand, itemStack,
                                        bucketGroup.getFilledBucket(Fluids.LAVA),
                                        blockStatex -> true,
                                        SoundEvents.BUCKET_FILL_LAVA
                                ));
                    }
                    if (bucketGroup.getFilledBucket(Blocks.POWDER_SNOW) != ItemStack.EMPTY)
                        CauldronInteraction.POWDER_SNOW.map().put(bucketItem, (blockState, level, blockPos, player, interactionHand, itemStack) ->
                                fillBucket(blockState, level, blockPos, player, interactionHand, itemStack,
                                        bucketGroup.getFilledBucket(Blocks.POWDER_SNOW),
                                        blockStatex -> blockStatex.getValue(LayeredCauldronBlock.LEVEL) == 3,
                                        SoundEvents.BUCKET_FILL_POWDER_SNOW
                                ));
                }

            });
            bucketGroup.getSolidCompat().forEach((block, solidBucketItem) -> {
                if (isVanillaBucket(solidBucketItem)) return;

                DispenserBlock.registerBehavior(solidBucketItem, BucketCompatibility.getEmptyBehaviour(bucketGroup));
                if (block == Blocks.POWDER_SNOW) BucketCompatibility.registerCauldronPowderSnow(solidBucketItem);
            });
        });
    }


    //---------- Dispenser ----------

    private static DispenseItemBehavior getEmptyBehaviour(BucketGroup bucketGroup) {
        return new DefaultDispenseItemBehavior() {
            private final DefaultDispenseItemBehavior fallbackBehavior = new DefaultDispenseItemBehavior();

            @Override
            public ItemStack execute(BlockSource pointer, ItemStack stack) {
                DispensibleContainerItem fluidModificationItem = (DispensibleContainerItem) stack.getItem();
                BlockPos blockPos = pointer.pos().relative(pointer.state().getValue(DispenserBlock.FACING));
                Level world = pointer.level();
                if (fluidModificationItem.emptyContents(null, world, blockPos, null)) {
                    fluidModificationItem.checkExtraContent(null, world, stack, blockPos);
                    return this.consumeWithRemainder(pointer, stack, bucketGroup.getEmptyBucket());
                } else {
                    return this.fallbackBehavior.dispense(pointer, stack);
                }
            }
        };
    }

    private static DispenseItemBehavior getFillBehaviour(BucketGroup bucketGroup) {
        return new DefaultDispenseItemBehavior() {
            @Override
            public ItemStack execute(BlockSource pointer, ItemStack stack) {
                LevelAccessor worldAccess = pointer.level();
                BlockPos blockPos = pointer.pos().relative(pointer.state().getValue(DispenserBlock.FACING));
                BlockState blockState = worldAccess.getBlockState(blockPos);
                if (blockState.getBlock() instanceof BucketPickup fluidDrainable) {
                    ItemStack itemStack = fluidDrainable.pickupBlock(null, worldAccess, blockPos, blockState);
                    if (itemStack.getItem() instanceof BucketItem)
                        itemStack = bucketGroup.getFilledBucket(((BucketItemAccessor) itemStack.getItem()).fabric_getFluid());
                    if (itemStack.getItem() instanceof SolidBucketItem)
                        itemStack = bucketGroup.getFilledBucket(((SolidBucketItem) itemStack.getItem()).getBlock());

                    if (itemStack.isEmpty()) {
                        return super.execute(pointer, stack);
                    } else {
                        worldAccess.gameEvent(null, GameEvent.FLUID_PICKUP, blockPos);
                        Item item = itemStack.getItem();
                        return this.consumeWithRemainder(pointer, stack, new ItemStack(item));
                    }
                } else {
                    return super.execute(pointer, stack);
                }
            }
        };
    }

    //---------- Cauldron ----------

    //TODO: Cauldron Compat (More fluids)
    static final CauldronInteraction FILL_WATER = (blockState, level, blockPos, player, interactionHand, itemStack) -> BucketCompatibility.emptyBucket(level, blockPos, player, interactionHand, itemStack, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, Integer.valueOf(3)), SoundEvents.BUCKET_EMPTY);
    static final CauldronInteraction FILL_LAVA = (blockState, level, blockPos, player, interactionHand, itemStack) -> BucketCompatibility.emptyBucket(level, blockPos, player, interactionHand, itemStack, Blocks.LAVA_CAULDRON.defaultBlockState(), SoundEvents.BUCKET_EMPTY_LAVA);
    static final CauldronInteraction FILL_POWDER_SNOW = (blockState, level, blockPos, player, interactionHand, itemStack) -> BucketCompatibility.emptyBucket(level, blockPos, player, interactionHand, itemStack, Blocks.POWDER_SNOW_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3), SoundEvents.BUCKET_EMPTY_POWDER_SNOW);


    public static ItemInteractionResult emptyBucket(Level level, BlockPos blockPos, Player player, InteractionHand interactionHand, ItemStack filledStack, BlockState blockState, SoundEvent soundEvent) {
        if (!level.isClientSide) {
            Item item = filledStack.getItem();
            player.setItemInHand(interactionHand, ItemUtils.createFilledResult(filledStack, player, BucketCompatibility.getEmptyBucket(filledStack.getItem())));
            player.awardStat(Stats.FILL_CAULDRON);
            player.awardStat(Stats.ITEM_USED.get(item));
            level.setBlockAndUpdate(blockPos, blockState);
            level.playSound(null, blockPos, soundEvent, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(null, GameEvent.FLUID_PLACE, blockPos);
        }

        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    static ItemInteractionResult fillBucket(BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand interactionHand, ItemStack emptyStack, ItemStack filledStack, Predicate<BlockState> predicate, SoundEvent soundEvent) {
        if (!predicate.test(blockState)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        } else {
            if (!level.isClientSide) {
                Item item = emptyStack.getItem();
                player.setItemInHand(interactionHand, ItemUtils.createFilledResult(emptyStack, player, filledStack));
                player.awardStat(Stats.USE_CAULDRON);
                player.awardStat(Stats.ITEM_USED.get(item));
                level.setBlockAndUpdate(blockPos, Blocks.CAULDRON.defaultBlockState());
                level.playSound(null, blockPos, soundEvent, SoundSource.BLOCKS, 1.0F, 1.0F);
                level.gameEvent(null, GameEvent.FLUID_PICKUP, blockPos);
            }

            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
    }

    private static void registerCauldronLava(BucketItem bucketItem) {
        CauldronInteraction.EMPTY.map().put(bucketItem, FILL_LAVA);
        CauldronInteraction.WATER.map().put(bucketItem, FILL_LAVA);
        CauldronInteraction.LAVA.map().put(bucketItem, FILL_LAVA);
        CauldronInteraction.POWDER_SNOW.map().put(bucketItem, FILL_LAVA);
    }

    private static void registerCauldronWater(BucketItem bucketItem) {
        CauldronInteraction.EMPTY.map().put(bucketItem, FILL_WATER);
        CauldronInteraction.WATER.map().put(bucketItem, FILL_WATER);
        CauldronInteraction.LAVA.map().put(bucketItem, FILL_WATER);
        CauldronInteraction.POWDER_SNOW.map().put(bucketItem, FILL_WATER);
    }

    private static void registerCauldronPowderSnow(SolidBucketItem solidBucketItem) {
        CauldronInteraction.EMPTY.map().put(solidBucketItem, FILL_POWDER_SNOW);
        CauldronInteraction.WATER.map().put(solidBucketItem, FILL_POWDER_SNOW);
        CauldronInteraction.LAVA.map().put(solidBucketItem, FILL_POWDER_SNOW);
        CauldronInteraction.POWDER_SNOW.map().put(solidBucketItem, FILL_POWDER_SNOW);
    }
}
