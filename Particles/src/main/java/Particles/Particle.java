package Particles;

public class Particle {
    double x;
    double y;
    double velocityX;
    double velocityY;
    double forceX;
    double forceY;
    double charge;

    public Particle(double x, double y, double velocityX, double velocityY, double charge) {
        this.x = x;
        this.y = y;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.charge = charge;
        this.forceX = 0;
        this.forceY = 0;
    }
}

