package de.crafty.lifecompat.api.event;

public abstract class CancellableEventCallback implements EventCallback {
    
    private boolean cancelled = false;


    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public boolean shouldStopQueue() {
        return this.isCancelled();
    }
}
