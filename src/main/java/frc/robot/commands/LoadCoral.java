// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.commands;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.WaitCommand;
import frc.robot.subsystems.EndEffector;

// import frc.robot.subsystems.CANLauncher;

/*This is an example of creating a command as a class. The base Command class provides a set of methods that your command
 * will override.
 */
public class LoadCoral extends Command {
  EndEffector m_effector;

  /** Creates a new LaunchNote. */
  public LoadCoral(EndEffector effector) {
    // save the launcher system internally
    m_effector = effector;

    // indicate that this command requires the launcher system
    addRequirements(m_effector);
  }

  // The initialize method is called when the command is initially scheduled.
  @Override
  public void initialize() {

  }

  // Called every time the scheduler runs while the command is scheduled.
  @Override
  public void execute() {
    if (m_effector.beamBreak.get()) {
      m_effector.setWheelVoltage(-7);
      m_effector.setConveyorVoltage(-6);
    } else {
      new WaitCommand(0.2).andThen(Commands.runOnce(() ->
      {
        m_effector.setWheelVoltage(0);
        m_effector.setConveyorVoltage(0);
      })).schedule();
    }
  }

  // Returns true when the command should end.
  @Override
  public boolean isFinished() {
    // Always return false so the command never ends on it's own. In this project we use the
    // scheduler to end the command when the button is released.
    return false;
  }
  
  @Override
  public void end(boolean interrupted) {
    // Stop the wheels when the command ends.
    m_effector.setWheelVoltage(0);
    m_effector.setConveyorVoltage(0);
  }
}
