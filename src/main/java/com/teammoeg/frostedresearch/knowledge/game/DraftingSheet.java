package com.teammoeg.frostedresearch.knowledge.game;

import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import java.util.*;
import static com.teammoeg.frostedresearch.knowledge.game.DraftingGeometry.*;

public record DraftingSheet(String title,String dimensions,double extent,List<P> datums,List<Curve> targets,List<Hint> hints) {
    public record Hint(String text,List<P> points) {}
    public static boolean matches(ResourceLocation id) { return id!=null && id.getPath().startsWith("drafting/"); }

    public static DraftingSheet read(JsonObject obj) {
        List<Hint> hints=new ArrayList<>();
        for(JsonElement item:obj.getAsJsonArray("hints")==null?new JsonArray():obj.getAsJsonArray("hints")) {
            var hint=item.getAsJsonObject();
            hints.add(new Hint(hint.get("text").getAsString(),points(hint.getAsJsonArray("points"))));
        }
        var targets=curves(obj.getAsJsonArray("targets"));
        double extent=obj.get("extent").getAsDouble();
        if(targets.isEmpty() || !(extent>0 && Double.isFinite(extent))) throw new IllegalArgumentException("Empty drawing or invalid extent");
        return new DraftingSheet(obj.get("title").getAsString(),obj.get("dimensions").getAsString(),extent,
            points(obj.getAsJsonArray("datums")),targets,List.copyOf(hints));
    }
    private static List<P> points(JsonArray array) {
        List<P> points=new ArrayList<>();
        if(array!=null) for(var item:array) { var p=item.getAsJsonArray(); points.add(new P(value(p,0),value(p,1))); }
        return List.copyOf(points);
    }
    private static double value(JsonArray row,int i) {
        double v=row.get(i).getAsDouble();
        if(!Double.isFinite(v)) throw new IllegalArgumentException("Invalid coordinate");
        return v;
    }
    public static List<Curve> curves(JsonArray array) {
        List<Curve> result=new ArrayList<>();
        for(JsonElement item:array) {
            JsonArray c=item.getAsJsonArray();
            P p=new P(value(c,0),value(c,1));
            if(c.size()==4) result.add(Curve.line(p,new P(value(c,2),value(c,3))));
            else if(c.size()==5 && value(c,2)>0 && value(c,4)>0 && value(c,4)<=360.000001)
                result.add(Curve.arc(p,value(c,2),value(c,3),value(c,4)));
            else throw new IllegalArgumentException("Invalid drawing primitive");
        }
        return List.copyOf(result);
    }
    public static String encode(List<Curve> ink) {
        JsonArray result=new JsonArray();
        for(Curve c:ink) {
            JsonArray row=new JsonArray();
            double[] values=c.curved()?new double[]{c.a().x(),c.a().y(),c.radius(),c.start(),c.sweep()}:
                new double[]{c.a().x(),c.a().y(),c.b().x(),c.b().y()};
            for(double v:values) row.add(Math.rint(v*1e6)/1e6);
            result.add(row);
        }
        return result.toString();
    }
    public List<Curve> gaps(List<Curve> ink) {
        return targets.stream().flatMap(t->split(t,ink,false).stream()).filter(s->!covered(s,ink)).toList();
    }
    public int seconds() { return 2+targets.stream().mapToInt(c->c.curved()&&c.sweep()<360?2:1).sum(); }
}
