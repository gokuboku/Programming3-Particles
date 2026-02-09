package Particles;

import Utils.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ParticleSimulationParallel {
    private final List<Particle> particles;
    private final SimulationConfig config;
    private final Random random;
    private final double DAMPING;
    private final double MINIMUM_DISTANCE;
    private final double MAXIMUM_SPEED;

    private static final double SLOW_DOWN = 0.1;


    private int cyclesPerSecond = 0;
    private int numberOfCompleteCycles = 0;
    private long startTime;
    private int chargeModifier = 1;

    public ParticleSimulationParallel(SimulationConfig config) {
        this.config = config;
        this.DAMPING = config.damping;
        this.MINIMUM_DISTANCE = config.minimumDistance;
        this.MAXIMUM_SPEED = config.maximumSpeed;
        this.random = new Random(config.particleSeed);
        this.particles = new ArrayList<>();
        initializeParticles();
    }

    private void initializeParticles() {
        for (int i = 0; i < config.numOfParticles; i++) {
            double x = random.nextDouble() * config.width;
            double y = random.nextDouble() * config.height;

            double startingVelocityX = (random.nextDouble() - 0.5);
            double startingVelocityY = (random.nextDouble() - 0.5);

            double chargeStrength = 0.5 + random.nextDouble() * 1.5;
            double charge = chargeStrength * chargeModifier;
            chargeModifier = chargeModifier * -1;

            Particle tempParticle = new Particle(x, y, startingVelocityX, startingVelocityY, charge);
            particles.add(tempParticle);
        }
    }

    public void runParallel() {
        int numberOfThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);

        GUI gui = null;
        if (config.enableGUI) {
            gui = new GUI(config, particles);
            gui.start();
        }

        for (int cycle = 0; cycle < config.cycles; cycle++) {
            calculateForcesParallel(executor, numberOfThreads);

            updatePositions();

            cyclesPerSecond++;
            if (System.currentTimeMillis() - startTime > 1000) {
                numberOfCompleteCycles += cyclesPerSecond;
                startTime = System.currentTimeMillis();
                Logger.info("Number of cycles completed: " + numberOfCompleteCycles + "/" + config.cycles);
                if (gui != null) {
                    gui.update(cyclesPerSecond);
                }
                cyclesPerSecond = 0;
            }
        }

        executor.shutdown();
        if (gui != null) {
            gui.stop();
        }
    }


    private void calculateForcesParallel(ExecutorService executor, int numberOfThreads) {
        for (int i = 0; i < config.numOfParticles; i++) {
            particles.get(i).forceX = 0;
            particles.get(i).forceY = 0;
        }
        int[] start = new int[numberOfThreads];
        int[] end = new int[numberOfThreads];
        int particlesPerThread = config.numOfParticles / numberOfThreads;
        double[][] threadForcesX = new double[numberOfThreads][config.numOfParticles];
        double[][] threadForcesY = new double[numberOfThreads][config.numOfParticles];

        for(int i = 0; i < numberOfThreads; i++){
            start[i] = i * particlesPerThread;
            if(i == numberOfThreads - 1) {
                end[i] = config.numOfParticles;
            }
            else{
                end[i] = (i + 1) * particlesPerThread;
            }
        }

        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        for (int i = 0; i < numberOfThreads; i++) {
            int tempThreadId = i;
            executor.submit(() -> {
                for (int j = start[tempThreadId]; j < end[tempThreadId]; j++) {
                    for (int k = j + 1; k < config.numOfParticles; k++) {
                        applyForceParallel(j, k, threadForcesX[tempThreadId], threadForcesY[tempThreadId]);
                    }
                }
                latch.countDown();

            });
        }

        try {
            latch.await();
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Logger.error("Force computation interrupted: " + e.getMessage());
        }

        CountDownLatch mergeLatch = new CountDownLatch(numberOfThreads);

        for (int i = 0; i < numberOfThreads; i++) {
            int tempThreadId = i;

            executor.submit(() -> {
                for (int j = start[tempThreadId]; j < end[tempThreadId]; j++) {
                    for (int threadId = 0; threadId < numberOfThreads; threadId++) {
                        Particle tempParticle = particles.get(j);
                        tempParticle.forceX += threadForcesX[threadId][j];
                        tempParticle.forceY += threadForcesY[threadId][j];
                    }
                }
                mergeLatch.countDown();
            });
        }

        try {
            mergeLatch.await();
        }
        catch (Exception e) {
            Thread.currentThread().interrupt();
            Logger.error("Particle force merge interrupted: " + e.getMessage());
        }


        for (int i = 0; i < config.numOfParticles; i++) {
            applyBoundaryForces(particles.get(i));
        }
    }

    private void applyForceParallel(int i, int j, double[] forcesX, double[] forcesY) {
        double distanceX = particles.get(j).x - particles.get(i).x;
        double distanceY = particles.get(j).y - particles.get(i).y;
        double distanceSquared = distanceX * distanceX + distanceY * distanceY;
        double distance = Math.sqrt(distanceSquared);

        if (distance < MINIMUM_DISTANCE) {
            distance = MINIMUM_DISTANCE;
            distanceSquared = distance * distance;
        }

        double particleAttraction = (particles.get(i).charge * particles.get(j).charge) / distanceSquared;
        double forceX = particleAttraction * (distanceX / distance);
        double forceY = particleAttraction * (distanceY / distance);

        forcesX[i] += forceX;
        forcesY[i] += forceY;
        forcesX[j] -= forceX;
        forcesY[j] -= forceY;
    }

    private void applyBoundaryForces(Particle particle) {
        double boundaryForce = config.boundaryCharge;
        double wallMargin = 15.0;

        /// Left wall
        if (particle.x < wallMargin) {
            double distance;
            if (particle.x < 1) {
                distance = 1;
            }
            else {
                distance = particle.x;
            }
            particle.forceX += boundaryForce / (distance * distance);
        }

        /// Right wall
        if (particle.x > config.width - wallMargin) {
            double distance;
            if (config.width - particle.x < 1) {
                distance = 1;
            }
            else {
                distance = config.width - particle.x;
            }
            particle.forceX -= boundaryForce / (distance * distance);

        }

        /// Ceiling
        if (particle.y < wallMargin) {
            double distance;
            if (particle.y < 1) {
                distance = 1;
            }
            else {
                distance = particle.y;
            }
            particle.forceY += boundaryForce / (distance * distance);
        }

        /// Floor
        if (particle.y > config.height - wallMargin) {
            double distance;
            if (config.height - particle.y < 1) {
                distance = 1;
            }
            else {
                distance = config.height - particle.y;
            }
            particle.forceY -= boundaryForce / (distance * distance);
        }
    }

    private void updatePositions() {
        for (int i = 0; i < config.numOfParticles; i++) {
            Particle currentParticle = particles.get(i);
            currentParticle.velocityX += currentParticle.forceX * SLOW_DOWN;
            currentParticle.velocityY += currentParticle.forceY * SLOW_DOWN;

            if (config.clumping) {
                currentParticle.velocityX = currentParticle.velocityX * DAMPING;
                currentParticle.velocityY = currentParticle.velocityY * DAMPING;
            }

            double particleSpeed = Math.sqrt(currentParticle.velocityX * currentParticle.velocityX + currentParticle.velocityY * currentParticle.velocityY);

            if (particleSpeed > MAXIMUM_SPEED) {
                double maxVelocityMultiplier = MAXIMUM_SPEED / particleSpeed;
                currentParticle.velocityX *= maxVelocityMultiplier;
                currentParticle.velocityY *= maxVelocityMultiplier;
            }

            currentParticle.x += currentParticle.velocityX * SLOW_DOWN;
            currentParticle.y += currentParticle.velocityY * SLOW_DOWN;

            if (currentParticle.x <= 0) {
                currentParticle.x = 0;
                currentParticle.velocityX = Math.abs(currentParticle.velocityX) * 0.2;
            }
            else if (currentParticle.x >= config.width) {
                currentParticle.x = config.width;
                currentParticle.velocityX = -1 * Math.abs(currentParticle.velocityX) * 0.2;
            }

            if (currentParticle.y <= 0) {
                currentParticle.y = 0;
                currentParticle.velocityY = Math.abs(currentParticle.velocityY) * 0.2;
            }
            else if (currentParticle.y >= config.height) {
                currentParticle.y = config.height;
                currentParticle.velocityY = -1 * Math.abs(currentParticle.velocityY) * 0.2;
            }
        }
    }
}