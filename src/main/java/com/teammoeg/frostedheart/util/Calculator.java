/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedheart.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public class Calculator {
    static class BiCalcNode extends BiNode {
        public static BiFunction<Double, Double, Double> add = (v1, v2) -> v1 + v2;
        public static BiFunction<Double, Double, Double> min = (v1, v2) -> v1 - v2;
        public static BiFunction<Double, Double, Double> mul = (v1, v2) -> v1 * v2;
        public static BiFunction<Double, Double, Double> div = (v1, v2) -> v1 / v2;
        public static BiFunction<Double, Double, Double> pow = (v1, v2) -> Math.pow(v1, v2);
        BiFunction<Double, Double, Double> calc;

        public BiCalcNode(Node left, Node right, BiFunction<Double, Double, Double> calc) {
            super(left, right);
            this.calc = calc;
        }

        @Override
        public double eval(VariantProvider env) {
            return calc.apply(left.eval(env), right.eval(env));
        }

        @Override
        public String toString() {
            return "BiCalcNode [calc=" + calc + ", left=" + left + ", right=" + right + "]";
        }

    }

    static abstract class BiNode implements Node {
        Node left;
        Node right;

        public BiNode(Node left, Node right) {
            super();
            this.left = left;
            this.right = right;
        }

    }

    static class CalcNode implements Node {
        Function<Double, Double> calc;
        Node nested;

        public CalcNode(Node nested, Function<Double, Double> calc) {
            this.calc = calc;
            this.nested = nested;
        }

        @Override
        public double eval(VariantProvider env) {
            return calc.apply(nested.eval(env));
        }

        @Override
        public String toString() {
            return "CalcNode [calc=" + calc + ", nested=" + nested + "]";
        }
    }

    static class ConstNode implements Node {
        double val;

        public ConstNode(double val) {
            this.val = val;
        }

        @Override
        public double eval(VariantProvider env) {
            return val;
        }

        @Override
        public String toString() {
            return "ConstNode [val=" + val + "]";
        }
    }

    public interface Node {
        double eval(VariantProvider env);
    }

    static class TermNode implements Node {
        List<Node> positive = new ArrayList<>();
        List<Node> negative = new ArrayList<>();

        public TermNode() {
        }

        @Override
        public double eval(VariantProvider env) {
            double rslt = 1;
            for (Node n : positive)
                rslt *= n.eval(env);
            for (Node n : negative)
                rslt /= n.eval(env);
            return rslt;
        }

        @Override
        public String toString() {
            String x = "";
            if (!positive.isEmpty()) {
                x = String.join("*", new Iterable<String>() {
                    @Override
                    public Iterator<String> iterator() {
                        return positive.stream().map(n -> n.toString()).iterator();
                    }
                });
            } else if (!negative.isEmpty())
                x = "1";
            if (!negative.isEmpty()) {
                x += "/";
                x += String.join("/", new Iterable<String>() {
                    @Override
                    public Iterator<String> iterator() {
                        return negative.stream().map(n -> n.toString()).iterator();
                    }
                });
            }
            return x;
        }

    }

    static class VarNode implements Node {
        String token;

        public VarNode(String token) {
            this.token = token;
        }

        @Override
        public double eval(VariantProvider env) {
            return env.getOrDefault(token, 0.0);
        }

        @Override
        public String toString() {
            return "VarNode [token=" + token + "]";
        }
    }

    int pos = -1, ch;
    String str;

    public static Node eval(String exp) {
        return new Calculator(exp).parse();
    }
    // Grammar:
    // expression = term | expression `+` term | expression `-` term
    // term = factor | term `*` factor | term `/` factor
    // factor = `+` factor | `-` factor | `(` expression `)`
    // | number | functionName factor | factor `^` factor

    public static void main(String[] args) {
        System.out.println(eval("1+1"));
    }

    public Calculator(String str) {
        this.str = str;
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

    void nextChar() {
        ch = (++pos < str.length()) ? str.charAt(pos) : -1;
    }

    public Node parse() {
        nextChar();
        Node x = parseExpression();
        if (pos < str.length())
            throw new RuntimeException("Unexpected: " + (char) ch);
        return x;
    }

    Node parseExpression() {
        Node x = parseTerm();
        for (; ; ) {
            if (eat('+'))
                x = new BiCalcNode(x, parseTerm(), BiCalcNode.add); // addition
            else if (eat('-'))
                x = new BiCalcNode(x, parseTerm(), BiCalcNode.min); // subtraction
            else
                return x;
        }
    }

    Node parseFactor() {
        if (eat('+'))
            return parseFactor(); // unary plus
        if (eat('-'))
            return new CalcNode(parseFactor(), v -> -v); // unary minus

        Node x = null;
        int startPos = this.pos;
        if (eat('(')) { // parentheses
            x = parseExpression();
            eat(')');
        } else if ((ch >= '0' && ch <= '9') || ch == '.') { // numbers
            while ((ch >= '0' && ch <= '9') || ch == '.')
                nextChar();
            x = new ConstNode(Double.parseDouble(str.substring(startPos, this.pos)));
        } else if (ch >= 'a' && ch <= 'z') { // functions
            while (ch >= 'a' && ch <= 'z')
                nextChar();
            String func = str.substring(startPos, this.pos);
            if (eat('(')) {
                x = parseFactor();
                eat(')');
            }
            if (x != null) {
                if (func.equals("sqrt"))
                    x = new CalcNode(x, v -> Math.sqrt(v));
                else if (func.equals("sin"))
                    x = new CalcNode(x, v -> Math.sin(Math.toRadians(v)));
                else if (func.equals("cos"))
                    x = new CalcNode(x, v -> Math.cos(Math.toRadians(v)));
                else if (func.equals("tan"))
                    x = new CalcNode(x, v -> Math.tan(Math.toRadians(v)));
                else
                    throw new RuntimeException("Unknown function: " + func);
            } else {
                x = new VarNode(func);
            }
        } else {
            throw new RuntimeException("Unexpected: " + (char) ch);
        }

        if (eat('^'))
            x = new BiCalcNode(x, parseFactor(), BiCalcNode.pow); // exponentiation

        return x;
    }

    Node parseTerm() {
        Node x = parseFactor();
        for (; ; ) {
            if (eat('*'))
                x = new BiCalcNode(x, parseFactor(), BiCalcNode.mul); // multiplication
            else if (eat('/'))
                x = new BiCalcNode(x, parseFactor(), BiCalcNode.div); // division
            else
                return x;
        }
    }

}