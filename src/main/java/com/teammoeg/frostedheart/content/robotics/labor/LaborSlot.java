package com.teammoeg.frostedheart.content.robotics.labor;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import com.teammoeg.chorda.menu.CBaseMenu;
import com.teammoeg.chorda.menu.CCustomMenuSlot;
import com.teammoeg.chorda.menu.CCustomMenuSlot.CDataSlot;

import net.minecraft.core.GlobalPos;

public class LaborSlot {
	/** 某一个劳工类型在菜单里的一组数据槽。 */
	public static class TypePool{
		public CDataSlot<Integer> currentLevel;
		public CDataSlot<Integer> setLevel;
	
		public CDataSlot<Integer> totalLabour;
		public CDataSlot<Integer> currentLabour;
		public CDataSlot<Integer> desiredLabour;
		public LaborType type;
		public TypePool(CBaseMenu menu, LaborType labor) {
			currentLevel = CCustomMenuSlot.SLOT_INT.create(menu);
			setLevel = CCustomMenuSlot.SLOT_INT.create(menu);

			totalLabour = CCustomMenuSlot.SLOT_INT.create(menu);
			currentLabour = CCustomMenuSlot.SLOT_INT.create(menu);
			desiredLabour = CCustomMenuSlot.SLOT_INT.create(menu);
			type=labor;
		}
		public void bind(MachineLevelManager data,GlobalPos pos) {

			setLevel.bind(()->data.getMachine(pos).getDesiredLevel(type));
			currentLevel.bind(()->data.getMachine(pos).getActualLevel(type));
			totalLabour.bind(()->data.getTotalPool(type));
			currentLabour.bind(()->data.getMachine(pos).getActualCost(type));
			desiredLabour.bind(()->data.getMachine(pos).getDesiredCost(type));
		}
	}
	MachineType type;
	Map<LaborType,TypePool> pools=new LinkedHashMap<>();
	public LaborSlot(CBaseMenu menu, MachineType type) {
		this.type=type;
		for(LaborType labor:type.getLaborTypes()) {
			pools.put(labor, new TypePool(menu,labor));
		}
	}
	public void bind(MachineLevelManager man,GlobalPos pos) {

		for(TypePool pool:pools.values()) {
			pool.bind(man, pos);
		}
	}
	/**
	 * 直接按劳工类型取得它在内部池里的槽组，不必遍历 {@link #bind} 用的池集合。
	 *
	 * <pre>{@code
	 * CDataSlot<Integer> level = laborSlot.getSlot(LaborTypes.GENERAL).currentLevel;
	 * int value = laborSlot.getSlot(LaborTypes.GENERAL).currentLevel.getValue();
	 * }</pre>
	 *
	 * @param labor 劳工类型
	 * @return 该劳工类型的槽组；本 LaborSlot 未为该类型建立槽时为 null
	 */
	public TypePool getSlot(LaborType labor) {
		return pools.get(labor);
	}
	/** 该 LaborSlot 绑定的机器类型。 */
	public MachineType getType() {
		return type;
	}
	/** 所有已建立槽的劳工类型对应的槽组，顺序与劳工类型的声明顺序一致。 */
	public Collection<TypePool> getSlots() {
		return pools.values();
	}
	/**
	 * 该劳工类型当前的实际等级。
	 *
	 * <p>以下这几个 getter 都按劳工类型直接取整数值（未建立该类型的槽、或槽取不到值时返回 0）；
	 * 需要槽对象本身、例如继续绑定到 UI 控件时，请用 {@link #getSlot(LaborType)}。</p>
	 */
	public int getCurrentLevel(LaborType labor) {
		TypePool pool = pools.get(labor);
		return pool == null ? 0 : value(pool.currentLevel);
	}
	/** 该劳工类型当前的期望等级（玩家设置的等级）。 */
	public int getSetLevel(LaborType labor) {
		TypePool pool = pools.get(labor);
		return pool == null ? 0 : value(pool.setLevel);
	}
	/** 该劳工类型所在池的总点数。 */
	public int getTotalLabour(LaborType labor) {
		TypePool pool = pools.get(labor);
		return pool == null ? 0 : value(pool.totalLabour);
	}
	/** 该劳工类型已分配的点数。 */
	public int getCurrentLabour(LaborType labor) {
		TypePool pool = pools.get(labor);
		return pool == null ? 0 : value(pool.currentLabour);
	}
	/** 该劳工类型期望的点数。 */
	public int getDesiredLabour(LaborType labor) {
		TypePool pool = pools.get(labor);
		return pool == null ? 0 : value(pool.desiredLabour);
	}

	/** 读取槽的整数值；槽尚未取到值时按 0 处理。 */
	private static int value(CDataSlot<Integer> slot) {
		Integer value = slot.getValue();
		return value == null ? 0 : value;
	}
}
