# LifeCompat

LifeCompat is a Compatibility Mod designed to simplify essential Modding Processes like events, bucket compatibility, and Energy. This Library currently provides a pretty lightweight but strong Event API and a way to add Custom Buckets without the need of dealing directly with Minecraft's Code. Life Compat comes with a Set of Basic Events (Block breaking, Block changes, Item Uses,...) which allows the Mod Author to hook into the most common use cases. If there is a thing that can't be done by using existing Events, you can easily add a new Event. You can find a detailed guide below.

##Adding new custom Buckets
Adding new Buckets is rather simple. Just create your Bucket Items and register them by creating a new Bucket Group.
For example, if you want to add new wooden Buckets, look at this code:
```java
BucketCompatibility.registerBucketGroup(
ResourceLocation.fromNamespaceAndPath(your_mod_id_here, "wood"), // Here you register the Id of your Group
ModItems.WOODEN_BUCKET, //And here you add your items
ModItems.WOODEN_WATER_BUCKET,
ModItems.WOODEN_POWDER_SNOW_BUCKET
);

```

