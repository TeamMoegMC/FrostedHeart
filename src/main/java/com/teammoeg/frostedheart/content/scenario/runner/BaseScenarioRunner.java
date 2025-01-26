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

package com.teammoeg.frostedheart.content.scenario.runner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.teammoeg.chorda.math.evaluator.Evaluator;
import com.teammoeg.frostedheart.content.scenario.FHScenario;
import com.teammoeg.frostedheart.content.scenario.ScenarioExecutionException;
import com.teammoeg.frostedheart.content.scenario.parser.Node;
import com.teammoeg.frostedheart.content.scenario.parser.Scenario;
import com.teammoeg.frostedheart.content.scenario.runner.target.ExecuteStackElement;
import com.teammoeg.frostedheart.content.scenario.runner.target.ExecuteTarget;
import com.teammoeg.frostedheart.content.scenario.runner.target.PreparedScenarioTarget;
import com.teammoeg.frostedheart.content.scenario.runner.target.ScenarioTarget;

import net.minecraft.nbt.CompoundTag;

/**
 * The Class BaseScenarioRunner.
 */
public class BaseScenarioRunner implements ScenarioThread{
	
	/** Current scenario. */
	protected Scenario sp;
	
	/** Program register. */
	protected int nodeNum=0;
	
	protected Scene scene;
	
	@Override
	public String toString() {
		return "BaseScenarioRunner [sp=" + sp + ", nodeNum=" + nodeNum + ", scene=" + scene + ", toExecute=" + toExecute
				+ ", triggers=" + triggers + ", status=" + status + ", waiting=" + waiting + ", callStack=" + callStack
				+ ", isConducting=" + isConducting + ", currentLabel=" + currentLabel + "]";
	}



	/** Actions appended by trigger and awaiting execution. */
	protected LinkedList<ScenarioTarget> toExecute=new LinkedList<>();

	/** Triggers. */
	protected List<TriggerTarget> triggers=new ArrayList<>();
	
	/** Running Status. */
	protected RunStatus status=RunStatus.STOPPED;
	
	protected int waiting;
	
	/** Call stack. */
	protected LinkedList<ScenarioTarget> callStack=new LinkedList<>();
	
	/** Prevent call run recursively. */
	protected boolean isConducting;
	
	protected ExecuteTarget currentLabel;

	/**
	 * Instantiates a new base scenario runner.
	 */
	public BaseScenarioRunner() {
	}
	
	/**
	 * Gets the scenario.
	 *
	 * @return the scenario
	 */
	@Override
	public Scenario getScenario() {
		return sp;
	}
	
	/**
	 * Sets the scenario.
	 *
	 * @param sp the new scenario
	 */
	@Override
	public void setScenario(Scenario sp) {
		this.sp = sp;
	}
	
	/**
	 * Sets the execute pos.
	 *
	 * @param num the new execute pos
	 */
	@Override
	public void setExecutePos(int num) {
		nodeNum = num;
	}
	
	/**
	 * Gets the status.
	 *
	 * @return the status
	 */
	public RunStatus getStatus() {
		return status;
	}


	/**
	 * Gets the execute pos.
	 *
	 * @return the execute pos
	 */
	@Override
	public int getExecutePos() {
        return nodeNum;
    }


	@Override
	public void jump(ScenarioContext ctx, ScenarioTarget t) {
		if(t instanceof ExecuteTarget et)
			this.currentLabel=et;
		jump(t.prepare(ctx, getScenario()));
	}

	@Override
	public void jump(PreparedScenarioTarget target) {
		if(target.target()!=null)
			this.setScenario(target.target());
		this.setExecutePos(target.nodeNum());
		run();
	}
	/**
	 * Gets the call stack.
	 *
	 * @return the call stack
	 */
	public LinkedList<ScenarioTarget> getCallStack() {
		return callStack;
	}

	
	/**
	 * Queue.
	 *
	 * @param questExecuteTarget the quest execute target
	 */
	public void queue(ScenarioTarget questExecuteTarget) {
		addToQueue(questExecuteTarget);
	}
	
	/**
	 * Adds the to queue.
	 *
	 * @param questExecuteTarget the quest execute target
	 */
	public final void addToQueue(ScenarioTarget questExecuteTarget) {
		toExecute.add(questExecuteTarget);
	}
    
