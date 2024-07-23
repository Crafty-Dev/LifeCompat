package de.crafty.lifecompat.api.event;

public interface EventCallback {

    //Returns true when the Event should stop call more listeners
    default boolean shouldStopQueue(){
        return false;
    };

}
