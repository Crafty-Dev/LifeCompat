# LifeCompat
(Currently only for fabric)

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
### Info for custom Fluid storages (Tanks, Mod Blocks that can hold fluids)
If you add a custom Block, that should be able to interact with buckets and does not just expand Minecraft's Fluid Blocks like `LiquidBlock` or `SimpleWaterloggableBlock`, consider implementing the `IFluidProvider` interface to ensure that everything concerning Buckets works perfectly. Otherwise it could be, that only the vanilla buckets work.
**Another last Hint**: When you implement Minecraft's `BucketPickup` interface, you don't need to return custom buckets by yourself. Just return the vanilla ones (or custom buckets that belong to the vanilla group) and you will be fine.
## Listen to Events
As mentioned before there is a usefull Set of Basic Events you can find in the `BaseEvents` class.
To listen to one of these, simply create a class that implements the `EventListener` interface from the package `de.crafty.lifecompat.api.event` and give it the correct Callback as the generic Parameter.
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
The `EventListener` interface forces you to override the `onEventCallback` method. This method is called whenever the event is triggered.
The `callback` parameter contains all the relevant data associated with the event.
To finish the event listener creation and finally get it working, simply call `EventManager.registerListener` in your `ModInitializer`.
```java
        EventManager.registerListener(BaseEvents.BLOCK_BREAK, new BlockBreakListener());
```
### Modifiable Event Callbacks
Some Callbacks allow you to change their data, which results in a different game behaviour.
For example, the BlockBreakEvent is cancellable, so the execution of the action (breaking the block) can be prevented by calling `callback.setCancelled(true)`.
## Create custom Events
Creating new Events is a bit more complicated, but should be pretty straight forward.
First you need to create the event class itself. To understand the event creation process better, let's assume we want to create an event that is called whenever an entity is removed from the world.
So create a class called `EntityRemoveEvent` and let it extend from the `Event` class, located in the `de.crafty.lifecompat.api.event` package.
You will notice that there is a generic parameter missing that is used by the `Event` class. This is where you define the callback that is later passed to the event listeners.
So let's create one:
```java
public class EntityRemoveEvent extends Event<EntityRemoveEvent.Callback> { //Tell the event here which callback it should use


    public EntityRemoveEvent() {
        super(ResourceLocation.fromNamespaceAndPath(MODID, "entity_remove")); //Here we define the id of the event to make it unique
    }

    public record Callback(Entity entity, Level level, Entity.RemovalReason removalReason) implements EventCallback { //Don't forget to implement the EventCallback

    }
}
```
The `Callback` class (or record) defines the data, the event will and need to provide when it's triggered.
So in our case, we provide the `Entity` that is removed, the `Level` the entity was in and the `Entity.RemovalReason` why the entity has been removed.

The next step is to register the event. Therefore you just have to call `EventManager.registerEvent` and give it a new Instance of the event.
```java
    public static final EntityRemoveEvent ENTITY_REMOVE = EventManager.registerEvent(new EntityRemoveEvent());
```
I recommend putting the event references all in one place, like a class called `YourModEvents`, so it's way easier for another person to see which events are present.

After you've created your event and registered it, you can call it by using the `EventManager.callEvent` method. This is usually done in a `Mixin`, but feel free to call events from somewhere else, as long as it makes sense. (for example if you've created a new game mechanic and what it to trigger an event from there).
In our example we inject some code into the `Entity` class.
```java
@Mixin(Entity.class)
public abstract class MixinEntity {

    @Shadow private Level level;

    @Inject(method = "remove", at = @At("HEAD"))
    private void hookIntoEntityRemoving(Entity.RemovalReason reason, CallbackInfo ci){
        EventManager.callEvent(YourModEvents.ENTITY_REMOVE, new EntityRemoveEvent.Callback((Entity) (Object) this, this.level, reason));
    }
}
```
The `EventManager.callEvent` method takes 2 arguments. A reference to the event you've just created and a new Callback of this event, which is also returned by the method to resolve callback differences and handle things like event cancelling.
In our case we use `YourModEvents.ENTITY_REMOVE` as the reference and `new EntityRemoveEvent.Callback((Entity) (Object) this, this.level, reason)` as the callback.

Congratulations!, you successfully created a new event.
Now you can listen for it by following the instructions mentioned at the beginning.

