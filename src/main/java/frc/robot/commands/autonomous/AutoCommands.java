/* (C)2026 */
package frc.robot.commands.autonomous;

import com.pathplanner.lib.auto.NamedCommands;
import com.pathplanner.lib.events.EventTrigger;
import frc.robot.subsystems.Scoring;
import java.util.Set;
import org.wpilib.command2.Command;
import org.wpilib.command2.CommandScheduler;
import org.wpilib.command2.Commands;

public final class AutoCommands {
    public static void registerNamedCommands(Scoring scoring) {
        NamedCommands.registerCommand("Shoot", scoring.indexer.runIndexerCommand());
        NamedCommands.registerCommand("StopShoot", scoring.indexer.stopCommand());
        NamedCommands.registerCommand("ToggleHubTracking", scoring.toggleTrackingCommand());

        NamedCommands.registerCommand(
                "SqueezeShoot",
                Commands.defer(
                        () -> {
                            Command squeezeRoutine =
                                    Commands.sequence(
                                            scoring.intake.retractWithRollersCommand(),
                                            Commands.waitSeconds(1.5),
                                            scoring.intake.stopRollerCommand(),
                                            scoring.intake.stopExtendCommand());

                            return Commands.sequence(
                                    scoring.indexer.runIndexerCommand(),
                                    Commands.runOnce(
                                            () ->
                                                    CommandScheduler.getInstance()
                                                            .schedule(squeezeRoutine)));
                        },
                        Set.of()));
    }

    public static void registerEventTriggers(Scoring scoring) {
        new EventTrigger("RunIntake").onTrue(scoring.intake.runIntakeRollersCommand());
        new EventTrigger("DeployIntake")
                .onTrue(scoring.intake.extendIntakeCommand().withTimeout(0.05));
        new EventTrigger("StopIntake")
                .onTrue(
                        scoring.intake
                                .stopRollerCommand()
                                .andThen(scoring.intake.stopExtendCommand()));
    }

    public static void registerAll(Scoring scoring) {
        registerNamedCommands(scoring);
        registerEventTriggers(scoring);
    }

    private AutoCommands() {}
}
