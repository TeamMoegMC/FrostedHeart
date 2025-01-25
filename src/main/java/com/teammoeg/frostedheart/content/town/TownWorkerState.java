/*
 * Copyright (c) 2024 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

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