    /**
     * Pop call stack.
     */
	@Override
    public void popCallStack(ScenarioContext ctx) {
		if(getCallStack().isEmpty()) {
			throw new ScenarioExecutionException("Invalid return at "+getScenario().name());
		}
		jump(ctx,getCallStack().pollLast());
	}
    
    /**
     * Try pop call stack.
     */
	@Override
    public void tryPopCallStack(ScenarioContext ctx) {
		if(!getCallStack().isEmpty()) {
			jump(ctx,getCallStack().pollLast());
		}
	}
	
	/**
	 * Adds the call stack.
	 */
	@Override
	public void addCallStack(ExecuteTarget replace) {
		if(replace!=null)
			getCallStack().add(replace);
		else
			getCallStack().add(getCurrentPosition(0));
	}
	
	
	/**
	 * Gets the current position.
	 *
	 * @return the current position
	 */
	@Override
	public ExecuteStackElement getCurrentPosition(int offset) {
    	return new ExecuteStackElement(sp.name(),nodeNum+offset);
    }
	
	/**
	 * Run code.
	 */
	protected void runCode(ScenarioContext ctx) {
    	ScenarioCommandContext commandCtx=new ScenarioCommandContext(ctx,this);
    	while(isRunning()&&getScenario()!=null&&nodeNum<getScenario().pieces().size()) {
    		Node node=getScenario().pieces().get(nodeNum++);
    		try {
    			scene.appendLiteral(node.getLiteral(commandCtx));
    			node.run(commandCtx);
    		}catch(Throwable t) {
    			new ScenarioExecutionException("Unexpected error when executing scenario",t).printStackTrace();
    			ctx.sendMessage("Execution Exception when executing scenario: "+t.getMessage()+" see logs for more detail");
    			setStatus((RunStatus.STOPPED));
    			//ctx.scene().clear(this);
	    		//sendCachedSence();
    			break;
    		}
	    	if(getScenario()==null||nodeNum>=getScenario().pieces().size()) {
	    		setStatus((RunStatus.STOPPED));
	    		//ctx.scene().clear(this);
	    		//sendCachedSence();
	    		this.tryPopCallStack(ctx);
	    		return;
    		}
    	}
    	if(isRunning())
    		setStatus((RunStatus.STOPPED));
    }
	
	/**
	 * Checks if is running.
	 *
	 * @return true, if is running
	 */
	public boolean isRunning() {
		return getStatus()==RunStatus.RUNNING;
	}

    /**
     * Run scheduled.
     */
    protected void runScheduled(ScenarioContext ctx) {
    	if(getStatus().shouldPause) {
    		ScenarioTarget nxt=toExecute.pollFirst();
    		if(nxt!=null) {
    			jump(ctx,nxt);
    		}
    	}
    }
    /**
	 * Break any waits and stop, Execute on next tick.
	 * <p>
	 * 
	 * */
    protected void run() {
    	setStatus((RunStatus.RUNNING));
    }


    /**
     * Run.
     *
     * @param sp the sp
     */
    public void run(Scenario sp) {
		this.setScenario(sp);
		nodeNum=0;
		run();
	}

    /**
     * Stop.
     */
    public void stop() {
    	this.sp=null;
    	nodeNum=0;
    	setStatus(RunStatus.STOPPED);
    }
    protected void runCodeExecutionLoop(ScenarioContext ctx) {
		while(isRunning()){
    		runCode(ctx);
    		runScheduled(ctx);
		}
    }
	/**
	 * Tick.
	 */
	public void tick(ScenarioContext ctx) {
		runIfNeeded(ctx);
    	//detect triggers
		tickTrigger(ctx);
		if(tickWaitIfNeeded())
			return;
    	runScheduled(ctx);
    }
	public boolean tickWaitIfNeeded() {
		if(getStatus()==RunStatus.WAITTIMER) {
    		if(tickWait()) {
    			run();
    			return true;
    		}
    	}
		return false;
	}
	@Override
	public void appendLiteral(String text) {
		scene.appendLiteral(text);
	}
	public void tickMain(ScenarioContext ctx) {
		runIfNeeded(ctx);
		tickWaitIfNeeded();
	}
	public void runIfNeeded(ScenarioContext ctx) {
		if(getStatus()==RunStatus.RUNNING) {
			runCodeExecutionLoop(ctx);
		}
	}
	public void tickTrigger(ScenarioContext ctx) {
    	for(TriggerTarget t:triggers) {
    		if(t.trigger().test(ctx)) {
    			if(t.trigger().use()) {
    				if(t.trigger().isAsync())
    					addToQueue(t.target());
					else
						jump(ctx,t.target());
    			}
    		}
    	}
    	triggers.removeIf(t->!t.trigger().canUse());
    	
	}
	
