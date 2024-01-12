package com.teammoeg.frostedheart.scenario.runner;

import java.util.Arrays;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;

public class ParagraphRunner {
    CompoundNBT parVars;
    int paragraphNum;
    String section;
    String chapter;
    String file;
    PlayerEntity player;

    public ParagraphRunner(CompoundNBT parVars, int paragraphNum, String section, String chapter, String file,
                           PlayerEntity player) {
        super();
        this.parVars = parVars;
        this.paragraphNum = paragraphNum;
        this.section = section;
        this.chapter = chapter;
        this.file = file;
        this.player = player;
    }

    public void setPath(String path, INBT val) {
        String[] paths = path.split("\\.");
        CompoundNBT nbt = parVars;
        for (int i = 0; i < paths.length - 1; i++) {
            if (nbt.contains(paths[i], 10)) {
                nbt = nbt.getCompound(paths[i]);
            } else if (!nbt.contains(paths[i])) {
                CompoundNBT cnbt = new CompoundNBT();
                nbt.put(paths[i], cnbt);
                nbt = cnbt;
            } else
                throw new IllegalArgumentException(String.join(".", Arrays.copyOfRange(paths, 0, i + 1)) + " is not an object");
        }
        nbt.put(paths[paths.length - 1], val);
    }

    public void setPathString(String path, String val) {
        String[] paths = path.split("\\.");
        CompoundNBT nbt = parVars;
        for (int i = 0; i < paths.length - 1; i++) {
            if (nbt.contains(paths[i], 10)) {
                nbt = nbt.getCompound(paths[i]);
            } else if (!nbt.contains(paths[i])) {
                CompoundNBT cnbt = new CompoundNBT();
                nbt.put(paths[i], cnbt);
                nbt = cnbt;
            } else
                throw new IllegalArgumentException(String.join(".", Arrays.copyOfRange(paths, 0, i + 1)) + " is not an object");
        }
        nbt.putString(paths[paths.length - 1], val);
    }

    public void setPathNumber(String path, Number val) {
        String[] paths = path.split("\\.");
        CompoundNBT nbt = parVars;
        for (int i = 0; i < paths.length - 1; i++) {
            if (nbt.contains(paths[i], 10)) {
                nbt = nbt.getCompound(paths[i]);
            } else if (!nbt.contains(paths[i])) {
                CompoundNBT cnbt = new CompoundNBT();
                nbt.put(paths[i], cnbt);
                nbt = cnbt;
            } else
                throw new IllegalArgumentException(String.join(".", Arrays.copyOfRange(paths, 0, i + 1)) + " is not an object");
        }
        nbt.putDouble(paths[paths.length - 1], val.doubleValue());
    }

    public INBT evalPath(String path) {
        String[] paths = path.split("\\.");
        CompoundNBT nbt = parVars;
        for (int i = 0; i < paths.length - 1; i++) {
            nbt = nbt.getCompound(paths[i]);
        }
        return nbt.get(paths[paths.length - 1]);
    }

    public Double evalPathDouble(String path) {
        String[] paths = path.split("\\.");
        CompoundNBT nbt = parVars;
        for (int i = 0; i < paths.length - 1; i++) {
            nbt = nbt.getCompound(paths[i]);
        }
        return nbt.getDouble(paths[paths.length - 1]);
    }

    public boolean containsPath(String path) {
        String[] paths = path.split("\\.");
        CompoundNBT nbt = parVars;
        for (int i = 0; i < paths.length - 1; i++) {
            if (!nbt.contains(paths[i], 10))
                return false;
            nbt = nbt.getCompound(paths[i]);
        }
        return nbt.contains(paths[paths.length - 1]);
    }

    public String evalPathString(String path) {
        return evalPath(path).getString();
    }

    public CompoundNBT getExecutionData() {
        return parVars;
    }

    public PlayerEntity getPlayer() {
        return player;
    }
}
