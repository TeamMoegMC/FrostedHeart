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

package com.teammoeg.frostedresearch.events;

import net.minecraftforge.eventbus.api.Event;


/**
 * Class ResearchLoadEvent.
 * Post when research data is loading in server.
 *
 * @author khjxiaogu
 */
public class ResearchLoadEvent extends Event {

    /**
     * Class ResearchLoadEvent.Finish.
     * Post after research data is loaded and prepared to work in server.
     *
     * @author khjxiaogu
     */
    public static class Finish extends ResearchLoadEvent {

    }

    /**
     * Class ResearchLoadEvent.Post.
     * Post after research data is loaded but before indexed in server.
     *
     * @author khjxiaogu
     */
    public static class Post extends ResearchLoadEvent {

    }

    /**
     * Class ResearchLoadEvent.Pre.
     * Post before research data is loading in server.
     *
     * @author khjxiaogu
     */
    public static class Pre extends ResearchLoadEvent {

    }
}
