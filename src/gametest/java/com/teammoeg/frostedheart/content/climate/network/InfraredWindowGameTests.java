/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.network;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("frostedheart_infrared_window")
@PrefixGameTestTemplate(false)
public final class InfraredWindowGameTests {
    @GameTest(template = "phase0a_empty", timeoutTicks = 500)
    public static void mirrorShiftPreservesEveryOverlappingVoxelAndClearsEnteringSpace(GameTestHelper helper) {
        int side = InfraredWindow.SIDE;
        var mirror = ByteBuffer.allocateDirect(side * side * side * 2).order(ByteOrder.nativeOrder()).asShortBuffer();
        long center = SectionPos.asLong(-17, -4, 31);
        int[][] shifts = {{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1},{1,-1,1},{-1,1,-1},{8,-8,8},{9,0,0}};
        for (int[] shift : shifts) {
            for (int z=0;z<side;z++) for(int y=0;y<side;y++) for(int x=0;x<side;x++)
                mirror.put((z*side+y)*side+x, value(x,y,z));
            long next = SectionPos.asLong(-17+shift[0], -4+shift[1], 31+shift[2]);
            InfraredWindow.shift(mirror, center, next);
            for (int z=0;z<side;z++) for(int y=0;y<side;y++) for(int x=0;x<side;x++) {
                int sx=x+shift[0]*16, sy=y+shift[1]*16, sz=z+shift[2]*16;
                short expected = sx<0||sx>=side||sy<0||sy>=side||sz<0||sz>=side
                        ? InfraredBrickCodec.INVALID_TEMPERATURE : value(sx,sy,sz);
                if (mirror.get((z*side+y)*side+x)!=expected)
                    helper.fail("Mirror rebase changed a world sample: shift="+java.util.Arrays.toString(shift)+", voxel="+x+","+y+","+z);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void pageMasksFollowTheSameWorldOverlap(GameTestHelper helper) {
        long[] source = new long[12];
        for(int page=0;page<729;page++) if(page%3==0) source[page>>>6]|=1L<<(page&63);
        long old = SectionPos.asLong(20, -4, -30), next=SectionPos.asLong(21,-5,-29);
        long[] target = new long[12];
        InfraredWindow.remap(source,target,old,next);
        for(int page=0;page<729;page++) {
            int x=page%9+1,z=page/9%9+1,y=page/81-1;
            boolean expected=x<9&&z<9&&y>=0&&(x+9*(z+9*y))%3==0;
            helper.assertTrue(((target[page>>>6]&(1L<<(page&63)))!=0)==expected,
                    "Page presence must refer to the same absolute Section after movement");
        }
        helper.succeed();
    }

    private static short value(int x,int y,int z) { return (short)((x*73 ^ y*151 ^ z*263)&32767); }
}
