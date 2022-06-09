package com.teammoeg.frostedheart.research.clues;

import java.util.UUID;
import java.util.function.Consumer;
import com.teammoeg.frostedheart.research.gui.editor.BaseEditDialog;
import com.teammoeg.frostedheart.research.gui.editor.EditUtils;
import com.teammoeg.frostedheart.research.gui.editor.LabeledSelection;
import com.teammoeg.frostedheart.research.gui.editor.LabeledTextBox;
import com.teammoeg.frostedheart.research.gui.editor.LabeledTextBoxAndBtn;
import dev.ftb.mods.ftblibrary.ui.Widget;

public abstract class ClueEditor<T extends Clue> extends BaseEditDialog{

	String lbl;
	T e;
	Consumer<T> cb;
	protected LabeledTextBoxAndBtn nonce;
	protected LabeledTextBox name;
	protected LabeledTextBox desc;
	protected LabeledTextBox hint;
	protected LabeledTextBox cont;
	public ClueEditor(Widget panel, String lbl,T e, Consumer<T> cb) {
		super(panel);
		
		this.lbl = lbl;
		
		
		if(e==null) {
			e=createClue();	
		}
		this.e = e;
		nonce=new LabeledTextBoxAndBtn(this,"nonce",e!=null?e.getNonce():"","Random",t->t.accept(Long.toHexString(UUID.randomUUID().getMostSignificantBits())));
		name=new LabeledTextBox(this,"Name",e.name);
		desc=new LabeledTextBox(this,"Description",e.desc);
		hint=new LabeledTextBox(this,"Hint",e.hint);
		cont=new LabeledTextBox(this,"Contribution(%)",Float.toString(e.contribution*100));
		this.cb = cb;
	}
	public abstract T createClue();
	@Override
	public void onClose() {
		e.name=name.getText();
		
		if(e.getRId()!=0){
			e.setNewId(nonce.getText());
		}else {
			e.nonce=nonce.getText();
		}
		cb.accept(e);
		
	}

	@Override
	public void addWidgets() {
		add(EditUtils.getTitle(this, lbl));
		add(nonce);
		add(name);
		add(desc);
		add(hint);
		add(cont);
	}
	private static class Custom extends ClueEditor<CustomClue>{

		public Custom(Widget panel, String lbl, CustomClue e, Consumer<CustomClue> cb) {
			super(panel, lbl, e, cb);
		}

		@Override
		public CustomClue createClue() {
			return new CustomClue();
		}
	}
	private static abstract class Listener<U extends ListenerClue> extends ClueEditor<U>{
		LabeledSelection<Boolean> aa;
		public Listener(Widget panel, String lbl, U e, Consumer<U> cb) {
			super(panel, lbl, e, cb);
			aa=LabeledSelection.createBool(this,"Listen when not active",this.e.alwaysOn);
		}

		@Override
		public void addWidgets() {
			super.addWidgets();
			add(aa);
		}

		@Override
		public void onClose() {
			e.alwaysOn=aa.getSelection();
			super.onClose();
		}
		

	}
	private static class Advancement extends Listener<AdvancementClue>{

		public Advancement(Widget panel, String lbl, AdvancementClue e, Consumer<AdvancementClue> cb) {
			super(panel, lbl, e, cb);
		}

		@Override
		public AdvancementClue createClue() {
			return new AdvancementClue();
		}
	}
	private static class Kill extends Listener<KillClue>{

		public Kill(Widget panel, String lbl, KillClue e, Consumer<KillClue> cb) {
			super(panel, lbl, e, cb);
		}

		@Override
		public KillClue createClue() {
			return null;
		}


	}
}
