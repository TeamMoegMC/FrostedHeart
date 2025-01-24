package com.teammoeg.chorda.util.struct;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
/**
 * Registry for "meta" events.
 * meta event is a flag that changes after specific initialization code is executed.
 * Listeners would be called after the flag changes, or immediate when flag is already changed.
 * 
 * */
public class MetaEventObservers {
	List<Supplier<Runnable>> listener=new ArrayList<>();
	boolean isFired=false;
	public MetaEventObservers() {
	}
	public synchronized void addListener(Supplier<Runnable> runnable) {
		if(isFired) {
			runnable.get().run();
		}else 
			listener.add(runnable);
		
	}
	public synchronized void setFinished() {
		isFired=true;
		listener.forEach(t->t.get().run());
	}

}
