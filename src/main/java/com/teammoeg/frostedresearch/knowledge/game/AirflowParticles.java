package com.teammoeg.frostedresearch.knowledge.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Cosmetic particles in cell coordinates. Outcomes are decided separately by AirflowGame. */
public final class AirflowParticles {
    public static final int LIMIT = 72;
    public static final class Particle {
        public double x, y, vx, vy;
        public boolean heated;
        private int age, turn;
    }
    private final Random random;
    private final List<Particle> particles = new ArrayList<>();
    private final List<Particle> view = Collections.unmodifiableList(particles);
    public AirflowParticles() { this(System.nanoTime()); }
    public AirflowParticles(long seed) { random = new Random(seed); }
    public List<Particle> particles() { return view; }
    public void clear() { particles.clear(); }
    private void turn(Particle particle) {
        double angle = random.nextDouble() * Math.PI * 2, speed = .16 + random.nextDouble() * .22;
        particle.vx = Math.cos(angle) * speed; particle.vy = Math.sin(angle) * speed;
        particle.turn = 8 + random.nextInt(14);
    }
    private void spawn(Particle particle) {
        particle.x = 1.2 + random.nextDouble() * 2.3; particle.y = 19.2 + random.nextDouble() * 2.3;
        particle.age = 0; particle.heated = false; turn(particle);
    }
    private static boolean open(AirflowGame game, double x, double y) {
        if (x < 0 || x >= AirflowGame.WIDTH || y < 0 || y >= AirflowGame.HEIGHT) return false;
        int cell = game.cell((int)y * AirflowGame.WIDTH + (int)x);
        return cell == AirflowGame.OPEN || cell == AirflowGame.BED;
    }
    public void tick(AirflowGame game) {
        if (particles.size() < LIMIT) { var particle = new Particle(); spawn(particle); particles.add(particle); }
        for (var particle : particles) {
            if (++particle.age > 500 || !open(game, particle.x, particle.y)) spawn(particle);
            if (--particle.turn <= 0) turn(particle);
            if (particle.heated) particle.vy = Math.max(-.4, particle.vy - .018);
            double nx = particle.x + particle.vx;
            if (nx < 0 || nx >= AirflowGame.WIDTH) { spawn(particle); continue; }
            if (open(game, nx, particle.y)) particle.x = nx; else particle.vx = -particle.vx;
            double ny = particle.y + particle.vy;
            if (ny < 0 || ny >= AirflowGame.HEIGHT) { spawn(particle); continue; }
            if (open(game, particle.x, ny)) particle.y = ny; else particle.vy = -particle.vy;
            if (game.cell((int)particle.y * AirflowGame.WIDTH + (int)particle.x) == AirflowGame.BED) particle.heated = true;
        }
    }
}
