package com.teammoeg.frostedresearch.knowledge;

import com.google.gson.JsonParser;
import com.teammoeg.frostedresearch.knowledge.game.DraftingSheet;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.gametest.*;
import java.util.*;
import static com.teammoeg.frostedresearch.knowledge.game.DraftingGeometry.*;

@GameTestHolder("frostedresearch_drafting")
@PrefixGameTestTemplate(false)
public final class DraftingGameTests {
    @GameTest(template="empty")
    public static void verifyPumpFromIndependentConstruction(GameTestHelper h) throws Exception {
        var resource=new ResourceLocation("frostedresearch:drafting/four_blade_pump.json");
        try(var reader=h.getLevel().getServer().getResourceManager().openAsReader(resource)) {
            var sheet=DraftingSheet.read(JsonParser.parseReader(reader).getAsJsonObject());
            P origin=new P(0,0);
            h.assertTrue(sheet.datums().equals(List.of(origin, new P(24,0))) && sheet.seconds()==16,"Shaft and blade centre datums and blueprint workload");
            h.assertTrue(sheet.gaps(List.of()).size()==10,"Blank drawing has ten missing outlines");
            List<Curve> ink=new ArrayList<>();
            for(double r:new double[]{48,12,24}) ink.add(Curve.arc(origin,r,0,360));
            for(P centre:List.of(new P(-24,0),new P(24,0),new P(0,-24),new P(0,24)))
                ink.add(Curve.arc(centre,20,0,360));
            ink.add(Curve.line(new P(-44,0),new P(44,0)));
            ink.add(Curve.line(new P(0,44),new P(0,-43.5)));
            var gaps=sheet.gaps(ink);
            h.assertTrue(gaps.size()==1 && Math.abs(gaps.get(0).length()-.5)<EPS,"Detect half-millimetre tip gap");
            ink.add(Curve.line(new P(0,-44),new P(0,-43.5)));
            h.assertTrue(sheet.gaps(ink).isEmpty(),"Extra ink, reversed and split lines satisfy coverage");
            var rim=ink.get(3);
            var pieces=split(rim,sheet.targets(),true);
            h.assertTrue(pieces.stream().anyMatch(p->covered(p,sheet.targets())) &&
                pieces.stream().anyMatch(p->!covered(p,sheet.targets())),"Only the required part of a blade circle is green");
            h.succeed();
        }
    }
}
