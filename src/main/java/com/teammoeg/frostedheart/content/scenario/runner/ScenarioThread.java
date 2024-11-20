package com.teammoeg.frostedheart.content.scenario.runner;

import java.util.Collection;
import java.util.Map;

import com.teammoeg.frostedheart.content.scenario.parser.Scenario;
import com.teammoeg.frostedheart.content.scenario.runner.target.ExecuteStackElement;
import com.teammoeg.frostedheart.content.scenario.runner.target.PreparedScenarioTarget;
import com.teammoeg.frostedheart.content.scenario.runner.target.ScenarioTarget;

/**
 * The Interface ScenarioRunner.
 * Interface for all classes that can store execution information for scenario
 */
public interface ScenarioThread {
	
	/**
	 * Sets the current scenario.
	 *
	 * @param s the new scenario
	 */
	void setScenario(Scenario s);
	
	/**
	 * Gets the current scenario.
	 *
	 * @return the scenario
	 */
	Scenario getScenario();
	
	/**
	 * Set current execution position(program counter).
	 *
	 * @param num the new execution position
	 */
	void setExecutePos(int num);
	
	/**
	 * Gets the current execution position
	 *
	 * @return the current execution position
	 */
	int getExecutePos();
	
	/**
	 * Queue a task to the execution task list, queued task would be executed if this runner is free.
	 *
	 * @param t the task
	 */
	void queue(ScenarioTarget t);
	
	/**
	 * go to the task and start running, interrupts any and all current execution but not breaking triggers.
	 *
	 * @param t the task
	 */
	void jump(ScenarioContext ctx,ScenarioTarget t);
	
	void jump(PreparedScenarioTarget target);
	
	/**
	 * Gets the status.
	 *
	 * @return the status
	 */
	RunStatus getStatus();
	
	/**
	 * Sets the status.
	 *
	 * @param status the new status
	 */
	void setStatus(RunStatus status);
	
	/**
	 * Gets the call stack.
	 *
	 * @return the call stack
	 */
	Collection<? extends ExecuteStackElement> getCallStack();
	public double eval(ScenarioContext ctx,String exp) ;

	void callCommand(ScenarioCommandContext scenarioCommandContext, String command, Map<String, String> params);

	void newParagraph(int pn);


	void notifyClientResponse(ScenarioContext ctx, boolean isSkip, int status);

	Scene scene();

	void addWait(int time);

	void stop();
}
