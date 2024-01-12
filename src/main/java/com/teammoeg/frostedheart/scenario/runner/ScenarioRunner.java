package com.teammoeg.frostedheart.scenario.runner;

import java.util.ArrayList;
import java.util.List;

import com.teammoeg.frostedheart.util.evaluator.Evaluator;
import com.teammoeg.frostedheart.util.evaluator.IEnvironment;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;

public class ScenarioRunner implements IEnvironment {
    int paragraphNum;
    int nodeNum;
    ParagraphRunner current;
    ArrayList<CallStackElement> callStack = new ArrayList<>();

    public int getNodeNum() {
        return nodeNum;
    }

    public void setNodeNum(int nodeNum) {
        this.nodeNum = nodeNum;
    }

    public ParagraphRunner getCurrentParagraph() {
        return current;
    }

    public CompoundNBT getExecutionData() {
        return null;
    }

    public ServerPlayerEntity getPlayer() {
        return null;
    }

    public void setPath(String path, INBT val) {
        current.setPath(path, val);
    }

    public void setPathString(String path, String val) {
        current.setPathString(path, val);
    }

    public void setPathNumber(String path, Number val) {
        current.setPathNumber(path, val);
    }

    public INBT evalPath(String path) {
        return current.evalPath(path);
    }

    public Double evalPathDouble(String path) {
        return current.evalPathDouble(path);
    }

    public String evalPathString(String path) {
        return current.evalPathString(path);
    }

    public boolean containsPath(String path) {
        return current.containsPath(path);
    }

    public boolean popCall() {
        if (callStack.size() > 0) {
            nodeNum = callStack.remove(callStack.size() - 1).getTarget();
            return true;
        }
        return false;
    }

    public boolean popCaller(int caller) {
        if (callStack.size() > 0) {
            if (getLast().getCaller() == caller) {
                nodeNum = callStack.remove(callStack.size() - 1).getTarget();
                return true;
            }
        }
        return false;
    }

    public boolean removeCall() {
        if (callStack.size() > 0) {
            callStack.remove(callStack.size() - 1).getTarget();
            return true;
        }
        return false;
    }

    public boolean removeCaller(int caller) {
        if (callStack.size() > 0) {
            if (getLast().getCaller() == caller) {
                callStack.remove(callStack.size() - 1).getTarget();
                return true;
            }
        }
        return false;
    }

    public void pushCall(int caller, int target) {
        callStack.add(new CallStackElement(caller, target));
    }

    public CallStackElement getLast() {
        return callStack.get(callStack.size() - 1);
    }

    public CallStackElement getByCaller(int caller) {
        for (int i = callStack.size() - 1; i >= 0; i--) {
            CallStackElement cur = callStack.get(i);
            if (cur.getCaller() == caller)
                return cur;
        }
        return null;
    }

    public void jump(int target) {
        nodeNum = target;
    }

    @Override
    public double get(String key) {

        return evalPathDouble(key);
    }

    @Override
    public Double getOptional(String key) {
        if (!containsPath(key))
            return null;
        return get(key);
    }

    @Override
    public void set(String key, double v) {
        setPathNumber(key, v);
    }

    public double eval(String exp) {
        return Evaluator.eval(exp).eval(this);
    }
}