	/**
	 * Adds the trigger.
	 *
	 * @param trig the trig
	 * @param targ the targ
	 */
	@Override
	public void addTrigger(IScenarioTrigger trig,ScenarioTarget targ) {
		triggers.add(new TriggerTarget(trig,targ));
	}
    

	
	/** The Constant cmd. */
	private static final Pattern cmd=Pattern.compile("\\@([^@$;]+);");
	
	/** The Constant cmd2. */
	private static final Pattern cmd2=Pattern.compile("\\$([^@$;]+);");
	
	/**
	 * Call command.
	 *
	 * @param name the name
	 * @param params the params
	 */
	public void callCommand(ScenarioCommandContext ctx,String name,Map<String,String> params) {
		name=name.toLowerCase();
		Map<String,String> cparams=new HashMap<>();
		for(Entry<String, String> i:params.entrySet()) {
			String val=i.getValue();
			Matcher m=cmd.matcher(val);
	        StringBuffer sb=new StringBuffer();
            while(m.find()){
                String replacement = ""+eval(ctx.context(),m.group(1));
                m.appendReplacement(sb, replacement);
            }
            m.appendTail(sb);
			m=cmd2.matcher(sb.toString());
			sb=new StringBuffer();
            while(m.find()){
                String replacement = ""+eval(ctx.context(),m.group(1));
                m.appendReplacement(sb, replacement);
            }
            m.appendTail(sb);
			cparams.put(i.getKey(), sb.toString());
		}
		if(ctx.context().macros.containsKey(name)) {
			CompoundTag mp=new CompoundTag();
			for(Entry<String, String> e:cparams.entrySet()) {
				mp.putString(e.getKey(), e.getValue());
			}
			ctx.context().varData.getExtraData().put("mp", mp);
			addCallStack(null);
			jump(ctx.context(),ctx.context().macros.get(name));
		}else
			FHScenario.callCommand(name, ctx, cparams);
			
	}
	
    
    /**
     * Eval.
     *
     * @param exp the exp
     * @return the double
     */
    public double eval(ScenarioContext ctx,String exp) {
        com.teammoeg.chorda.math.evaluator.Node n= Evaluator.eval(exp);
        //System.out.println(n);
        return n.eval(ctx.varData);
    }
    @Override
	public void addWait( int time) {
		waiting += time;
		setStatus(RunStatus.WAITTIMER);
	}

	public boolean tickWait() {
		if (waiting > 0) {
			waiting--;

            return waiting <= 0;
		}
		return false;
	}
	public void stopWait() {
		if(getStatus()==RunStatus.WAITTIMER) {
			waiting=0;
			run();
		}
	}
	@Override
	public void setStatus(RunStatus status) {
		this.status=status;
	}
	@Override
	public void notifyClientResponse(ScenarioContext ctx,boolean isSkip,int status) {
		
		if(this.getStatus()==RunStatus.WAITCLIENT) {
			scene.notifyClientResponse(ctx,this, status);
			run();
		}else if(this.getStatus()==RunStatus.WAITTIMER&&isSkip) {
			scene.notifyClientResponse(ctx,this, status);
			this.stopWait();
			run();
		}
		
    }
	public void restoreLocation(ScenarioContext ctx) {
		if(currentLabel!=null)
			this.jump(ctx, currentLabel);
	}


	@Override
	public Scene scene() {
		return scene;
	}

	@Override
	public void waitClient() {
		this.setStatus(RunStatus.WAITCLIENT);
	}

	@Override
	public void setCurrentLabel(String target) {
		if(this.sp!=null)
			currentLabel=new ExecuteTarget(this.sp.name(),target);
	}

	@Override
	public int getRunId() {
		return 0;
	}
}
