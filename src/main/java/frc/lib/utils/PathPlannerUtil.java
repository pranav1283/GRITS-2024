package frc.lib.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import com.pathplanner.lib.auto.AutoBuilder;
import com.pathplanner.lib.auto.NamedCommands;
import com.pathplanner.lib.util.HolonomicPathFollowerConfig;
import com.pathplanner.lib.util.ReplanningConfig;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.networktables.DoubleArraySubscriber;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Filesystem;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import frc.robot.Constants.SwerveConstants;
import frc.robot.generated.TunerConstants;
import frc.robot.subsystems.Drive;
import frc.robot.subsystems.Shooter;


public class PathPlannerUtil {
    private static final DoubleArraySubscriber kTargetPoseSub = NetworkTableInstance.getDefault()
        .getDoubleArrayTopic("/Pathplanner/targetPose")
        .subscribe(new double[] {0,0,0});

    public static void configure(Drive drive, Shooter shooter){
        HolonomicPathFollowerConfig config = new HolonomicPathFollowerConfig(
            SwerveConstants.translationalPID, 
            SwerveConstants.rotationalPID, 
            TunerConstants.kSpeedAt12VoltsMps, 
            SwerveConstants.driveBaseRadiusMeter, 
            new ReplanningConfig(true, true)
        );
        
        AutoBuilder.configureHolonomic(
            drive::getPose,
            drive::resetPose,
            drive::getChassisSpeeds,
            drive::driveRobotCentric,
            config,
            () -> DriverStation.getAlliance().get() == Alliance.Red,
            drive
        );

        NamedCommands.registerCommand("brake", drive.brakeCommand());
        NamedCommands.registerCommand("pickUpNote",Commands.print("Running intakes in"));
        NamedCommands.registerCommand("intakeOut", Commands.print("Running Intake Out"));
        // NamedCommands.registerCommand("stopIntake", intake.stopIntakeCommand());
        NamedCommands.registerCommand("stopIntake", Commands.print("Stopping Intake"));
        // NamedCommands.registerCommand("armLow", intake.setArmLowCommand());
        NamedCommands.registerCommand("armLow", Commands.print("Setting intake Arm Low"));
        // NamedCommands.registerCommand("armHigh", intake.setArmHighCommand());
        NamedCommands.registerCommand("armHigh", Commands.print("Setting intake Arm High"));
        // NamedCommands.registerCommand("shoot", shooter.shootCommand().withTimeout(5));
        NamedCommands.registerCommand("shoot", Commands.print("Running Shooter Out"));
        // NamedCommands.registerCommand("shooterIn", shooter.shootCommand());
        NamedCommands.registerCommand("shooterIn", Commands.print("Running SHooter in"));
        // NamedCommands.registerCommand("shooterLow", shooter.setArmIn());
        NamedCommands.registerCommand("shooterLow", Commands.print("Setting Shooter Low"));
        // NamedCommands.registerCommand("shooterHigh", shooter.setArmOut());
        NamedCommands.registerCommand("shooterHigh", Commands.print("Setting Shooter High"));
        NamedCommands.registerCommand("elevatorHigh", Commands.print("elevatorHigh"));
        NamedCommands.registerCommand("elevatorMid", Commands.print("elevatorMed"));
        NamedCommands.registerCommand("elevatorLow", Commands.print("elevatorLow"));
    }

    public static Command getAutoCommand(String name){
        try {
            return AutoBuilder.buildAuto(name);
        } catch (Exception e) {
            // TODO: handle exception
            DriverStation.reportError("An error loaded while loading path planner auto", e.getStackTrace());
            return Commands.none();
        }
    }

    public static List<String> getAutos(){
        var path = Path.of(Filesystem.getDeployDirectory().getAbsolutePath(), "pathplanner");
        try(Stream<Path> stream = Files.walk(path)){
            return stream.filter(x -> getFileExtension(x).equals(".auto")) .map(x -> getFileStem(x)) .toList();
        }catch(IOException e){
            return Collections.emptyList();
        }
    }

    private static String getFileExtension(Path path){
        try{
            String name = path.getFileName().toString();
            return name.substring(name.lastIndexOf("."));
        } catch(Exception e){
            return "";
        }
    }

    private static String getFileStem(Path path){
        try{
            String name = path.getFileName().toString();
            return name.substring(0,name.lastIndexOf("."));
        }catch(Exception e){
            return "";
        }
    }

    public static Pose2d getCurrentTargetPose(){
        var arr = kTargetPoseSub.get();
        double x = arr[0];
        double y = arr[1];
        Rotation2d rot = Rotation2d.fromRadians(arr[2]);
        return new Pose2d(x,y,rot); 
    }

}
