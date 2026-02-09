package Particles;

import Utils.Logger;
import mpi.MPI;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ParticleSimulationDistributed {
    private final List<Particle> particles;
    private final SimulationConfig config;
    private final Random random;
    private final double DAMPING;
    private final double MINIMUM_DISTANCE;
    private final double MAXIMUM_SPEED;

    private static final double SLOW_DOWN = 0.1;


    private boolean firstCollect = true;
    private int cyclesPerSecond = 0;
    private int numberOfCompleteCycles = 0;
    private long startTime;
    private int chargeModifier = 1;

    public ParticleSimulationDistributed(SimulationConfig config) {
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

    public void runDistributed() {
        int rank = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();

        int particlesPerProcess = config.numOfParticles / size;
        int start = rank * particlesPerProcess;
        int end;

        if (rank == size - 1) {
            end = config.numOfParticles;
        }
        else {
            end = (rank + 1) * particlesPerProcess;
        }


        GUI gui = null;
        if (config.enableGUI && MPI.COMM_WORLD.Rank() == 0) {
            gui = new GUI(config, particles);
            gui.start();
        }

        double[] allPositionsX = new double[config.numOfParticles];
        double[] allPositionsY = new double[config.numOfParticles];
        double[] allCharges = new double[config.numOfParticles];
        double[] allVelocitiesX = new double[config.numOfParticles];
        double[] allVelocitiesY = new double[config.numOfParticles];

        for (int cycle = 0; cycle < config.cycles; cycle++) {
            gatherAllParticleData(allPositionsX, allPositionsY, allCharges, allVelocitiesX, allVelocitiesY, size, start, end);

            computeForcesDistributed(start, end, allPositionsX, allPositionsY, allCharges);

            updatePositions(start, end);

            if (rank == 0) {
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
        }

        if (rank == 0 && gui != null) {
            gui.stop();
        }

    }

    private void gatherAllParticleData(double[] allPositionsX, double[] allPositionsY, double[] allCharges, double[] allVelocitiesX, double[] allVelocitiesY, int size, int start, int end) {
        int myCount = end - start;
        double[] threadPositionsX = new double[myCount];
        double[] threadPositionsY = new double[myCount];
        double[] threadCharges = new double[myCount];
        double[] threadVelocitiesX = new double[myCount];
        double[] threadVelocitiesY = new double[myCount];


        for (int i = start; i < end; i++) {
            Particle tempParticle = particles.get(i);
            int localIndex = i - start;
            threadPositionsX[localIndex] = tempParticle.x;
            threadPositionsY[localIndex] = tempParticle.y;
            threadCharges[localIndex] = tempParticle.charge;
            threadVelocitiesX[localIndex] = tempParticle.velocityX;
            threadVelocitiesY[localIndex] = tempParticle.velocityY;
        }

        int[] receiveCounts = new int[size];
        int[] receiveDisplacements = new int[size];

        for (int i = 0; i < size; i++) {
            int threadStart = i * (config.numOfParticles / size);
            int threadEnd;

            if(i == size - 1) {
                threadEnd = config.numOfParticles;
            }
            else{
                threadEnd = (i + 1) * (config.numOfParticles / size);
            }
            int count = threadEnd - threadStart;

            receiveCounts[i] = count;
            receiveDisplacements[i] = threadStart;
        }

        MPI.COMM_WORLD.Allgatherv(threadPositionsX, 0, myCount, MPI.DOUBLE, allPositionsX, 0, receiveCounts, receiveDisplacements, MPI.DOUBLE);
        MPI.COMM_WORLD.Allgatherv(threadPositionsY, 0, myCount, MPI.DOUBLE, allPositionsY, 0, receiveCounts, receiveDisplacements, MPI.DOUBLE);
        if(firstCollect){
            MPI.COMM_WORLD.Allgatherv(threadCharges, 0, myCount, MPI.DOUBLE, allCharges, 0, receiveCounts, receiveDisplacements, MPI.DOUBLE);
        }
        MPI.COMM_WORLD.Allgatherv(threadVelocitiesX, 0, myCount, MPI.DOUBLE, allVelocitiesX, 0, receiveCounts, receiveDisplacements, MPI.DOUBLE);
        MPI.COMM_WORLD.Allgatherv(threadVelocitiesY, 0, myCount, MPI.DOUBLE, allVelocitiesY, 0, receiveCounts, receiveDisplacements, MPI.DOUBLE);

        for (int i = 0; i < config.numOfParticles; i++) {
            Particle tempParticle = particles.get(i);
            tempParticle.x = allPositionsX[i];
            tempParticle.y = allPositionsY[i];
            if(firstCollect){
                tempParticle.charge = allCharges[i];
                firstCollect = false;
            }
            tempParticle.velocityX = allVelocitiesX[i];
            tempParticle.velocityY = allVelocitiesY[i];
        }
    }

    private void computeForcesDistributed(int start, int end, double[] allPositionsX, double[] allPositionsY, double[] allCharges) {
        for (int i = start; i < end; i++) {
            particles.get(i).forceX = 0;
            particles.get(i).forceY = 0;
        }

        for (int i = start; i < end; i++) {
            for (int j = 0; j < config.numOfParticles; j++) {
                if (i != j) {
                    applyForceDistributed(i, j, allPositionsX, allPositionsY, allCharges);
                }
            }
            applyBoundaryForces(particles.get(i));
        }
    }

    private void applyForceDistributed(int i, int j, double[] allPositionsX, double[] allPositionsY, double[] allCharges) {
        Particle particle1 = particles.get(i);

        double particle2X = allPositionsX[j];
        double particle2Y = allPositionsY[j];
        double particle2Charge = allCharges[j];

        double distanceX = particle2X - particle1.x;
        double distanceY = particle2Y - particle1.y;
        double distanceSquared = distanceX * distanceX + distanceY * distanceY;
        double distance = Math.sqrt(distanceSquared);

        if (distance < MINIMUM_DISTANCE) {
            distance = MINIMUM_DISTANCE;
            distanceSquared = distance * distance;
        }

        double particleAttraction = (particle1.charge * particle2Charge) / distanceSquared;

        double fx = particleAttraction * (distanceX / distance);
        double fy = particleAttraction * (distanceY / distance);

        particle1.forceX += fx;
        particle1.forceY += fy;
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

    private void updatePositions(int start, int end) {
        for (int i = start; i < end; i++) {
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