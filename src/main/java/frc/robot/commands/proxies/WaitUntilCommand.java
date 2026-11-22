/* (C)2026 */
package frc.robot.commands.proxies;

import java.util.function.BooleanSupplier;
import org.wpilib.command2.Command;
import org.wpilib.command2.WaitCommand;
import org.wpilib.command2.WrapperCommand;

public class WaitUntilCommand extends WrapperCommand {

    public WaitUntilCommand(Command command, BooleanSupplier supplier) {
        super(new WaitCommand(Double.POSITIVE_INFINITY).until(supplier).andThen(command));
    }
}
