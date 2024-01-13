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

package com.teammoeg.frostedheart.scenario.runner;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.teammoeg.frostedheart.FHPacketHandler;
import com.teammoeg.frostedheart.scenario.network.ServerSenarioTextPacket;
import com.teammoeg.frostedheart.scenario.parser.Node;
import com.teammoeg.frostedheart.scenario.parser.ScenarioPiece;
import com.teammoeg.frostedheart.util.evaluator.Evaluator;
import com.teammoeg.frostedheart.util.evaluator.IEnvironment;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.network.PacketDistributor;

public class ScenarioRunner implements IEnvironment {
    int paragraphNum;
    int nodeNum = 0;
    public ParagraphRunner current;
    ArrayList<CallStackElement> callStack = new ArrayList<>();
    ScenarioPiece sp;
    public PlayerEntity player;
    File folder = FMLPaths.CONFIGDIR.get().toFile();
    File rf = new File(folder, "fhresearches");
    public boolean waitClient;
    public boolean isNowait;
    public boolean isSkip;
    int waiting;
    int status;
    StringBuilder currentLiteral;

    public ScenarioRunner(PlayerEntity player) {
        super();
        this.player = player;
    }

    public void addWait(int time) {
        waiting += time;
    }

    public void tick() {
        if (!waitClient)
            if (waiting > 0) {
                waiting--;
                if (waiting <= 0)
                    run();
            }
    }

    public void run(ScenarioPiece sp) {
        this.sp = sp;
        nodeNum = 0;
        paragraphNum = 0;
        current = new ParagraphRunner(new CompoundNBT(), paragraphNum, player);
        for (Node n : sp.pieces) {
            System.out.println(n.getText());
        }
        run();
    }

    public void run() {
        while (!waitClient && waiting <= 0) {
            if (currentLiteral == null)
                currentLiteral = new StringBuilder();
            Node node = sp.pieces.get(nodeNum);
            currentLiteral.append(node.getDisplay(this));
            node.run(this);
            nodeNum++;
            if (nodeNum >= sp.pieces.size())
                break;
        }
    }

    public boolean shouldWaitClient() {
        return currentLiteral != null && currentLiteral.length() != 0;
    }

    public void sendNormal() {
        if (currentLiteral != null && currentLiteral.length() != 0)
            FHPacketHandler.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) player), new ServerSenarioTextPacket(currentLiteral.toString(), true, isNowait));
        currentLiteral = null;
    }

    public void sendNoreline() {
        if (currentLiteral != null && currentLiteral.length() != 0)
            FHPacketHandler.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) player), new ServerSenarioTextPacket(currentLiteral.toString(), false, isNowait));
        currentLiteral = null;
    }

    public void waitClient() {
        waitClient = true;
    }

    public void notifyClientResponse(boolean isSkip, int status) {
        if (waitClient) {
            waitClient = false;
            this.isSkip = isSkip;
            this.status = status;
            run();
        }
    }

    public void paragraph(int pn) {
        paragraphNum = pn;
        current = new ParagraphRunner(new CompoundNBT(), paragraphNum, player);
        waitClient();
        sendNormal();

    }
    public boolean containsPath(String path) {
        return current.containsPath(path);
    }

    public double eval(String exp) {
        return Evaluator.eval(exp).eval(this);
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

    @Override
    public double get(String key) {

        return evalPathDouble(key);
    }

    public CallStackElement getByCaller(int caller) {
        for (int i = callStack.size() - 1; i >= 0; i--) {
            CallStackElement cur = callStack.get(i);
            if (cur.getCaller() == caller)
                return cur;
        }
        return null;
    }

    public ParagraphRunner getCurrentParagraph() {
        return current;
    }

    public CompoundNBT getExecutionData() {
        return null;
    }

    public CallStackElement getLast() {
        return callStack.get(callStack.size() - 1);
    }

    public int getNodeNum() {
        return nodeNum;
    }

    @Override
    public Double getOptional(String key) {
        if (!containsPath(key))
            return null;
        return get(key);
    }

    public ServerPlayerEntity getPlayer() {
        return null;
    }

    public void jump(int target) {
        nodeNum = target;
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

    public void pushCall(int caller, int target) {
        callStack.add(new CallStackElement(caller, target));
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

    @Override
    public void set(String key, double v) {
        setPathNumber(key, v);
    }

    public void setNodeNum(int nodeNum) {
        this.nodeNum = nodeNum;
    }

    public void setPath(String path, INBT val) {
        current.setPath(path, val);
    }

    public void setPathNumber(String path, Number val) {
        current.setPathNumber(path, val);
    }

    public void setPathString(String path, String val) {
        current.setPathString(path, val);
    }

}
