package com.teammoeg.frostedheart.content.robotics.logistics.tasks;

import com.teammoeg.frostedheart.content.robotics.logistics.LogisticNetwork;

public interface LogisticTask {

	void work(LogisticNetwork network,int msize);

}
