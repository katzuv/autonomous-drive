package robot; /**
 * This is a very simple robot program that can be used to send telemetry to
 * the data_logger script to characterize your drivetrain. If you wish to use
 * your actual robot code, you only need to implement the simple logic in the
 * autonomousPeriodic function and change the NetworkTables update rate
 *
 * This program assumes that you are using TalonSRX motor controllers and that
 * the drivetrain encoders are attached to the TalonSRX
 */



import java.util.function.Supplier;

import com.ctre.phoenix.motorcontrol.FeedbackDevice;
import com.ctre.phoenix.motorcontrol.NeutralMode;
import com.ctre.phoenix.motorcontrol.can.WPI_TalonSRX;

import com.ctre.phoenix.motorcontrol.can.WPI_VictorSPX;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.RobotController;
import edu.wpi.first.wpilibj.SpeedControllerGroup;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.drive.DifferentialDrive;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

public class Robot extends TimedRobot {
    static private double WHEEL_DIAMETER = 0.1524;
    static private double ENCODER_PULSE_PER_REV = 4096;
    static private int PIDIDX = 0;

    Joystick stick;
    DifferentialDrive drive;

    public WPI_TalonSRX leftMaster;
    public WPI_TalonSRX rightMaster;

    Supplier<Double> leftEncoderPosition;
    Supplier<Double> leftEncoderRate;
    Supplier<Double> rightEncoderPosition;
    Supplier<Double> rightEncoderRate;

    NetworkTableEntry autoSpeedEntry = NetworkTableInstance.getDefault().getEntry("/robot/autospeed");
    NetworkTableEntry telemetryEntry = NetworkTableInstance.getDefault().getEntry("/robot/telemetry");

    double priorAutospeed = 0;
    Number[] numberArray = new Number[9];

    @Override
    public void robotInit() {

        stick = new Joystick(0);

         leftMaster = new WPI_TalonSRX(3);
        leftMaster.setInverted(false);
        leftMaster.setSensorPhase(false);
        leftMaster.setNeutralMode(NeutralMode.Coast);

        rightMaster = new WPI_TalonSRX(6);
        rightMaster.setInverted(false);
        rightMaster.setSensorPhase(true);
        rightMaster.setNeutralMode(NeutralMode.Coast);

        // left rear follows front
        WPI_VictorSPX leftSlave1 = new WPI_VictorSPX(4);
        leftSlave1.setInverted(false);
        leftSlave1.setSensorPhase(false);
        leftSlave1.follow(leftMaster);
        leftSlave1.setNeutralMode(NeutralMode.Coast);

        WPI_VictorSPX leftSlave2 = new WPI_VictorSPX(5);
        leftSlave2.setInverted(false);
        leftSlave2.setSensorPhase(false);
        leftSlave2.follow(leftMaster);
        leftSlave2.setNeutralMode(NeutralMode.Coast);

        // right rear follows front
        WPI_VictorSPX rightSlave1 = new WPI_VictorSPX(7);
        rightSlave1.setInverted(false);
        rightSlave1.setSensorPhase(true);
        rightSlave1.follow(rightSlave1);
        rightSlave1.setNeutralMode(NeutralMode.Coast);

        WPI_VictorSPX rightSlave2 = new WPI_VictorSPX(8);
        rightSlave2.setInverted(false);
        rightSlave2.setSensorPhase(true);
        rightSlave2.follow(rightSlave2);
        rightSlave2.setNeutralMode(NeutralMode.Coast);


        //
        // Configure drivetrain movement
        //

        SpeedControllerGroup leftGroup = new SpeedControllerGroup(leftMaster, leftSlave1, leftSlave2);
        SpeedControllerGroup rightGroup = new SpeedControllerGroup(rightMaster, rightSlave1, rightSlave2);

        drive = new DifferentialDrive(leftGroup, rightGroup);
        drive.setDeadband(0);


        //
        // Configure encoder related functions -- getDistance and getrate should return
        // ft and ft/s
        //

        double encoderConstant = (1 / ENCODER_PULSE_PER_REV) * WHEEL_DIAMETER * Math.PI;

        leftMaster.configSelectedFeedbackSensor(FeedbackDevice.QuadEncoder, PIDIDX, 10);
        leftEncoderPosition = () -> leftMaster.getSelectedSensorPosition(PIDIDX) /1935.0;
        leftEncoderRate = () -> leftMaster.getSelectedSensorVelocity(PIDIDX) /1935.0 * 10;

        rightMaster.configSelectedFeedbackSensor(FeedbackDevice.QuadEncoder, PIDIDX, 10);
        rightEncoderPosition = () -> rightMaster.getSelectedSensorPosition(PIDIDX) /1935.0;
        rightEncoderRate = () -> rightMaster.getSelectedSensorVelocity(PIDIDX) /1935.0 * 10;

        // Reset encoders
        leftMaster.setSelectedSensorPosition(0);
        rightMaster.setSelectedSensorPosition(0);

        // Set the update rate instead of using flush because of a ntcore bug
        // -> probably don't want to do this on a robot in competition
        NetworkTableInstance.getDefault().setUpdateRate(0.010);
    }

    @Override
    public void disabledInit() {
        System.out.println("Robot disabled");
        drive.tankDrive(0, 0);
    }

    @Override
    public void disabledPeriodic() {
    }

    @Override
    public void robotPeriodic() {
        // feedback for users, but not used by the control program
        SmartDashboard.putNumber("l_encoder_pos", leftEncoderPosition.get());
        SmartDashboard.putNumber("l_encoder_rate", leftEncoderRate.get());
        SmartDashboard.putNumber("r_encoder_pos", rightEncoderPosition.get());
        SmartDashboard.putNumber("r_encoder_rate", rightEncoderRate.get());
    }

    @Override
    public void teleopInit() {
        System.out.println("Robot in operator control mode");
    }

    @Override
    public void teleopPeriodic() {
        drive.arcadeDrive(-stick.getY(), stick.getX());

    }

    @Override
    public void autonomousInit() {

        System.out.println("Robot in autonomous mode");
    }

    /**
     * If you wish to just use your own robot program to use with the data logging
     * program, you only need to copy/paste the logic below into your code and
     * ensure it gets called periodically in autonomous mode
     *
     * Additionally, you need to set NetworkTables update rate to 10ms using the
     * setUpdateRate call.
     */
    @Override
    public void autonomousPeriodic() {


        // Retrieve values to send back before telling the motors to do something
        double now = Timer.getFPGATimestamp();

        double leftPosition = leftEncoderPosition.get();
        double leftRate = leftEncoderRate.get();

        double rightPosition = rightEncoderPosition.get();
        double rightRate = rightEncoderRate.get();

        double battery = RobotController.getBatteryVoltage();

        double leftMotorVolts = leftMaster.getMotorOutputVoltage();
        double rightMotorVolts = rightMaster.getMotorOutputVoltage();

        // Retrieve the commanded speed from NetworkTables
        double autospeed = autoSpeedEntry.getDouble(0);
        priorAutospeed = autospeed;

        // command motors to do things
        drive.tankDrive(autospeed, autospeed, false);


        // send telemetry data array back to NT
        numberArray[0] = now;
        numberArray[1] = battery;
        numberArray[2] = autospeed;
        numberArray[3] = leftMotorVolts;
        numberArray[4] = rightMotorVolts;
        numberArray[5] = leftPosition;
        numberArray[6] = rightPosition;
        numberArray[7] = leftRate;
        numberArray[8] = rightRate;

        telemetryEntry.setNumberArray(numberArray);
    }
}