/* (C)2026 */
package frc.robot.commands.proxies;

import frc.robot.Robot;
import org.wpilib.command2.Command;
import org.wpilib.command2.Commands;
import org.wpilib.command2.Subsystem;
import org.wpilib.command2.WaitCommand;
import org.wpilib.command2.WrapperCommand;

public class TimingSimulatedCommand extends WrapperCommand {
    /**
     * If running in simulation, replaces the passed-in command with a WaitCommand that waits for a
     * length of your choosing.
     *
     * @param command the command to run on a real robot
     * @param time the time to wait (in seconds) if running in simulation
     *
     * @apiNote This command will require the same subsystems as the passed-in command, even if
     *          running as a WaitCommand.
     *
     * @apiNote This command wraps the passed-in command. If this command is cancelled, the
     *          passed-in command will be cancelled too.
     */
    public TimingSimulatedCommand(Command command, double time) {
        super(Robot.isReal() ? command : new WaitCommand(time));
        if (!this.getRequirements().equals(command.getRequirements())) {
            this.addRequirements(command.getRequirements().toArray(new Subsystem[0]));
        }
    }

    /**
     * If running in simulation, replaces the passed-in command with {@link Commands#none()}
     *
     * @param command the command to run if on a real robot
     */
    public TimingSimulatedCommand(Command command) {
        super(Robot.isReal() ? command : Commands.none());
    }
}
