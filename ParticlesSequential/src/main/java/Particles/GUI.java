package Particles;

import Utils.Logger;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class GUI extends JFrame {
    private final JPanel panel;
    private final List<Particle> particles;
    private final List<ParticlePositionCopy> particleCopy;
    private final SimulationConfig config;
    private volatile boolean running = true;
    private Thread renderThread;
    private static final int FPS = 60;
    private static final long FRAME_TIME = 1000 / FPS;
    private int currentCPS = 0;
    private long startTime = 0;

    private static class ParticlePositionCopy {
        double x;
        double y;
        double charge;

        ParticlePositionCopy(double x, double y, double charge) {
            this.x = x;
            this.y = y;
            this.charge = charge;
        }
    }

    public GUI(SimulationConfig config, List<Particle> particles) {
        this.config = config;
        this.particles = particles;
        this.particleCopy = new ArrayList<>(config.numOfParticles);

        for (int i = 0; i < config.numOfParticles; i++) {
            Particle particle = particles.get(i);
            particleCopy.add(new ParticlePositionCopy(particle.x, particle.y, particle.charge));
        }

        setTitle("Particles");
        setSize(config.width, config.height);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        panel = new DrawPanel();
        add(panel);

        setVisible(true);
    }

    public void start() {
        renderThread = new Thread(() -> {
            while (running) {
                long startTime = System.currentTimeMillis();

                updateParticleCopy();
                panel.repaint();

                long elapsed = System.currentTimeMillis() - startTime;
                long sleepTime = FRAME_TIME - elapsed;
                if (System.currentTimeMillis() - startTime < FRAME_TIME) {
                    try {
                        Thread.sleep(FRAME_TIME - (System.currentTimeMillis() - startTime));
                    } catch (InterruptedException e) {
                        Logger.error(e.getMessage());
                    }
                }
            }
        });
        renderThread.start();
    }

    public void updateParticleCopy() {
        for (int i = 0; i < config.numOfParticles; i++) {
            Particle p = particles.get(i);
            ParticlePositionCopy s = particleCopy.get(i);
            s.x = p.x;
            s.y = p.y;
            s.charge = p.charge;
        }
    }

    public void stop() {
        running = false;
        try {
            renderThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        this.dispose();
    }

    public void update(int cyclesPerSecond) {
        long endTime = System.currentTimeMillis();
        if(endTime - startTime >= 1000) {
            currentCPS = cyclesPerSecond;
            startTime = System.currentTimeMillis();
        }
    }

    class DrawPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, getWidth(), getHeight());

            for(int i = 0; i < config.numOfParticles; i++) {
                ParticlePositionCopy currentParticleCopy = particleCopy.get(i);

                if (currentParticleCopy.charge > 0) {
                    g2.setColor(Color.RED);
                } else {
                    g2.setColor(Color.BLUE);
                }

                int size = 6;
                g2.fillOval((int) currentParticleCopy.x - size / 2, (int) currentParticleCopy.y - size / 2, size, size);
            }

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("serif", Font.BOLD, 30));
            g2.drawString("CPS:" + String.valueOf(currentCPS), 0, 25);
        }
    }
}