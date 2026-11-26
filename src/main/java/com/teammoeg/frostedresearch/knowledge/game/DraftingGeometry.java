package com.teammoeg.frostedresearch.knowledge.game;

import java.util.*;
import java.util.stream.Stream;

/** Lines, points and positive-sweep circular arcs in drawing millimetres (y increases down). */
public final class DraftingGeometry {
    private DraftingGeometry() {}
    public static final double EPS = 0.08;
    public record P(double x, double y) {
        public P plus(P b) { return new P(x+b.x,y+b.y); }
        public P minus(P b) { return new P(x-b.x,y-b.y); }
        public P times(double k) { return new P(x*k,y*k); }
        public double dot(P b) { return x*b.x+y*b.y; }
        public double cross(P b) { return x*b.y-y*b.x; }
        public double dist(P b) { return Math.hypot(x-b.x,y-b.y); }
    }
    // For a line, a and b are endpoints. For an arc, a is the centre.
    public record Curve(P a, P b, double radius, double start, double sweep) {
        public static Curve line(P a,P b) { return new Curve(a,b,0,0,0); }
        public static Curve arc(P c,double r,double start,double sweep) { return new Curve(c,c,r,start,sweep); }
        public boolean curved() { return radius>0; }
        public double length() { return curved()?radius*Math.toRadians(sweep):a.dist(b); }
        public P at(double t) {
            if(!curved()) return a.plus(b.minus(a).times(t));
            double angle=Math.toRadians(start+sweep*t);
            return a.plus(new P(Math.cos(angle),Math.sin(angle)).times(radius));
        }
        public Curve piece(double lo,double hi) {
            return curved()?arc(a,radius,start+sweep*lo,sweep*(hi-lo)):line(at(lo),at(hi));
        }
        public double parameter(P p) {
            P d=p.minus(a);
            if(curved()) {
                double angle=(Math.toDegrees(Math.atan2(d.y,d.x))-start)%360;
                if(angle < -1e-8) angle+=360;
                return angle/sweep;
            }
            P axis=b.minus(a);
            return axis.dot(axis)==0?0:axis.dot(d)/axis.dot(axis);
        }
        public P nearest(P p) {
            double t=parameter(p);
            if(t>=0 && t<=1) return at(t);
            return at(0).dist(p)<at(1).dist(p)?at(0):at(1);
        }
        public boolean contains(P p) { return nearest(p).dist(p)<=EPS; }
        boolean sameSupport(Curve c) {
            if(curved()!=c.curved()) return false;
            if(curved()) return a.dist(c.a)<=EPS && Math.abs(radius-c.radius)<=EPS;
            if(length()<EPS || c.length()<EPS) return length()<EPS && c.length()<EPS && a.dist(c.a)<=EPS;
            P unit=b.minus(a).times(1/length());
            return Math.abs(c.a.minus(a).cross(unit))<=EPS && Math.abs(c.b.minus(a).cross(unit))<=EPS;
        }
    }
    public static List<P> intersections(Curve s,Curve t) {
        List<P> points=new ArrayList<>();
        if(s.curved() && !t.curved()) return intersections(t,s);
        P delta=t.a.minus(s.a);
        if(!s.curved()) {
            P d=s.b.minus(s.a);
            if(t.curved()) {
                double len=s.length();
                if(len<EPS) return t.contains(s.a)?List.of(s.a):List.of();
                P unit=d.times(1/len);
                double along=delta.dot(unit), height2=t.radius*t.radius-Math.pow(delta.cross(unit),2);
                if(height2>=-1e-8) for(int sign:new int[]{-1,1})
                    points.add(s.a.plus(unit.times(along+sign*Math.sqrt(Math.max(0,height2)))));
            } else {
                P e=t.b.minus(t.a);
                if(Math.abs(d.cross(e))>1e-9) points.add(s.a.plus(d.times(delta.cross(e)/d.cross(e))));
            }
        } else {
            double dist=s.a.dist(t.a);
            if(dist<EPS) return List.of();
            double along=(s.radius*s.radius-t.radius*t.radius+dist*dist)/(2*dist);
            double height2=s.radius*s.radius-along*along;
            if(height2>=-1e-8) {
                P unit=delta.times(1/dist), foot=s.a.plus(unit.times(along));
                P normal=new P(-unit.y,unit.x).times(Math.sqrt(Math.max(0,height2)));
                points.add(foot.plus(normal)); points.add(foot.minus(normal));
            }
        }
        return points.stream().filter(p->s.contains(p)&&t.contains(p)).distinct().toList();
    }
    /** Split wherever coverage can change, and optionally at crossing construction strokes. */
    public static List<Curve> split(Curve s,List<Curve> others,boolean crossings) {
        TreeSet<Double> cuts=new TreeSet<>(List.of(0d,1d));
        for(Curve c:others) {
            Stream<P> points;
            if(s.sameSupport(c)) points=Stream.of(c.at(0),c.at(1));
            else if(crossings) points=intersections(s,c).stream();
            else continue;
            points.filter(s::contains).map(s::parameter).filter(t->t>0 && t<1).forEach(cuts::add);
        }
        if(s.length()<EPS) return List.of(s);
        List<Curve> out=new ArrayList<>();
        double prev=0;
        for(double t:cuts) {
            if((t-prev)*s.length()<1e-7) continue;
            out.add(s.piece(prev,t)); prev=t;
        }
        return out;
    }
    public static boolean covered(Curve piece,List<Curve> ink) {
        return ink.stream().anyMatch(c->piece.sameSupport(c) && c.contains(piece.at(.5)));
    }
}
