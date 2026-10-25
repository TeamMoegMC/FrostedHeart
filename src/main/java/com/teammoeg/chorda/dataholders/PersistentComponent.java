package com.teammoeg.chorda.dataholders;

/**
 * Opt-in change tracking for a component whose persisted mutations are covered.
 * The listener only records changes; it must not save, send packets or run gameplay.
 * Passing null detaches the server owner and restores a no-op listener.
 */
public interface PersistentComponent extends SpecialData {
    void setPersistenceListener(Runnable listener);
}
