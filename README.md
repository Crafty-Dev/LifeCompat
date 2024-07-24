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
## Listen to Events
As mentioned before there is a usefull Set of Basic Events you can find in the `BaseEvents` class.
To listen to one of these simply create a class that implements the `EventListener` from the package `de.crafty.lifecompat.api.event` and give it the correct Callback as the generic Parameter.
For example, if you want to create an Event Listener that listens for Block Break Events look at the following code:
```java
import de.crafty.lifecompat.api.event.EventListener;
import de.crafty.lifecompat.events.block.BlockBreakEvent;

public class BlockBreakListener implements EventListener<BlockBreakEvent.Callback> {


    @Override
    public void onEventCallback(BlockBreakEvent.Callback callback) {
        //Code to be executed
    }
```
The `EventListener` interface forces you to override the `onEventCallback` method. This method is called whenever the event is called.
The `callback` parameter contains all the relevant data associated with the event.
### Modifiable Event Callbacks
Some Callbacks allow you to change their data, which results in a different game behaviour.
For example, the BlockBreakEvent is cancellable, so the execution of the action (breaking the block) can be prevented by calling `callback.setCancelled(true)`.
## Create custom Events
