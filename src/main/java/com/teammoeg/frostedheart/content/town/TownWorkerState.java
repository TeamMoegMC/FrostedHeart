package com.teammoeg.frostedheart.content.town;

public enum TownWorkerState{
    NOT_INITIALIZED(-1), NOT_VALID(0), VALID(1), OCCUPIED_AREA_OVERLAPPED(2);

    byte stateNum;

    TownWorkerState(byte stateNum) {
        this.stateNum = stateNum;
    }
    TownWorkerState(int stateNum){
        this.stateNum = (byte) stateNum;
    }

    public byte getStateNum() {
        return stateNum;
    }
    public static TownWorkerState fromByte(byte stateNum) {
        for (TownWorkerState state : values()) {
            if (state.stateNum == stateNum) {
                return state;
            }
        }
        throw new IllegalArgumentException("Invalid state number: " + stateNum);
    }
    public static TownWorkerState fromInt(int stateNum) {return fromByte((byte) stateNum);}

    public boolean isValid(){
        return this == VALID;
    }
}