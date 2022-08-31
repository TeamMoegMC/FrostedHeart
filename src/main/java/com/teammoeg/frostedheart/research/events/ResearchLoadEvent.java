package com.teammoeg.frostedheart.research.events;

import net.minecraftforge.eventbus.api.Event;

// TODO: Auto-generated Javadoc
/**
 * Class ResearchLoadEvent.
 * Post when research data is loading in server.
 * @author khjxiaogu
 */
public class ResearchLoadEvent extends Event {
	
	/**
	 * Class ResearchLoadEvent.Pre.
	 * Post before research data is loading in server.
	 * @author khjxiaogu
	 */
	public static class Pre extends ResearchLoadEvent{
		
	}
	
	/**
	 * Class ResearchLoadEvent.Post.
	 * Post after research data is loaded but before indexed in server.
	 * @author khjxiaogu
	 */
	public static class Post extends ResearchLoadEvent{
		
	}
	/**
	 * Class ResearchLoadEvent.Finish.
	 * Post after research data is loaded and prepared to work in server.
	 * @author khjxiaogu
	 */
	public static class Finish extends ResearchLoadEvent{
		
	}
}
