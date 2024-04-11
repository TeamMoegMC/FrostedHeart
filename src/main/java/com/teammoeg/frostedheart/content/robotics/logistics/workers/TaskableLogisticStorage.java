package com.teammoeg.frostedheart.content.robotics.logistics.workers;

import com.teammoeg.frostedheart.content.robotics.logistics.tasks.LogisticTask;

public interface TaskableLogisticStorage extends ILogisticsStorage{
	LogisticTask[] getTasks();
}
