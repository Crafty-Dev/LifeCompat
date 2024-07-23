package de.crafty.lifecompat.api.bucket;

import de.crafty.lifecompat.LifeCompat;
import net.fabricmc.fabric.mixin.transfer.BucketItemAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Currently only supports fluid & solid Buckets
 * TODO: Add EntityBucket Compat
 */
public class BucketCompatibility {

    private static final List<BucketGroup> BUCKET_GROUPS = new ArrayList<>();


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

    public static void bootstrap() {
        BUCKET_GROUPS.forEach(bucketGroup -> {
            ResourceLocation id = bucketGroup.getId();
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < bucketGroup.getBuckets().size(); i++) {
                builder.append(bucketGroup.getBuckets().get(i).builtInRegistryHolder().getRegisteredName());
                if (i < bucketGroup.getBuckets().size() - 1)
                    builder.append(", ");
            }
            LifeCompat.LOGGER.info("Bucket Group {}:{} present; containing: {}", id.getNamespace(), id.getPath(), builder);

            if (bucketGroup.getId().getNamespace().equals("minecraft"))
                return;

            bucketGroup.getFluidCompat().forEach((fluid, bucketItem) -> {
                if (!fluid.equals(Fluids.EMPTY))
                    DispenserBlock.registerBehavior(bucketItem, BucketCompatibility.getEmptyBehaviour(bucketGroup));
                else
                    DispenserBlock.registerBehavior(bucketItem, BucketCompatibility.getFillBehaviour(bucketGroup));
            });
            bucketGroup.getSolidCompat().forEach((block, powderSnowBucketItem) -> {
                DispenserBlock.registerBehavior(powderSnowBucketItem, BucketCompatibility.getEmptyBehaviour(bucketGroup));
            });
        });
    }

    public static void registerFluidBucketGroup(ResourceLocation id, Item... buckets) {

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

        BUCKET_GROUPS.add(new BucketGroup(id, fluidCompat, solidCompat));

    }

    public static ItemStack getFilledBucket(BucketItem emptyBucket, Fluid fluid) {
        for (BucketGroup type : BUCKET_GROUPS) {
            if (type.contains(emptyBucket))
                return type.getFilledBucket(fluid);

        }
        return ItemStack.EMPTY;
    }

    public static ItemStack getFilledBucket(BucketItem emptyBucket, Block block) {
        for (BucketGroup type : BUCKET_GROUPS) {
            if (type.contains(emptyBucket))
                return type.getFilledBucket(block);

        }
        return ItemStack.EMPTY;
    }

    public static ItemStack getEmptyBucket(Item bucket) {
        for (BucketGroup type : BUCKET_GROUPS) {
            if (type.contains(bucket))
                return type.getEmptyBucket();
        }
        return new ItemStack(Items.BUCKET);
    }


    static class BucketGroup {

        private final ResourceLocation id;
        private final HashMap<Fluid, BucketItem> fluidCompat;
        private final HashMap<Block, SolidBucketItem> solidCompat;

        private BucketGroup(ResourceLocation id, HashMap<Fluid, BucketItem> fluidCompat, HashMap<Block, SolidBucketItem> solidCompat) {
            this.id = id;
            this.fluidCompat = fluidCompat;
            this.solidCompat = solidCompat;
        }

        public ResourceLocation getId() {
            return this.id;
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

    }


}
