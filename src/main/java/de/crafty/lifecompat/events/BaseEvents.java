package de.crafty.lifecompat.events;

import de.crafty.lifecompat.api.event.EventManager;
import de.crafty.lifecompat.events.block.BlockBreakEvent;
import de.crafty.lifecompat.events.block.BlockChangeEvent;
import de.crafty.lifecompat.events.block.BlockInteractEvent;
import de.crafty.lifecompat.events.blockentity.BlockEntityLoadEvent;
import de.crafty.lifecompat.events.entity.EntityRemoveEvent;
import de.crafty.lifecompat.events.game.GamePostInitEvent;
import de.crafty.lifecompat.events.item.ItemDropEvent;
import de.crafty.lifecompat.events.item.ItemTickEvent;
import de.crafty.lifecompat.events.item.ItemUseEvent;
import de.crafty.lifecompat.events.player.PlayerDeathEvent;
import de.crafty.lifecompat.events.player.PlayerEnterLevelEvent;
import de.crafty.lifecompat.events.player.PlayerMoveEvent;
import de.crafty.lifecompat.events.player.PlayerToggleSneakEvent;
import de.crafty.lifecompat.events.world.WorldStartupEvent;

public class BaseEvents {

    //Game
    public static final GamePostInitEvent GAME_POST_INIT = EventManager.registerEvent(new GamePostInitEvent());

    //Player
    public static final PlayerEnterLevelEvent PLAYER_ENTER_LEVEL = EventManager.registerEvent(new PlayerEnterLevelEvent());
    public static final PlayerToggleSneakEvent PLAYER_TOGGLE_SNEAK = EventManager.registerEvent(new PlayerToggleSneakEvent());
    public static final PlayerMoveEvent PLAYER_MOVE = EventManager.registerEvent(new PlayerMoveEvent());
    public static final PlayerDeathEvent PLAYER_DEATH = EventManager.registerEvent(new PlayerDeathEvent());

    //Item
    public static final ItemUseEvent ITEM_USE = EventManager.registerEvent(new ItemUseEvent());
    public static final ItemDropEvent ITEM_DROP = EventManager.registerEvent(new ItemDropEvent());
    public static final ItemTickEvent ITEM_TICK = EventManager.registerEvent(new ItemTickEvent());

    //Block
    public static final BlockChangeEvent BLOCK_CHANGE = EventManager.registerEvent(new BlockChangeEvent());
    public static final BlockInteractEvent BLOCK_INTERACT = EventManager.registerEvent(new BlockInteractEvent());
    public static final BlockBreakEvent BLOCK_BREAK = EventManager.registerEvent(new BlockBreakEvent());

    //BlockEntity
    public static final BlockEntityLoadEvent BLOCK_ENTITY_LOAD = EventManager.registerEvent(new BlockEntityLoadEvent());

    //World
    public static final WorldStartupEvent WORLD_STARTUP = EventManager.registerEvent(new WorldStartupEvent());

    //Entity
    public static final EntityRemoveEvent ENTITY_REMOVE = EventManager.registerEvent(new EntityRemoveEvent());

}
