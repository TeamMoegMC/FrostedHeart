/*
 * Copyright (c) 2023 IEEM Trivium Society/khjxiaogu
 *
 * This file is part of Convivium.
 *
 * Convivium is free software: you can redistribute it and/or modify
 * it under the terms of the GNU LESSER GENERAL PUBLIC LICENSE as published by
 * the Free Software Foundation, version 3.
 *
 * Convivium is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU LESSER GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU LESSER GENERAL PUBLIC LICENSE
 * along with Convivium. If not, see <https://www.gnu.org/licenses/>.
 */

package com.teammoeg.frostedheart.util.evaluator;

import java.util.Arrays;
import java.util.function.ToDoubleFunction;

class FuncCallNode implements Node{
	ToDoubleFunction<double[]> calc;
	Node[] nested;
	String name;
	boolean isDeterministic;
	public FuncCallNode(Node[] nested,String name,int paramCount,ToDoubleFunction<double[]> calc,boolean isDeterministic) {
		this.calc = calc;
		this.nested = nested;
		this.name=name;
		this.isDeterministic=isDeterministic;
		if(nested.length!=paramCount) {
			throw new RuntimeException("Bad param count for "+name+" expected "+paramCount+" but got "+nested.length);
		}
	}

	@Override
	public double eval(IEnvironment env) {
		double[] par=new double[nested.length];
		for(int i=0;i<par.length;i++) {
			par[i]=nested[i].eval(env);
		}
		return calc.applyAsDouble(par);
	}
	
	@Override
	public boolean isPrimary() {
		if(!isDeterministic)
			return false;
		boolean isPrime=true;
		for(int i=0;i<nested.length;i++) {
			isPrime&=nested[i].isPrimary();
		}
		return isPrime;
	}
	
	@Override
	public Node simplify() {
		for(int i=0;i<nested.length;i++) {
			nested[i]=nested[i].simplify();
		}
		if(isPrimary()) {
			return new ConstNode(eval(NullEnvironment.INSTANCE));
		}
		return this;
	}

	@Override
	public String toString() {
		
		return name + "(" + Arrays.toString(nested) + ")";
	}
}