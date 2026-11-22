/* (C)2026 */
package frc.robot.subsystems;

import frc.robot.Constants.INDEXER;
import frc.robot.helpers.SparkFlexControl;
import org.littletonrobotics.junction.Logger;
import org.wpilib.command2.Command;
import org.wpilib.command2.SubsystemBase;

public class Indexer extends SubsystemBase {
    private SparkFlexControl spinMotor;

    public boolean indexerRunning;
    public boolean reverseIndexerRunning;

    public Indexer() {
        spinMotor = new SparkFlexControl(INDEXER.SPINNER_CAN_ID, true);
        spinMotor.setInverted();
        spinMotor.setCurrentLimit(INDEXER.SPIN_CURRENT_LIMIT);

        // To Tune PID
        // SmartDashboard.putNumber("P_SPINNER", INDEXER.SPIN_P);
        // SmartDashboard.putNumber("I_SPINNER", INDEXER.SPIN_I);
        // SmartDashboard.putNumber("D_SPINNER", INDEXER.SPIN_D);
        // SmartDashboard.putNumber("S_SPINNER", INDEXER.SPIN_S);
        // SmartDashboard.putNumber("VEL_SPINNER", INDEXER.SPIN_MOTOR_SPEED);

        spinMotor.setPIDF(
                INDEXER.SPIN_P,
                INDEXER.SPIN_I,
                INDEXER.SPIN_D,
                INDEXER.SPIN_S,
                INDEXER.SPIN_V,
                INDEXER.SPIN_A,
                INDEXER.SPIN_G,
                INDEXER.SPIN_ALLOWED_ERROR);
    }

    @Override
    public void periodic() {
        spinMotor.motorLogging(INDEXER.LOG_PATH);
        Logger.recordOutput(INDEXER.LOG_PATH + "indexerRunning", indexerRunning);
    }

    public Command stopCommand() {
        return this.runOnce(() -> spinMotor.stop())
                .andThen(
                        () -> {
                            indexerRunning = false;
                        });
    }

    public Command runIndexerCommand() {
        return this.runOnce(() -> spinMotor.setVoltage(11.0))
                .andThen(
                        () -> {
                            indexerRunning = true;
                        });
    }

    public Command runReverseIndexerCommand() {
        return this.runOnce(() -> spinMotor.setVoltage(-11.0))
                .andThen(
                        () -> {
                            reverseIndexerRunning = true;
                        });
    }

    public Command jammingIndexerCommand() {
        return this.runOnce(() -> spinMotor.getCurrent())
                .andThen(
                        () -> {
                            indexerJamming = true;
                        });
    }

    public double getIndexerCurrent() {
        return spinMotor.getCurrent();
    }
}
