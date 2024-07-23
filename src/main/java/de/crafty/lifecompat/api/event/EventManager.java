package de.crafty.lifecompat.api.event;

import de.crafty.lifecompat.LifeCompat;

import java.util.ArrayList;
import java.util.List;

//Event Wrapper
public class EventManager {

    private static final List<Event<?>> EVENTS = new ArrayList<>();

    public static <T extends EventCallback, S extends Event<T>> S registerEvent(S event){
        EVENTS.add(event);
        LifeCompat.LOGGER.info("Registered Event with Id: {}", event.eventId());
        return event;
    }

    public static <T extends EventCallback> T callEvent(Event<T> event, T callback){
        event.call(callback);
        return callback;
    }

    public static <T extends EventCallback> void registerListener(Event<T> event, EventListener<T> listener){
        event.registerListener(listener);
    }

}
