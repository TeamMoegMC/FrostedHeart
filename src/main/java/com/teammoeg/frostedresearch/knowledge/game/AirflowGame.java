package com.teammoeg.frostedresearch.knowledge.game;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import java.util.*;

/** Five rotating pipes in a fixed solvable 3x3 topology. N/E/S/W ports are bits 1/2/4/8. */
public final class AirflowGame {
    private static final int[] BASE={0,6,12,10,9,3,0,0,0};
    private final int[] rotations=new int[9];
    public void reset(RandomSource random) {
        do { for(int i=0;i<9;i++) rotations[i]=BASE[i]==0 ? 0 : random.nextInt(4); } while(won());
    }
    public int mask(int cell) {
        if(cell<0 || cell>=9) return 0;
        int bits=BASE[cell],n=rotations[cell];return ((bits<<n)|(bits>>(4-n)))&15;
    }
    public boolean rotate(int cell) {
        if(cell<0 || cell>=9 || BASE[cell]==0 || won()) return false;
        rotations[cell]=(rotations[cell]+1)%4;return true;
    }
    public boolean won() {
        if((mask(3)&8)==0 || (mask(5)&2)==0) return false;
        Set<Integer> seen=new HashSet<>();var queue=new ArrayDeque<Integer>();queue.add(3);
        int[] dx={0,1,0,-1},dy={-1,0,1,0};
        while(!queue.isEmpty()) {
            int at=queue.remove();if(!seen.add(at)) continue;if(at==5) return true;
            for(int d=0;d<4;d++) if((mask(at)&(1<<d))!=0) {
                int x=at%3+dx[d],y=at/3+dy[d];if(x<0 || x>2 || y<0 || y>2) continue;
                int next=y*3+x;if((mask(next)&(1<<((d+2)%4)))!=0) queue.add(next);
            }
        }return false;
    }
    public CompoundTag save() { var tag=new CompoundTag();tag.putIntArray("rotations",rotations);return tag; }
    public void load(CompoundTag tag) {
        int[] saved=tag.getIntArray("rotations");
        if(saved.length!=9) { reset(RandomSource.create());return; }
        for(int i=0;i<9;i++) rotations[i]=Math.floorMod(saved[i],4);
    }
    public CompoundTag view() {
        var tag=new CompoundTag();int[] masks=new int[9];for(int i=0;i<9;i++) masks[i]=mask(i);
        tag.putIntArray("masks",masks);tag.putBoolean("won",won());return tag;
    }
}
