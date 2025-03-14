// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import static edu.wpi.first.units.Units.*;

import com.ctre.phoenix6.swerve.SwerveModule.DriveRequestType;
import com.ctre.phoenix6.swerve.SwerveRequest;

import com.pathplanner.lib.auto.AutoBuilder;
import com.pathplanner.lib.auto.NamedCommands;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.WaitUntilCommand;
import frc.robot.LimelightHelpers.PoseEstimate;
import frc.robot.commands.EffectorPivot;
import frc.robot.commands.EjectCoral;
import frc.robot.commands.LoadCoral;
import frc.robot.commands.IntakeAlgae;
import frc.robot.generated.TunerConstants;
import frc.robot.subsystems.CommandSwerveDrivetrain;
import frc.robot.subsystems.ElevatorSubsystem;
import frc.robot.subsystems.EndEffector;
import frc.robot.subsystems.Limelight;

public class RobotContainer {

    private double MaxSpeed = TunerConstants.kSpeedAt12Volts.in(MetersPerSecond); // kSpeedAt12Volts desired top speed
    private double MaxAngularRate = RotationsPerSecond.of(1.6).in(RadiansPerSecond); // 1.6 rotations per second max angular velocity

    PoseEstimate llMeasurement = LimelightHelpers.getBotPoseEstimate_wpiBlue("limelight");

    /* Setting up bindings for necessary control of the swerve drive platform */
    private final SwerveRequest.FieldCentric drive = new SwerveRequest.FieldCentric()
            .withDeadband(MaxSpeed * 0.1).withRotationalDeadband(MaxAngularRate * 0.1) // Add a 10% deadband
            .withDriveRequestType(DriveRequestType.OpenLoopVoltage); // Use open-loop control for drive motors
    private final SwerveRequest.SwerveDriveBrake brake = new SwerveRequest.SwerveDriveBrake();

    private final Telemetry logger = new Telemetry(MaxSpeed);
    Field2d field2d = new Field2d();

    public final CommandSwerveDrivetrain drivetrain = TunerConstants.createDrivetrain();
    public final ElevatorSubsystem elevator = new ElevatorSubsystem();
    public final EndEffector effector = new EndEffector();
    public final Limelight limelight = new Limelight("limelight", drivetrain);

    /* Path follower */
    private final SendableChooser<Command> autoChooser;

    public RobotContainer() {
        
        NamedCommands.registerCommand("L1", Commands.runOnce(() -> elevator.setLevelOne()));
        NamedCommands.registerCommand("L2", Commands.runOnce(() -> elevator.setLevelTwo()));
        NamedCommands.registerCommand("L3", Commands.runOnce(() -> elevator.setLevelThree()));
        NamedCommands.registerCommand("L4", Commands.runOnce(() -> elevator.setLevelFour()));
        
        NamedCommands.registerCommand("dispenseCoral", effector.setWheelVoltageCommand(-12));
        NamedCommands.registerCommand("waitForCoral", new WaitUntilCommand(effector.checkBeam));
        NamedCommands.registerCommand("loadCoral", effector.setWheelVoltageCommand(-7).andThen());
        NamedCommands.registerCommand("stopEffector", effector.setWheelVoltageCommand(0));
        NamedCommands.registerCommand("pivotEffector", Commands.runOnce(() -> new EffectorPivot(effector).execute()));
        
        NamedCommands.registerCommand("zeroGyro", Commands.runOnce(() -> drivetrain.seedFieldCentric()));

        autoChooser = AutoBuilder.buildAutoChooser("Backup");

        SmartDashboard.putData("Auto Mode", autoChooser);

        configureBindings();
    }

    private void configureBindings() {
        // Note that X is defined as forward according to WPILib convention,
        // and Y is defined as to the left according to WPILib convention.

        drivetrain.setDefaultCommand(
            // Drivetrain will execute this command periodically
            drivetrain.applyRequest(() ->
                drive.withVelocityX(-MathUtil.applyDeadband(Constants.OperatorConstants.driverController.getLeftY(), 0.1) * MaxSpeed) // Drive forward with negative Y (forward)
                    .withVelocityY(-MathUtil.applyDeadband(Constants.OperatorConstants.driverController.getLeftX(), 0.1) * MaxSpeed) // Drive left with negative X (left)
                    .withRotationalRate(-MathUtil.applyDeadband(Constants.OperatorConstants.driverController.getRightX(), 0.1) * MaxAngularRate) // Drive counterclockwise with negative X (left)
            )
        );

        Constants.OperatorConstants.driverController.b().whileTrue(drivetrain.applyRequest(() -> brake));

        Constants.OperatorConstants.driverController.leftBumper().onTrue(Commands.runOnce(() -> limelight.pathfindWithPath("Left").schedule()).unless(() -> limelight.tid.getDouble(0.0) <= 0));
        Constants.OperatorConstants.driverController.rightBumper().onTrue(Commands.runOnce(() -> limelight.pathfindWithPath("Right").schedule()).unless(() -> limelight.tid.getDouble(0.0) <= 0));
        Constants.OperatorConstants.driverController.y().onTrue(Commands.runOnce(() -> limelight.pathfindWithPath("Center").schedule()).unless(() -> limelight.tid.getDouble(0.0) <= 0));
        
        Constants.OperatorConstants.driverController.x().onTrue(Commands.runOnce(() -> CommandScheduler.getInstance().cancelAll()));
        Constants.OperatorConstants.driverController.a().onTrue(drivetrain.runOnce(() -> drivetrain.seedFieldCentric()));

        Constants.OperatorConstants.operatorController.leftTrigger()
            .onTrue(elevator.stopElevator().andThen(elevator.setVoltage(3)))
            .onFalse(elevator.setVoltage(0));
            Constants.OperatorConstants.operatorController.rightTrigger()
            .onTrue(elevator.stopElevator().andThen(elevator.setVoltage(-3)))
            .onFalse(elevator.setVoltage(0));

        // elevator.setDefaultCommand(new ControlElevatorWithJoystick(elevator));

        Constants.OperatorConstants.operatorController.leftBumper().onTrue(Commands.runOnce(() -> elevator.setLevelAlgaeLowPrep())).onFalse(Commands.runOnce(() -> elevator.setLevelAlgaeLow()));
        Constants.OperatorConstants.operatorController.rightBumper().onTrue(Commands.runOnce(() -> elevator.setLevelAlgaeHighPrep())).onFalse(Commands.runOnce(() -> elevator.setLevelAlgaeHigh()));

        Constants.OperatorConstants.operatorController.povUp().onTrue(Commands.runOnce(() -> elevator.setLevelThree()));
        Constants.OperatorConstants.operatorController.povLeft().onTrue(Commands.runOnce(() -> elevator.setLevelTwo()));
        Constants.OperatorConstants.operatorController.povRight().onTrue(Commands.runOnce(() -> elevator.setLevelFour()));
        Constants.OperatorConstants.operatorController.povDown().onTrue(Commands.runOnce(() -> elevator.setLevelOne()));

        Constants.OperatorConstants.operatorController.a().whileTrue(new LoadCoral(effector));
        Constants.OperatorConstants.operatorController.b().whileTrue(new EjectCoral(effector));
        Constants.OperatorConstants.operatorController.x().whileTrue(new IntakeAlgae(effector));
        Constants.OperatorConstants.operatorController.y().onTrue(Commands.runOnce(() -> new EffectorPivot(effector).execute()));
        
        drivetrain.registerTelemetry(logger::telemeterize);
    }

    public Command getAutonomousCommand() {
        /* Run the path selected from the auto chooser */
        return autoChooser.getSelected();
    }
}