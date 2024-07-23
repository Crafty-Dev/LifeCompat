package de.crafty.lifecompat.api.event;

public interface EventListener<T extends EventCallback> {


    void onEventCallback(T callback);

}
