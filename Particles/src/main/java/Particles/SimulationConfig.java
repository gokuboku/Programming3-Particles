package Particles;

public class SimulationConfig  {

    /// Parameter used to set wanted mode of simulation
    /// Possible values:
    /// SimulationMode.SEQUENTIAL
    /// SimulationMode.PARALLEL
    /// SimulationMode.DISTRIBUTED
    SimulationMode mode = SimulationMode.DISTRIBUTED;

    /// Parameter to toggle on/off GUI
    /// Possible values:
    /// true - GUI is rendered
    /// false - GUI is not rendered
    /// about 20% difference
    boolean enableGUI = false;

    /// Only used for visual esthetics
    /// nicer to look at
    /// Possible values:
    /// true - forces particles to start clumping by reducing their speed a tiny amount
    /// false - does not force particles to clump makes a visual mess
    boolean clumping = false;

    /// Damping used for creating clumping effect
    double damping = 0.99995;

    /// Parameter to set number of particles, fewer particles better performance
    /// For testing by limiting particles
    /// set to - 3000
    /// For testing by limiting cycles
    /// set to - 500 increase by 500 every test
    int numOfParticles = 500;

    /// Minimum distance between particles to avoid division by 0
    double minimumDistance = 5;

    /// Maximum particle speed, more than 5 creates a lot of chaos
    double maximumSpeed = 5;

    /// Parameter to set number of computation cycles the program does
    /// For testing by limiting particles
    /// set to - 500 increase by 500 every run
    /// For testing by limiting cycles
    /// set to - 10000
    int cycles = 100000;

    /// Width of frame if GUI is enabled
    /// default - 800
    int width = 800;

    /// Height of frame if GUI is enabled
    /// defualt - 600;
    int height = 600;

    /// Seed for creating particles at random locations and with random starting velocities
    /// Default - current time in milliseconds
    long particleSeed = System.currentTimeMillis();

    /// Charge big enough to negate particle velocity and push it to the opposite side.
    /// Anything below 100 promotes clumping
    /// Anything above 100 creates chaos
    double boundaryCharge = 1000.0;
}
