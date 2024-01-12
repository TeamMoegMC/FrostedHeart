/*
 * Copyright (c) 2021-2024 TeamMoeg
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

package com.teammoeg.frostedheart.util;

import com.teammoeg.frostedheart.FHMain;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FHLogger {
    public static boolean debug = false;
    public static Logger logger = LogManager.getLogger(FHMain.MODNAME);

    public static void debug(Object object) {
        if (debug) log(Level.DEBUG, object);
    }

    public static void error(Object object) {
        log(Level.ERROR, object);
    }

    public static void error(String message, Object... params) {
        logger.log(Level.ERROR, message, params);
    }

    public static void info(Object object) {
        log(Level.INFO, object);
    }

    public static void info(String message, Object... params) {
        logger.log(Level.INFO, message, params);
    }

    public static void log(Level logLevel, Object object) {
        logger.log(logLevel, String.valueOf(object));
    }

    public static void warn(Object object) {
        log(Level.WARN, object);
    }

    public static void warn(String message, Object... params) {
        logger.log(Level.WARN, message, params);
    }


}
