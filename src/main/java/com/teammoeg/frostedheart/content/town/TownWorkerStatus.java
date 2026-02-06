/*
 * Copyright (c) 2026 TeamMoeg
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
/**
 * FSM:
 * NOT_INITIALIZED -> NOT_VALID_STRUCTURE -> OCCUPIED_AREA_OVERLAPPED -> TOO_COLD -> VALID <-> NOT_VALID(User State Invalidate)
 * OCCUPIED_AREA_OVERLAPPED -> NOT_INITIALIZED
 * TOO_COLD -> NOT_INITIALIZED
 * NOT_VALID_STRUCTURE -> NOT_INITIALIZED
 * VALID -> NOT_INITIALIZED
 * */
public enum TownWorkerStatus{
    NOT_INITIALIZED(0), NOT_VALID(1), VALID(2), OCCUPIED_AREA_OVERLAPPED(3),TOO_COLD(4),NOT_VALID_STRUCTURE(5);

    byte stateNum;

    TownWorkerStatus(byte stateNum) {
        this.stateNum = stateNum;
    }
    TownWorkerStatus(int stateNum){
        this.stateNum = (byte) stateNum;
    }

    public byte getStateNum() {
        return stateNum;
    }
    public static TownWorkerStatus fromByte(byte stateNum) {
        for (TownWorkerStatus state : values()) {
            if (state.stateNum == stateNum) {
                return state;
            }
        }
        throw new IllegalArgumentException("Invalid state number: " + stateNum);
    }
    public static TownWorkerStatus fromInt(int stateNum) {return fromByte((byte) stateNum);}

    public boolean isValid(){
        return this == VALID;
    }
    public boolean isStructureValid() {
    	return this!=NOT_INITIALIZED&&this!=NOT_VALID_STRUCTURE;
    }
}