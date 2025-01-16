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

class TermNode implements Node {
    List<Node> positive = new ArrayList<>();
    List<Node> negative = new ArrayList<>();

    double primaries = 1;

    public TermNode() {
        super();
    }

    public TermNode(boolean type, Node pos, Node pos2) {
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
        double rslt = 1;
        for (Node n : positive)
            rslt *= n.eval(env);
        for (Node n : negative)
            rslt /= n.eval(env);
        return rslt;
    }

    @Override
    public boolean isPrimary() {
        return false;
    }

    @Override
    public Node simplify() {
        primaries = 1;
        positive.replaceAll(Node::simplify);
        List<Node> pcopy = new ArrayList<>(positive);
        for (Node n : pcopy) {//combine
            if (n instanceof TermNode) {
                positive.remove(n);
                positive.addAll(((TermNode) n).positive);
                negative.addAll(((TermNode) n).negative);
            }
        }
        negative.replaceAll(Node::simplify);
        List<Node> ncopy = new ArrayList<>(negative);
        for (Node n : ncopy) {
            if (n instanceof TermNode) {
                negative.remove(n);
                positive.addAll(((TermNode) n).negative);
                negative.addAll(((TermNode) n).positive);
            }
        }
        List<Node> primaryExprp = new ArrayList<>();
        positive.removeIf(s -> {//calc all primaries
            if (s.isPrimary()) {
                primaries *= s.eval(NullEnvironment.INSTANCE);
                return true;
            } else if (!(s instanceof ExprNode)) {
                primaryExprp.add(s);
                return true;
            }
            return false;
        });

        negative.removeIf(s -> {
            if (s.isPrimary()) {
                primaries /= s.eval(NullEnvironment.INSTANCE);
                return true;
            }
            return false;
        });
        if (positive.isEmpty()) {
            if (primaryExprp.isEmpty())
                return new ConstNode(primaries);
            positive.addAll(primaryExprp);
            if (primaries != 1)
                positive.add(new ConstNode(primaries));
            if (positive.isEmpty())
                positive.add(new ConstNode(1));
            if (positive.size() == 1 && negative.isEmpty())
                return positive.get(0);
            //System.out.println(this.toString());
            return this;
        }
        positive.addAll(primaryExprp);
        if (primaries != 1)
            positive.add(new ConstNode(primaries));
		/*ExprNode en=(ExprNode)positive.remove(0);
		en.positive.replaceAll(nxx->{
			TermNode tn=new TermNode();
			if(primaries!=1)
				tn.positive.add(new ConstNode(primaries));
			tn.positive.addAll(positive);
			if(positive.isEmpty())
				positive.add(new ConstNode(1));
			tn.negative.addAll(negative);
			tn.positive.add(nxx);
			return tn.simplify();
		});
		en.negative.replaceAll(nxx->{
			TermNode tn=new TermNode();
			if(primaries!=1)
				tn.positive.add(new ConstNode(primaries));
			tn.positive.addAll(positive);
			if(positive.isEmpty())
				positive.add(new ConstNode(1));
			tn.negative.addAll(negative);
			tn.positive.add(nxx);
			return tn.simplify();
		});
		positive.clear();
		positive.add(en);*/
        //positive.replaceAll(n->n.simplify());
        if (positive.size() == 1 && negative.isEmpty())
            return positive.get(0);
		/*for(Node t:positive) {
			System.out.println(t.getClass().getSimpleName()+":"+t);
		}
		System.out.println("e:"+this.toString());*/
        return this;
    }

    @Override
    public String toString() {
        String x = "";
		/*System.out.println("st");
		for(Node n:positive) {
			System.out.println(n.getClass().getSimpleName()+":"+n);
		}
		System.out.println("te");*/
        if (!positive.isEmpty()) {
            x = String.join("*", (Iterable<String>) () -> positive.stream().map(n -> "(" + n + ")").iterator());
        } else if (!negative.isEmpty())
            x = "1";
        if (!negative.isEmpty()) {
            x += "/";
            x += String.join("/", (Iterable<String>) () -> negative.stream().map(n -> "(" + n + ")").iterator());
        }
        return x;
    }

}