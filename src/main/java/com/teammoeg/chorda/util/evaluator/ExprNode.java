/*
 * Copyright (c) 2023-2024 TeamMoeg
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

package com.teammoeg.chorda.util.evaluator;

import java.util.ArrayList;
import java.util.List;

class ExprNode implements Node {
    List<Node> positive = new ArrayList<>();
    List<Node> negative = new ArrayList<>();

    double primaries = 0;

    public ExprNode() {
        super();
    }

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