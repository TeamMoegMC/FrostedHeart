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

package com.teammoeg.frostedheart.content.climate.gamedata.climate;

import java.util.Calendar;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

/**
 * An anti command and sleep clock source
 */
public class WorldClockSource {
    long secs;
    long lastdaytime;
	public static final int secondsPerHour = 50;
	public static final int hoursPerDay = 24;
	public static final int secondsPerDay = hoursPerDay * secondsPerHour;
	public static final int daysPerMonth = 30;
	public static final int gameStartYear=2060;
	public static final int gameStartMonth=9;
	public static final int gameStartDate=5;
	private Calendar calendar;
	
	
    public WorldClockSource() {
    }

    public void deserialize(CompoundTag cnbt) {
        secs = cnbt.getLong("secs");
        lastdaytime = cnbt.getLong("last");
    }
    public int getMonth() {
        return getDate() / daysPerMonth;
    }

    public int getDate() {
        return (int) (secs / secondsPerDay);
    }

    public int getHourInDay() {
        return (int) ((secs / secondsPerHour) % hoursPerDay);
    }

    public long getHours() {
        return (secs / secondsPerHour);
    }

    public int getMinutes() {
        return Math.round(((secs % secondsPerHour)/secondsPerHour)*60);
    }

    public long getTimeSecs() {
        return secs;
    }
    public Calendar getGameCalendar() {
    	if(calendar==null) {
	    	calendar=Calendar.getInstance();
	    	calendar.set(gameStartYear, gameStartMonth, gameStartDate,0,0,0);
	    	calendar.add(Calendar.DATE, getDate());
	    	calendar.add(Calendar.HOUR_OF_DAY, getHourInDay());
	    	calendar.add(Calendar.MINUTE, getMinutes());
    	}
    	return calendar;
    }
    public int getDisplayWeekOfYear() {
        return calendar.get(Calendar.WEEK_OF_YEAR);
    }
    public int getDisplayYear() {
        return calendar.get(Calendar.YEAR);
    }
    public int getDisplayMonth() {
        return calendar.get(Calendar.MONTH);
    }

    public int getDisplayDate() {
        return calendar.get(Calendar.DAY_OF_MONTH);
    }

    public int getDisplayHourOfDay() {
        return calendar.get(Calendar.HOUR_OF_DAY);
    }

    public int getDisplayHours() {
        return calendar.get(Calendar.HOUR);
    }
    public int getDisplayAMPM() {
        return calendar.get(Calendar.AM_PM);
    }

    public int getDisplayMinutes() {
        return calendar.get(Calendar.MINUTE);
    }

    public CompoundTag serialize() {
        return serialize(new CompoundTag());
    }

    public CompoundTag serialize(CompoundTag cnbt) {
        cnbt.putLong("secs", secs);
        cnbt.putLong("last", lastdaytime);
        return cnbt;
    }

    public void setDate(long date) {
        secs = (secs % 1200) + date * 1200;
    }

    @Override
    public String toString() {
        return "WorldClockSource [secs=" + secs + "]";
    }

    public void update(long newTime) {
        long dt = newTime - lastdaytime;
        if (dt < 0) {// if time run backwards, it's command done the trick
            long nextday = lastdaytime + 24000L;
            nextday = nextday - nextday % 24000L;
            dt = newTime % 24000L + nextday - lastdaytime;//assume it's next day and continue
        }
        secs += dt / 20;
        if(dt>20)
        	calendar=null;
        lastdaytime = newTime - newTime % 20;
    }

    public void update(ServerLevel w) {
        update(w.getDayTime());
    }
}
