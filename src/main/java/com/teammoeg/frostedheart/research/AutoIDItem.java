package com.teammoeg.frostedheart.research;

public abstract class AutoIDItem extends FHRegisteredItem {
    private String AssignedID;

    @Override
    public String getLId() {
        return AssignedID + "_" + getNonce();
    }

    public void addID(String id, int index) {
        AssignedID = id + "." + getType() + "." + index;
    }

    public abstract String getType();

    public abstract String getNonce();
}
