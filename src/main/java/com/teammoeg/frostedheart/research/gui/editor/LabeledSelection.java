package com.teammoeg.frostedheart.research.gui.editor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.SimpleTextButton;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import net.minecraft.util.text.StringTextComponent;

public class LabeledSelection<R> extends LabeledPane<Button> {
	public static LabeledSelection<Boolean> createBool(Panel p,String lab,boolean val){
		return new LabeledSelection<>(p,lab,val,Arrays.asList(true,false),String::valueOf);
	}
	List<R> objs;
	Function<R,String> tostr;
	
	int sel;
	public LabeledSelection(Panel panel, String lab,R val,List<R> aobjs,Function<R,String> atostr) {
		super(panel, lab);
		this.objs=aobjs;
		this.tostr=atostr;
		sel=objs.indexOf(val);
		obj=new SimpleTextButton(this,new StringTextComponent(tostr.apply(val)),Icon.EMPTY) {

			@Override
			public void onClicked(MouseButton arg0) {
				sel++;
				if(sel>=objs.size())
					sel=0;
				this.setTitle(new StringTextComponent(tostr.apply(objs.get(sel))));
				refreshWidgets();
				onChange(objs.get(sel));
			}

			@Override
			public void addMouseOverText(TooltipList list) {
				int i=0;
				for(R elm:objs) {
					if(i==sel)
						list.add(new StringTextComponent("->"+tostr.apply(elm)));
					else
						list.add(new StringTextComponent(tostr.apply(elm)));
					i++;
				}
			}
			
		};
		
		obj.setHeight(20);
	}
	public LabeledSelection(Panel panel, String lab,R val,R[] aobjs,Function<R,String> atostr) {
		this(panel, lab,val,Arrays.asList(aobjs),atostr);
	}
	public LabeledSelection(Panel panel, String lab,R val,Collection<R> aobjs,Function<R,String> atostr) {
		this(panel, lab,val,new ArrayList<>(aobjs),atostr);
	}
	public void onChange(R current) {}
	public R getSelection() {
		return objs.get(sel);
	}
	public void setSelection(R val) {
		sel=objs.indexOf(val);
	}
}
