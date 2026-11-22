/* (C)2026 */
package frc.robot.helpers;

import org.wpilib.math.kinematics.ChassisVelocities;

public class SmoothingFilter {
    double[] velocityXValues;
    double[] velocityYValues;
    double[] velocityOmegaValues;
    int sizeX;
    int sizeY;
    int sizeOmegas;
    int index = 0;

    /**
     * Create smoothing object, will slowly and smoothly adjust speed values until target is hit
     *
     * @param sizeX Size for X velocity smoothing array, bigger will be smoothed slower
     * @param sizeY Size for Y velocity smoothing array, bigger will be smoothed slower
     * @param sizeOmegas Size for Omega velocity smoothing array, bigger will be smoothed slower
     */
    public SmoothingFilter(int sizeX, int sizeY, int sizeOmegas) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeOmegas = sizeOmegas;
        velocityXValues = new double[sizeX];
        velocityYValues = new double[sizeY];
        velocityOmegaValues = new double[sizeOmegas];
    }

    /**
     * Take an array of zeros and fill each slot with a speed value until the value is hit
     *
     * @param desiredSpeed Speed to accelerate towards
     */
    public ChassisVelocities smooth(ChassisVelocities desiredSpeed) {
        double smoothedX = 0;
        double smoothedY = 0;
        double smoothedOmegas = 0;
        if (!desiredSpeed.equals(null)) {
            smoothedX = smoothX(desiredSpeed.vx);
            smoothedY = smoothY(desiredSpeed.vy);
            smoothedOmegas = smoothOmega(desiredSpeed.omega);
        }
        index++;
        return new ChassisVelocities(smoothedX, smoothedY, smoothedOmegas);
    }

    public double smoothX(double desiredSpeedX) {
        double sum = 0;
        velocityXValues[index % sizeX] = desiredSpeedX;
        for (int i = 0; i < velocityXValues.length; i++) {
            sum += velocityXValues[i];
        }
        return sum / sizeX;
    }

    public double smoothY(double desiredSpeedY) {
        double sum = 0;
        velocityYValues[index % sizeY] = desiredSpeedY;
        for (int i = 0; i < velocityYValues.length; i++) {
            sum += velocityYValues[i];
        }
        return sum / sizeY;
    }

    public double smoothOmega(double desiredSpeedOmega) {
        double sum = 0;
        velocityOmegaValues[index % sizeOmegas] = desiredSpeedOmega;
        for (int i = 0; i < velocityOmegaValues.length; i++) {
            sum += velocityOmegaValues[i];
        }
        return sum / sizeOmegas;
    }
}
