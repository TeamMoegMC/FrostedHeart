package com.teammoeg.frostedheart.scenario.runner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.research.data.FHResearchDataManager;
import com.teammoeg.frostedheart.scenario.FHScenario;
import com.teammoeg.frostedheart.scenario.ScenarioExecutionException;
import com.teammoeg.frostedheart.scenario.parser.Node;
import com.teammoeg.frostedheart.scenario.parser.Scenario;
import com.teammoeg.frostedheart.scenario.runner.target.ExecuteStackElement;
import com.teammoeg.frostedheart.scenario.runner.target.ExecuteTarget;
import com.teammoeg.frostedheart.scenario.runner.target.IScenarioTarget;
import com.teammoeg.frostedheart.util.evaluator.Evaluator;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;

public class ScenarioVM implements IScenarioConductor{
	protected Scenario sp;//current scenario
	protected int nodeNum=0;//Program register
	protected ScenarioVariables varData=new ScenarioVariables();
	protected LinkedList<IScenarioTarget> toExecute=new LinkedList<>();//Actions appended by trigger and awaiting execution
	protected List<IScenarioTrigger> triggers=new ArrayList<>();
	protected Map<String,ExecuteStackElement> macros=new HashMap<>();
	protected RunStatus status=RunStatus.STOPPED;
	protected LinkedList<ExecuteStackElement> callStack=new LinkedList<>();
	protected UUID player;
	protected boolean isSkip;
	protected int clientStatus;
	//Conducting and scheduling data 
	protected boolean isConducting;
	protected Scene scene;
	protected boolean clearAfterClick;
	public ScenarioVM() {
		// TODO Auto-generated constructor stub
	}
	@Override
	public Scenario getScenario() {
		return sp;
	}
	@Override
	public void setScenario(Scenario sp) {
		this.sp = sp;
	}
	@Override
	public void setNodeNum(int num) {
		nodeNum = num;
	}
	public RunStatus getStatus() {
		return status;
	}
	public void setStatus(RunStatus status) {
		this.status = status;
	}
	@Override
	public int getNodeNum() {
        return nodeNum;
    }

	public ServerPlayerEntity getPlayer() {
        return FHResearchDataManager.server.getPlayerList().getPlayerByUUID(player);
    }
	public boolean isOfflined() {
		return FHResearchDataManager.server.getPlayerList().getPlayerByUUID(player)==null;
	}
	@Override
    public String getLang() {
    	return getPlayer().getLanguage();
    }
	@Override
	public void sendMessage(String s) {
		getPlayer().sendStatusMessage(GuiUtils.str(s), false);
	}
	public void jump(String scenario,String label) {
		if(scenario==null)
			jump(new ExecuteTarget(this,scenario,label));
		else
			jump(new ExecuteTarget(getScenario(),label));
	}
	public LinkedList<ExecuteStackElement> getCallStack() {
		return callStack;
	}
	public void addMacro(String name) {
		macros.put(name.toLowerCase(), getCurrentPosition().next());
	}
	public void queue(IScenarioTarget questExecuteTarget) {
		//getCurrentAct().queue(questExecuteTarget);
		toExecute.add(questExecuteTarget);
	}
	public void jump(IScenarioTarget nxt) {
		nxt.accept(this);
		run();

	}
    public void popCallStack() {
		if(getCallStack().isEmpty()) {
			throw new ScenarioExecutionException("Invalid return at "+getScenario().name);
		}
		jump(getCallStack().pollLast());
	}
	public void addCallStack() {
		getCallStack().add(getCurrentPosition());
	}
	public void call(IScenarioTarget target) {
		addCallStack();
		jump(target);
	}
	public ExecuteStackElement getCurrentPosition() {
    	return new ExecuteStackElement(sp,nodeNum);
    }
	public void sendCachedSence() {
		getScene().sendCurrent();
	}
    public void stopWait() {
		getScene().stopWait();
	}
	protected void runCode() {
    	clearAfterClick=false;
    	
    	
    	while(isRunning()&&getScenario()!=null&&nodeNum<getScenario().pieces.size()) {
    		Node node=getScenario().pieces.get(nodeNum++);
    		try {
    			getScene().appendLiteral(node.getLiteral(this));
    			node.run(this);
    		}catch(Throwable t) {
    			new ScenarioExecutionException("Unexpected error when executing scenario",t).printStackTrace();
    			this.sendMessage("Execution Exception when executing scenario: "+t.getMessage()+" see logs for more detail");
    			setStatus((RunStatus.STOPPED));
	    		getScene().clear();
	    		sendCachedSence();
    			break;
    		}
	    	if(getScenario()==null||nodeNum>=getScenario().pieces.size()) {
	    		
	    		setStatus((RunStatus.STOPPED));
	    		getScene().clear();
	    		sendCachedSence();
	    		return;
    		}
    	}
    	if(isRunning())
    		setStatus((RunStatus.STOPPED));
		
    }
	public boolean isRunning() {
		return getStatus()==RunStatus.RUNNING;
	}

