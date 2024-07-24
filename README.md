# LifeCompat

LifeCompat is a Compatibility Mod designed to simplify essential Modding Processes like events, bucket compatibility, and Energy. This Library currently provides a pretty lightweight but strong Event API and a way to add Custom Buckets without the need of dealing directly with Minecraft's Code. Life Compat comes with a Set of Basic Events (Block breaking, Block changes, Item Uses,...) which allows the Mod Author to hook into the most common use cases. If there is a thing that can't be done by using existing Events, you can easily add a new Event. You can find a detailed guide below.

## Adding new custom Bucket Types
Adding new Bucket Types is rather simple. Just create your Bucket Items and register them by calling `registerBucketGroup` from the `BucketCompatibility` class.
For example, if you want to add new wooden Buckets, look at this code:
```java
        BucketCompatibility.registerBucketGroup(
                ResourceLocation.fromNamespaceAndPath(MODID, "wood"), 
                ItemRegistry.WOODEN_BUCKET, 
                ItemRegistry.WOODEN_WATER_BUCKET, 
                ItemRegistry.WOODEN_POWDER_SNOW_BUCKET
        );

```
## Expanding existing Bucket Types
Let's say you've just created your incredible unique `SpecialFluid` and now you want to add a Bucket for it to the default Minecraft Buckets.
For this case, there is an extra method called `addBucketToGroup` or `addBucketsToGroup` (for multiple Buckets) in the `BucketCompatibility` class.
The only thing you have to know is the name of the existing Group. In our example this would be `minecraft:iron` (for the default Buckets) so we can simply use `ResourceLocation.withDefaultNamespace("iron")` as the id.
```java
        BucketCompatibility.addBucketToGroup(
                ResourceLocation.withDefaultNamespace("iron"),
                ModItems.SPECIAL_FLUID_BUCKET
        );
```
Et voilà! There you have your own Buckets added to the Game.

