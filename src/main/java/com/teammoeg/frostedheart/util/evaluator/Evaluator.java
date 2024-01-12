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

package com.teammoeg.frostedheart.util.evaluator;

import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class Evaluator {
	public static Map<String, Function<Node[], Node>> functions = new HashMap<>();
	public static Map<String, Double> constants = new HashMap<>();
	int pos = -1, ch;
	public final String str;
	
	
	@Override
	public String toString() {
		return str;
	}

	public Evaluator(String str) {
		this.str = str;
	}

	void nextChar() {
		ch = (++pos < str.length()) ? str.charAt(pos) : -1;
	}

	boolean eat(int charToEat) {
		while (ch == ' ')
			nextChar();
		if (ch == charToEat) {
			nextChar();
			return true;
		}
		return false;
	}

	public Node parse() {
		try {
			nextChar();
			Node x = parseExpression();

			if (pos < str.length())
				throw new RuntimeException("Unexpected: " + (char) ch);
			return x;
		} catch (Exception ex) {
			throw new RuntimeException("Exception raised at " + pos + ": " + ex.getMessage(), ex);
		}

	}

	public static Node eval(String exp) {
		return new Evaluator(exp).parse().simplify();
	}
	// Grammar:
	// expression = term | expression `+` term | expression `-` term
	// term = factor | term `*` factor | term `/` factor | term `%` factor | term '\' factor
	// factor = `+` factor | `-` factor | `(` expression `)` | number |
	// variantName | constant | functionName `(` param `)` | factor `^` factor
	// param = expression | expression `,` param

	Node parseExpression() {
		Node x = parseTerm();
		for (;;) {
			if (eat('+'))
				x = new BiCalcNode(x, parseTerm(), BiCalcNode.add); // addition
			else if (eat('-'))
				x = new BiCalcNode(x, parseTerm(), BiCalcNode.min); // subtraction
			else
				return x;
		}
	}

	Node parseTerm() {
		Node x = parseFactor();
		for (;;) {
			if (eat('*'))
				x = new BiCalcNode(x, parseFactor(), BiCalcNode.mul); // multiplication
			else if (eat('/'))
				x = new BiCalcNode(x, parseFactor(), BiCalcNode.div); // division
			else if (eat('%'))
				x = new BiCalcNode(x, parseFactor(), BiCalcNode.mod); // modular
			else if (eat('\\'))
				x = new BiCalcNode(x, parseFactor(), BiCalcNode.sdiv); // secure division
			else
				return x;
		}
	}

	Node parseFactor() {
		if (eat('+'))
			return parseFactor(); // unary plus
		if (eat('-'))
			return new CalcNode(parseFactor(), "-", v -> -v); // unary minus
		Node x = null;
		int startPos = this.pos;
		if (eat('(')) { // parentheses
			x = parseExpression();
			eat(')');
		} else if ((ch >= '0' && ch <= '9') || ch == '.') { // numbers
			while ((ch >= '0' && ch <= '9') || ch == '.')
				nextChar();
			x = new ConstNode(Double.parseDouble(str.substring(startPos, this.pos)));
		} else if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')) { // functions
			while ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9') || ch=='.')
				nextChar();
			String func = str.substring(startPos, this.pos);
			// System.out.println(String.valueOf(Character.toString(str.charAt(pos))));
			// this.pos--;
			List<Node> param = new ArrayList<>();
			if (eat('(')) {
				// this.pos--;
				// this.pos--;
				do {
					param.add(parseExpression());
				} while (eat(','));
				if (!eat(')')) {
					throw new RuntimeException("Expect ')' at end of func: " + func);
				}
				// System.out.println(str.substring(pos));
				// System.out.println(eat(')'));
				// eat(')');
			}
			if (!param.isEmpty()) {
				Function<Node[], Node> prod = functions.get(func);
				if (prod == null)
					throw new RuntimeException("Unknown function: " + func);
				x = prod.apply(param.toArray(new Node[0]));

			} else {
				Double d = constants.get(func);
				if (d != null)
					x = new ConstNode(d);
				else
					x = new VarNode(func);
			}
		} else {
			throw new RuntimeException("Unexpected: " + (char) ch);
		}

		if (eat('^'))
			x = new BiCalcNode(x, parseFactor(), BiCalcNode.pow); // exponentiation
		
		return x;
	}

	static {
		functions.put("sqrt", x -> new FuncCallNode(x, "sqrt",1, v -> Math.sqrt(v[0]), true));
		functions.put("sin", x -> new FuncCallNode(x, "sin",1, v -> Math.sin(v[0]), true));
		functions.put("cos", x -> new FuncCallNode(x, "cos",1, v -> Math.cos(v[0]), true));
		functions.put("tan", x -> new FuncCallNode(x, "tan",1, v -> Math.tan(v[0]), true));
		functions.put("sinf", x -> new FuncCallNode(x, "sinf",1, v -> (double) MathHelper.sin((float) v[0]), true));
		functions.put("cosf", x -> new FuncCallNode(x, "cosf",1, v -> (double) MathHelper.cos((float) v[0]), true));
		functions.put("rad", x -> new FuncCallNode(x, "rad",1, v -> Math.toRadians(v[0]), true));
		functions.put("deg", x -> new FuncCallNode(x, "deg",1, v -> Math.toDegrees(v[0]), true));
		functions.put("abs", x -> new FuncCallNode(x, "abs",1, v -> Math.abs(v[0]), true));
		functions.put("ceil", x -> new FuncCallNode(x, "ceil",1, v -> Math.ceil(v[0]), true));
		functions.put("floor", x -> new FuncCallNode(x, "floor",1, v -> Math.floor(v[0]), true));
		functions.put("round", x -> new FuncCallNode(x, "round",1, v -> (double) Math.round(v[0]), true));
		functions.put("log", x -> new FuncCallNode(x, "log",1, v -> Math.log(v[0]), true));
		functions.put("log10", x -> new FuncCallNode(x, "log10",1, v -> Math.log10(v[0]), true));
		functions.put("exp", x -> new FuncCallNode(x, "exp",1, v -> Math.exp(v[0]), true));
		functions.put("max", x -> new FuncCallNode(x, "max",2, v -> Math.max(v[0], v[1]), true));
		functions.put("min", x -> new FuncCallNode(x, "min",2, v -> Math.min(v[0], v[1]), true));
		functions.put("if", x -> new FuncCallNode(x, "if",3, v -> v[0]>0?v[1]:v[2], true));
		functions.put("evl", x -> new FuncCallNode(x, "evl",2, v -> Double.isFinite(v[0])?v[0]:v[1], true));
		constants.put("PI", Math.PI);
		constants.put("E", Math.E);
	}

	public static void main(String[] args) {
		System.out.println(eval("sin(v+1+2+3*5*n)*(v*2*5*8*1)*cos(90)+PI"));
	}
}