    protected void runScheduled() {
    	if(getStatus().shouldPause) {
    		IScenarioTarget nxt=toExecute.pollFirst();
    		if(nxt!=null) {
    			//globalScope();
    			//paragraph(-1);
    			jump(nxt);
    		}
    	}
    }
    /**
	 * Break any waits and stop, Execute now.
	 * 
	 * 
	 * */
    protected void run() {
    	setStatus((RunStatus.RUNNING));
    	if(isConducting)return;
    	try {
    		isConducting=true;
    		
    		while(isRunning()){
	    		runCode();
	    		runScheduled();
    		}
    	}finally {
    		isConducting=false;
    	}
    }


    public void run(Scenario sp) {
		this.setScenario(sp);
		nodeNum=0;
		run();
	}

    public void stop() {
    	nodeNum=getScenario().pieces.size();
    	setStatus(RunStatus.STOPPED);
    }

	public void tick() {    	
    	//detect triggers
		if(getStatus()==RunStatus.RUNNING) {
			run();
		}
    	for(IScenarioTrigger t:triggers) {
    		if(t.test(this)) {
    			if(t.use()) {
    				this.queue(t);
    			}
    		}
    	}
    	triggers.removeIf(t->!t.canUse());
    	if(getStatus()==RunStatus.WAITTIMER) {
    		if(getScene().tickWait()) {
    			
    			run();
    			return;
    		}
    	}
    	//Execute Queued actions
    	runScheduled();
    }
	public void addTrigger(IScenarioTrigger trig) {
		triggers.add(trig);
	}
	public Scene getScene() {
    	return scene;
    }
	public void newLine() {
		getScene().sendNewLine();
	}
	protected void doParagraph() {
		getScene().clear();
	}
    public void paragraph(int pn) {
    	if(getScene().shouldWaitClient()) {
    		getScene().waitClientIfNeeded();
    		clearAfterClick=true;
    		getScene().sendCurrent();
    	}else doParagraph();
	}
	public int getClientStatus() {
		return clientStatus;
	}
	public ScenarioVariables getVaribles() {
    	return varData;
    }
    public void clearLink() {
    	getScene().clearLink();
	}
    public String createLink(String id,String scenario,String label) {
		if(id==null||getScene().getLinks().containsKey(id)) {
			id=UUID.randomUUID().toString();
		}
		getScene().getLinks().put(id, new ExecuteTarget(this,scenario,label));
		getScene().markChatboxDirty();
		return id;
	}
	private static final Pattern cmd=Pattern.compile("\\@([^@$;]+);");
	private static final Pattern cmd2=Pattern.compile("\\$([^@$;]+);");
	public void callCommand(String name,Map<String,String> params) {
		name=name.toLowerCase();
		Map<String,String> cparams=new HashMap<>();
		for(Entry<String, String> i:params.entrySet()) {
			String val=i.getValue();
			Matcher m=cmd.matcher(val);
	        StringBuffer sb=new StringBuffer();
            while(m.find()){
                String replacement = ""+eval(m.group(1));
                m.appendReplacement(sb, replacement);
            }
            m.appendTail(sb);
			m=cmd2.matcher(sb.toString());
			sb=new StringBuffer();
            while(m.find()){
                String replacement = ""+eval(m.group(1));
                m.appendReplacement(sb, replacement);
            }
            m.appendTail(sb);
			cparams.put(i.getKey(), sb.toString());
		}
		if(macros.containsKey(name)) {
			CompoundNBT mp=new CompoundNBT();
			for(Entry<String, String> e:cparams.entrySet()) {
				mp.putString(e.getKey(), e.getValue());
			}
			varData.getExtraData().put("mp", mp);
			call(macros.get(name));
		}else
			FHScenario.callCommand(name, this, cparams);
	}
	public void call(String scenario, String label) {
		call(new ExecuteTarget(this,scenario,label));
	}
    public double eval(String exp) {
        com.teammoeg.frostedheart.util.evaluator.Node n= Evaluator.eval(exp);
        //System.out.println(n);
        return n.eval(getVaribles());
    }
}
