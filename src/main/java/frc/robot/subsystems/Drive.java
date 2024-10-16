// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;


import static edu.wpi.first.units.Units.Volts;


import java.util.function.Supplier;

import com.ctre.phoenix6.SignalLogger;
import com.ctre.phoenix6.mechanisms.swerve.SwerveRequest.SysIdSwerveRotation;
import com.ctre.phoenix6.mechanisms.swerve.SwerveRequest.SysIdSwerveTranslation;

import edu.wpi.first.math.filter.SlewRateLimiter;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import edu.wpi.first.wpilibj2.command.sysid.SysIdRoutine;
import edu.wpi.first.wpilibj2.command.sysid.SysIdRoutine.Direction;

import frc.lib.swerve.Swerve;
import frc.lib.swerve.SwerveConfig;
import frc.lib.utils.FieldUtil;
import frc.lib.utils.PathPlannerUtil;
//import frc.robot.Vision;
import frc.robot.Constants.SwerveConstants;
import frc.robot.io.DriverControls;

public class Drive extends SubsystemBase {
  public static double limit = 1;
  private Swerve swerve;
  private FieldUtil fieldUtil = FieldUtil.getField();
  private boolean sysIdTranslator = true;
  private final SysIdSwerveTranslation translation = new SysIdSwerveTranslation();
  private final SysIdRoutine sysIdTranslation = new SysIdRoutine(
    new SysIdRoutine.Config(
      null, 
      Volts.of(7),
      null,
      null),  
    new SysIdRoutine.Mechanism(
      (volts) -> swerve.setControl(translation.withVolts(volts)),
      null,
      this)
    );
  private final SysIdSwerveRotation rotation = new SysIdSwerveRotation();
  private final SysIdRoutine sysIdRotation = new SysIdRoutine(
    new SysIdRoutine.Config(
      null,
      Volts.of(7),
      null,
      null),
    new SysIdRoutine.Mechanism(
      (volts) -> swerve.setControl(rotation.withVolts(volts)),
      null,
      this));

  private SlewRateLimiter forwardLimiter, strafeLimiter;
  /** Creates a new Drive */
  public Drive(Swerve swerve) {
    SignalLogger.setPath("logs/sysid/drive");
    this.swerve = swerve;

    forwardLimiter = new SlewRateLimiter(5, -10, 0);
    strafeLimiter = new SlewRateLimiter(5, -10, 0);
    swerve.setPigeonOffset();
  }

  @Override
  public void periodic() {
    // This method will be called once per scheduler run
  }

  @Override
  public void simulationPeriodic() {
    // This method will be called once per scheduler run during simulation
    Pose2d targetPose = PathPlannerUtil.getCurrentTargetPose();
    fieldUtil.setSwerveRobotPose(swerve.getPose2d(),swerve.getModuleStates(), SwerveConstants.modulePositions);
    fieldUtil.setObjectGlobalPose("Target Pose", targetPose);
    swerve.updateSimState(0.02, 12);
  }

  
  /** 
   * @param speeds
   */
  public void driveFieldCentric(ChassisSpeeds speeds){
    swerve.setControl(
      SwerveConfig.drive
      .withVelocityX(forwardLimiter.calculate(speeds.vxMetersPerSecond))
      .withVelocityY(strafeLimiter.calculate(speeds.vyMetersPerSecond))
      .withRotationalRate(speeds.omegaRadiansPerSecond)
    );
  }

  public void driveRobotCentric(ChassisSpeeds speeds){
    swerve.setControl(
      SwerveConfig.robotCentric
      .withVelocityX(forwardLimiter.calculate(speeds.vxMetersPerSecond))
      .withVelocityY(strafeLimiter.calculate(speeds.vyMetersPerSecond))
      .withRotationalRate(speeds.omegaRadiansPerSecond)
    );
  }

  public void brake(){
    swerve.setControl(SwerveConfig.brake);
  }

  public Rotation2d geRotation2d(){
    return swerve.getPose2d().getRotation();
  }

  public void increaseLimit(){
    if(limit < 1){
      limit += 0.2;
    }
  }

  public void decreaseLimit(){
    if (limit > 0.2){
      limit -= 0.2;
    }
  }

  public Command increaseLimitCommand(){
    return Commands.runOnce(this::increaseLimit);
  }

  public Command decreaseLimitCommand(){
    return Commands.runOnce(this::decreaseLimit);
  }

  public Command driveFieldCentricCommand(Supplier<ChassisSpeeds> chassisSpeeds){
    return run(() -> driveFieldCentric(chassisSpeeds.get()));
  }

  public Command driveRobotCentricCommand(Supplier<ChassisSpeeds> chassisSpeeds){
    return run(() -> driveRobotCentric(chassisSpeeds.get()));
  }

  public ChassisSpeeds getChassisSpeeds(){
    return swerve.getChassisSpeeds();
  }

  public Command brakeCommand(){
    return run(this::brake);
  }

  public Pose2d getPose(){
    return swerve.getPose2d();
  }

  public void resetPose(Pose2d pose){
    swerve.resetPose(pose);
  }

  public Command resetGyroCommand(){
    return swerve.zeroGyroCommand();
  }

  public Command sysIdDynamic(Direction direction){
    return sysIdTranslator ? sysIdTranslation.dynamic(direction) : sysIdRotation.dynamic(direction);
  }

  public Command sysIdQuasistatic(Direction direction){
    return sysIdTranslator ? sysIdTranslation.quasistatic(direction) : sysIdRotation.quasistatic(direction);
  }

  public Command toggleSysIdMode(){
    return Commands.runOnce(() -> sysIdTranslator = !sysIdTranslator);
  }

  public void targetAngleDrive(Translation2d targetAngle, DriverControls controls){
    swerve.targetAngleDrive(targetAngle, controls.driveForward(), controls.driveStrafe());
  }

  public void targetAngleDrive(Rotation2d targetAngle, DriverControls controls){
    swerve.targetAngleDrive(targetAngle, controls.driveForward(), controls.driveStrafe());
  }

  //public void addVisionMeasurement(){}

}
