package de.crafty.lifecompat.api.event;

import net.minecraft.resources.ResourceLocation;

import java.util.LinkedList;

public abstract class Event<T extends EventCallback> {

    private final ResourceLocation id;
    private final LinkedList<EventListener<T>> listeners;

    public Event(ResourceLocation id) {
        this.id = id;
        this.listeners = new LinkedList<>();
    }

    public ResourceLocation eventId(){
        return this.id;
    }

    protected  <S extends EventListener<T>> void registerListener(S listener) {
        if(!this.listeners.contains(listener))
            this.listeners.add(listener);
    }

    protected <S extends EventListener<T>> void removeListener(S listener){
        this.listeners.remove(listener);
    }

    protected void call(T callback) {
        for (EventListener<T> listener : this.listeners) {
            listener.onEventCallback(callback);
            if(callback.shouldStopQueue())
                break;;
        }
    }
}
