package com.teammoeg.frostedheart.scenario.runner;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import com.teammoeg.frostedheart.scenario.parser.Scenario;
import com.teammoeg.frostedheart.scenario.runner.target.ExecuteStackElement;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraftforge.common.util.Constants;

public class QuestData {
	ParagraphData paragraph;
	boolean isWaiting;
	LinkedList<ExecuteStackElement> callStack=new LinkedList<>();
	String chapterDisplay;
	String questDisplay;
	QuestNamespace currentQuest=new QuestNamespace();
    private final SceneHandler paraData;
    public QuestData(ScenarioConductor paraData) {
		super();
		this.paraData=new SceneHandler(paraData);
	}
    public CompoundNBT save() {
    	CompoundNBT nbt=new CompoundNBT();
    	nbt.putString("pname", paragraph.getName());
    	nbt.putInt("pn", paragraph.getParagraphNum());
    	ListNBT css=new ListNBT();
    	for(ExecuteStackElement cs:callStack) {
    		css.add(cs.save());
    	}
    	nbt.put("callStack", css);
    	nbt.putString("chapName", chapterDisplay);
    	nbt.putString("questName", questDisplay);
    	nbt.putString("chapter", currentQuest.chapter);
    	nbt.putString("quest", currentQuest.quest);
    	nbt.put("scene", paraData.save());
    	return nbt;
    }
    public void load(CompoundNBT nbt) {
    	paragraph=new ParagraphData(nbt.getString("pname"),nbt.getInt("pn"));
    	ListNBT css=nbt.getList("callStack", Constants.NBT.TAG_COMPOUND);
    	for(INBT n:css) {
    		callStack.add(new ExecuteStackElement((CompoundNBT) n));
    	}
    	chapterDisplay=nbt.getString("chapName");
    	questDisplay=nbt.getString("questName");
    	currentQuest.chapter=nbt.getString("chapter");
    	currentQuest.quest=nbt.getString("quest");
    	paraData.load(nbt.getCompound("scene"));
    	
    }
	public void newParagraph(Scenario sp,int pn) {
		paragraph.setParagraphNum(pn);
		paragraph.setScenario(sp);
		getScene().clear();
    }
	public SceneHandler getScene() {
		return paraData;
	}
	public Consumer<ScenarioConductor> getExecutionPoint(){
		return new QuestExecuteTarget(paragraph.getScenario(),paragraph.getParagraphNum(),currentQuest.asImmutable());
	}
}
