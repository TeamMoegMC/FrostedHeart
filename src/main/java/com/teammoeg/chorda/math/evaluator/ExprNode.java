/*
 * Copyright (c) 2026 TeamMoeg
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

package com.teammoeg.chorda.math.evaluator;

import java.util.ArrayList;
import java.util.List;

/**
 * 加减法表达式节点，维护正项和负项两个列表来表示多项加减运算。
 * 化简时会合并嵌套的ExprNode并折叠常量项。
 * <p>
 * Addition/subtraction expression node maintaining positive and negative term lists to represent
 * multi-term addition/subtraction. Simplification merges nested ExprNodes and folds constant terms.
 */
class ExprNode implements Node {
    List<Node> positive = new ArrayList<>();
    List<Node> negative = new ArrayList<>();

    double primaries = 0;

    public ExprNode() {
        super();
    }

    /**
     * 构造一个包含两个项的表达式节点。
     * <p>
     * Constructs an expression node with two terms.
     *
     * @param type 如果为true则两项都为正项（加法），否则第二项为负项（减法） / if true both terms are positive (addition), otherwise the second term is negative (subtraction)
     * @param pos 第一个操作数 / the first operand
     * @param pos2 第二个操作数 / the second operand
     */
    public ExprNode(boolean type, Node pos, Node pos2) {
        if (type) {
            positive.add(pos);
            positive.add(pos2);
        } else {
            positive.add(pos);
            negative.add(pos2);
        }
    }

    @Override
    public double eval(IEnvironment env) {
        double rslt = 0;
        for (Node n : positive)
            rslt += n.eval(env);
        for (Node n : negative)
            rslt -= n.eval(env);
        return rslt;
    }

    @Override
    public boolean isPrimary() {
        return false;
    }

    @Override
    public Node simplify() {

        primaries = 0;
        positive.replaceAll(Node::simplify);
        negative.replaceAll(Node::simplify);
        //System.out.println("f:"+this.toString());
        List<Node> pcopy = new ArrayList<>(positive);
        for (Node n : pcopy) {//combine
            if (n instanceof ExprNode) {
                positive.remove(n);
                positive.addAll(((ExprNode) n).positive);
                negative.addAll(((ExprNode) n).negative);
            }
        }


        List<Node> ncopy = new ArrayList<>(negative);
        for (Node n : ncopy) {
            if (n instanceof ExprNode) {
                negative.remove(n);
                positive.addAll(((ExprNode) n).negative);
                negative.addAll(((ExprNode) n).positive);
            }
        }
        positive.removeIf(s -> {//calc all primaries
            if (s.isPrimary()) {
                primaries += s.eval(NullEnvironment.INSTANCE);
                return true;
            }
            return false;
        });

        negative.removeIf(s -> {
            if (s.isPrimary()) {
                primaries -= s.eval(NullEnvironment.INSTANCE);
                return true;
            }
            return false;
        });
		/*for(Node t:positive) {
			System.out.println(t.getClass().getSimpleName()+":"+t);
		}
		System.out.println("c:"+this.toString());*/
        if (positive.isEmpty() && negative.isEmpty())
            return new ConstNode(primaries);
        if (primaries != 0)
            positive.add(new ConstNode(primaries));
		/*for(Node t:positive) {
			System.out.println(t.getClass().getSimpleName()+":"+t);
		}
		System.out.println("t:"+this.toString());*/
        return this;
    }

    @Override
    public String toString() {
        String x = "";
		/*System.out.println("sx");
		for(Node n:positive) {
			System.out.println(n.getClass().getSimpleName()+":"+n);
		}
		System.out.println("xe");*/
        if (!positive.isEmpty()) {
            x = String.join("+", (Iterable<String>) () -> positive.stream().map(Object::toString).iterator());
        } else if (!negative.isEmpty())
            x = "0";
        if (!negative.isEmpty()) {
            x += "-";
            x += String.join("-", (Iterable<String>) () -> negative.stream().map(Object::toString).iterator());
        }
        return x;
    }

}