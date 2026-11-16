package com.teammoeg.frostedresearch.knowledge.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Cosmetic gas; volume sets population while AirflowGame alone decides research outcomes. */
public final class AirflowParticles {
    public static final double DENSITY = .5, RADIUS = .24;
    public static final int LIMIT = (int)Math.ceil(AirflowGame.SIZE * DENSITY);
    private static final double DIAMETER = RADIUS * 2, MAX_SPEED = .45;
    private static final int SUBSTEPS = 2, SOLVER_PASSES = 2, BIRTHS_PER_TICK = 16;
    public static final class Particle {
        public double x, y, previousX, previousY, vx, vy;
        public boolean heated;
        private int turn;
    }
    private final Random random;
    private final List<Particle> particles = new ArrayList<>();
    private final List<Particle> view = Collections.unmodifiableList(particles);
    private final boolean[] previousOpen = new boolean[AirflowGame.SIZE];
    private final int[] openCells = new int[AirflowGame.SIZE], newCells = new int[AirflowGame.SIZE];
    private final int[] emissionCells = new int[AirflowGame.SIZE], heads = new int[AirflowGame.SIZE], next = new int[LIMIT];
    private int openCount, emissionCount, target, pairChecks;
    private long collisions;
    private boolean initialFill = true;

    public AirflowParticles() { this(System.nanoTime()); }
    public AirflowParticles(long seed) { random = new Random(seed); }
    public List<Particle> particles() { return view; }
    public int targetCount() { return target; }
    public int openCount() { return openCount; }
    public int lastPairChecks() { return pairChecks; }
    public long collisions() { return collisions; }
    public void clear() {
        particles.clear(); Arrays.fill(previousOpen,false);
        openCount = emissionCount = target = pairChecks = 0; collisions = 0; initialFill = true;
    }
    private void updateVolume(AirflowGame game) {
        int added = 0; openCount = 0;
        for (int cell = 0; cell < AirflowGame.SIZE; cell++) {
            int type = game.cell(cell); boolean open = type == AirflowGame.OPEN || type == AirflowGame.BED;
            if (open) { openCells[openCount++] = cell; if (!previousOpen[cell]) newCells[added++] = cell; }
            previousOpen[cell] = open;
        }
        if (added > 0) {
            if (!particles.isEmpty()) initialFill = false;
            System.arraycopy(newCells,0,emissionCells,0,added); emissionCount = added;
        }
        target = (int)Math.ceil(openCount * DENSITY);
    }
    private void turn(Particle particle) {
        double angle = random.nextDouble() * Math.PI * 2, speed = .16 + random.nextDouble() * .22;
        particle.vx = Math.cos(angle) * speed; particle.vy = Math.sin(angle) * speed;
        particle.turn = 8 + random.nextInt(14);
    }
    static boolean fits(AirflowGame game, double x, double y) {
        if (x < 0 || x >= AirflowGame.WIDTH || y < 0 || y >= AirflowGame.HEIGHT) return false;
        for (int cy = (int)Math.floor(y-RADIUS); cy <= (int)Math.floor(y+RADIUS); cy++)
            for (int cx = (int)Math.floor(x-RADIUS); cx <= (int)Math.floor(x+RADIUS); cx++) {
                if (cx < 0 || cx >= AirflowGame.WIDTH || cy < 0 || cy >= AirflowGame.HEIGHT) continue;
                int type = game.cell(cy * AirflowGame.WIDTH + cx);
                if (type == AirflowGame.OPEN || type == AirflowGame.BED) continue;
                double dx = x-Math.max(cx,Math.min(x,cx+1)), dy = y-Math.max(cy,Math.min(y,cy+1));
                if (dx*dx+dy*dy < RADIUS*RADIUS-1e-10) return false;
            }
        return true;
    }
    private boolean place(AirflowGame game, Particle particle) {
        if (openCount == 0) return false;
        for (int attempt = 0; attempt < 48; attempt++) {
            int cell;
            if (particles.isEmpty() || (!initialFill && attempt < 24 && random.nextInt(4)==0)) cell = (19+random.nextInt(3))*28+1+random.nextInt(3);
            else if (attempt < 24 && emissionCount > 0) cell = emissionCells[random.nextInt(emissionCount)];
            else cell = openCells[random.nextInt(openCount)];
            double x = cell%28 + RADIUS + random.nextDouble()*(1-DIAMETER);
            double y = cell/28 + RADIUS + random.nextDouble()*(1-DIAMETER);
            if (!fits(game,x,y)) continue;
            boolean occupied = false;
            for (var other : particles) if (other != particle) {
                double dx = other.x-x, dy = other.y-y;
                if (dx*dx+dy*dy < DIAMETER*DIAMETER) { occupied=true;break; }
            }
            if (occupied) continue;
            particle.x = particle.previousX = x; particle.y = particle.previousY = y;
            particle.heated = game.cell(cell)==AirflowGame.BED; turn(particle); return true;
        }
        return false;
    }
    static boolean resolvePair(AirflowGame game, Particle a, Particle b) {
        double dx=b.x-a.x, dy=b.y-a.y, distance2=dx*dx+dy*dy;
        if (distance2 >= DIAMETER*DIAMETER) return false;
        double distance=Math.sqrt(distance2), nx=distance>1e-9 ? dx/distance : 1, ny=distance>1e-9 ? dy/distance : 0;
        double overlap=DIAMETER-distance+1e-6, half=overlap*.5;
        boolean moveA=fits(game,a.x-nx*half,a.y-ny*half), moveB=fits(game,b.x+nx*half,b.y+ny*half);
        if (moveA && moveB) { a.x-=nx*half;a.y-=ny*half;b.x+=nx*half;b.y+=ny*half; }
        else if (moveA && fits(game,a.x-nx*overlap,a.y-ny*overlap)) { a.x-=nx*overlap;a.y-=ny*overlap; }
        else if (moveB && fits(game,b.x+nx*overlap,b.y+ny*overlap)) { b.x+=nx*overlap;b.y+=ny*overlap; }
        double closing=(b.vx-a.vx)*nx+(b.vy-a.vy)*ny;
        if (closing < 0) { a.vx+=closing*nx;a.vy+=closing*ny;b.vx-=closing*nx;b.vy-=closing*ny; }
        return true;
    }
    private void collide(AirflowGame game) {
        Arrays.fill(heads,-1);
        for (int i=0;i<particles.size();i++) {
            var p=particles.get(i);int cell=(int)p.y*28+(int)p.x;next[i]=heads[cell];heads[cell]=i;
        }
        for (int i=0;i<particles.size();i++) {
            var a=particles.get(i);int x=(int)a.x,y=(int)a.y;
            for (int cy=Math.max(0,y-1);cy<=Math.min(27,y+1);cy++) for (int cx=Math.max(0,x-1);cx<=Math.min(27,x+1);cx++)
                for (int j=heads[cy*28+cx];j>=0;j=next[j]) if (j>i) {
                    pairChecks++;if(resolvePair(game,a,particles.get(j)))collisions++;
                }
        }
    }
    public void tick(AirflowGame game) {
        updateVolume(game);pairChecks=0;
        while (particles.size()>target) particles.remove(particles.size()-1);
        for (int born=0;born<BIRTHS_PER_TICK && particles.size()<target;born++) {
            var particle=new Particle();if(place(game,particle))particles.add(particle);
        }
        for (int i=0;i<particles.size();i++) {
            var p=particles.get(i);
            if (!fits(game,p.x,p.y) && !place(game,p)) { particles.remove(i--);continue; }
            p.previousX=p.x;p.previousY=p.y;
            if (--p.turn<=0)turn(p);
            if(p.heated)p.vy=Math.max(-.4,p.vy-.018);
        }
        for (int step=0;step<SUBSTEPS;step++) {
            for (int i=0;i<particles.size();i++) {
                var p=particles.get(i);double speed=Math.hypot(p.vx,p.vy);
                if(speed>MAX_SPEED){p.vx*=MAX_SPEED/speed;p.vy*=MAX_SPEED/speed;}
                double nx=p.x+p.vx/SUBSTEPS,ny=p.y+p.vy/SUBSTEPS;
                if(nx<0 || nx>=28 || ny<0 || ny>=28) {
                    if(!place(game,p))particles.remove(i--);continue;
                }
                if(fits(game,nx,p.y))p.x=nx;else p.vx=-p.vx;
                if(fits(game,p.x,ny))p.y=ny;else p.vy=-p.vy;
            }
            for(int pass=0;pass<SOLVER_PASSES;pass++)collide(game);
            for(var p:particles)if(game.cell((int)p.y*28+(int)p.x)==AirflowGame.BED)p.heated=true;
        }
    }
}
