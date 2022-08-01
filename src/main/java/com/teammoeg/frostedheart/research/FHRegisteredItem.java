package com.teammoeg.frostedheart.research;

/**
 * Our own registry.
 */
public abstract class FHRegisteredItem {
    private int id = 0;

    public int getRId() {
        return id;
    }

    void setRId(int id) {
        this.id = id;
    }

    public abstract String getLId();

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        FHRegisteredItem other = (FHRegisteredItem) obj;
        if (id != other.id)
            return false;
        return true;
    }
}
