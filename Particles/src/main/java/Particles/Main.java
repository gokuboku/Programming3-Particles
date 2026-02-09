package Particles;

import Utils.Logger;
import mpi.MPI;

public class Main {

    public static void main(String[] args) {
        SimulationConfig config = parseArgs(args);

        long startTime = System.currentTimeMillis();
        if(config.mode == SimulationMode.SEQUENTIAL){
            ParticleSimulationSequential sequentialSimulation = new ParticleSimulationSequential(config);
            sequentialSimulation.runSequential();
        }
        else if (config.mode == SimulationMode.PARALLEL){
            ParticleSimulationParallel parallelSimulation = new ParticleSimulationParallel(config);
            parallelSimulation.runParallel();
        }
        else if (config.mode == SimulationMode.DISTRIBUTED){
            MPI.Init(args);
            ParticleSimulationDistributed distributedSimulation = new ParticleSimulationDistributed(config);
            distributedSimulation.runDistributed();
        }

        long endTime = System.currentTimeMillis();

        if(config.mode == SimulationMode.DISTRIBUTED && MPI.COMM_WORLD.Rank() == 0) {
            Logger.info("Simulation completed in " + (endTime - startTime) + " ms");
            Logger.info("Cycles: " + config.cycles);
            Logger.info("Particles: " + config.numOfParticles);
            Logger.info("Average calculations per second: " + Math.round((double)config.cycles / ((double)(endTime - startTime) / 1000)));
        }
        else if(config.mode == SimulationMode.SEQUENTIAL ||  config.mode == SimulationMode.PARALLEL){
            Logger.info("Simulation completed in " + (endTime - startTime) + " ms");
            Logger.info("Cycles: " + config.cycles);
            Logger.info("Particles: " + config.numOfParticles);
            Logger.info("Average calculations per second: " + Math.round((double)config.cycles / ((double)(endTime - startTime) / 1000)));
        }

        if(config.mode == SimulationMode.DISTRIBUTED){
            MPI.Finalize();
        }
        System.exit(0);
    }

    private static SimulationConfig parseArgs(String[] args) {
        SimulationConfig config = new SimulationConfig();

        for (int i = 0; i < args.length; i++) {
            try{
                if(args[i].equals("--mode")){
                    config.mode = SimulationMode.valueOf(args[i+1].toUpperCase());
                }
                else if(args[i].equals("--particles")){
                    config.numOfParticles = Integer.parseInt(args[i+1]);
                }
                else if(args[i].equals("--cycles")){
                    config.cycles = Integer.parseInt(args[i+1]);
                }
                else if(args[i].equals("--gui")){
                    config.enableGUI = Boolean.parseBoolean(args[i+1]);
                }
                else if(args[i].equals("--width")){
                    config.width = Integer.parseInt(args[i+1]);
                }
                else if(args[i].equals("--height")){
                    config.height = Integer.parseInt(args[i+1]);
                }
                else if(args[i].equals("--seed")){
                    config.particleSeed = Long.parseLong(args[i+1]);
                }
                else if(args[i].equals("--clumping")){
                    config.clumping =  Boolean.parseBoolean(args[i+1]);
                }
                else if(args[i].equals("--boundary")){
                    config.boundaryCharge = Double.parseDouble(args[i+1]);
                }
                else if(args[i].equals("--damping")){
                    config.damping = Double.parseDouble(args[i+1]);
                }
                else if(args[i].equals("--minDistance")){
                    config.minimumDistance = Double.parseDouble(args[i+1]);
                }
                else if(args[i].equals("--maxSpeed")){
                    config.maximumSpeed = Double.parseDouble(args[i+1]);
                }
            }
            catch (Exception e){
                Logger.error(e.getMessage());
            }
        }

        return config;
    }
}