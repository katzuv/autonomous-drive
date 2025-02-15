package robot.subsystems.Commands;

import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.command.Command;
import org.ghrobotics.lib.debug.LiveDashboard;
import org.ghrobotics.lib.mathematics.twodim.control.TrajectoryTracker;
import org.ghrobotics.lib.mathematics.twodim.geometry.Pose2d;
import org.ghrobotics.lib.mathematics.twodim.geometry.Pose2dWithCurvature;
import org.ghrobotics.lib.mathematics.twodim.trajectory.types.TimedEntry;
import org.ghrobotics.lib.mathematics.twodim.trajectory.types.TimedTrajectory;
import org.ghrobotics.lib.mathematics.twodim.trajectory.types.TrajectorySamplePoint;
import org.ghrobotics.lib.mathematics.units.TimeUnitsKt;
import org.ghrobotics.lib.subsystems.drive.TrajectoryTrackerOutput;
import robot.subsystems.drivetrainConstants;

import java.util.List;

import static robot.Robot.drivetrain;

public class trajectoryTrackerCommand extends Command {

    private TrajectoryTracker trajectoryTrack;
    private List<Pose2d> waypoints;
    private double startingVelocity;
    private double endingVelocity;
    private TimedTrajectory<Pose2dWithCurvature> trajectory;
    private boolean finished = false;
    private boolean reversed = false;
    private double maxVelocity = drivetrainConstants.MAX_VELOCITY;
    private double maxAcceleration = drivetrainConstants.MAX_ACCEL;

    public trajectoryTrackerCommand(List<Pose2d> waypoints, double startingVelocity, double endingVelocity, boolean reversed) {
        trajectoryTrack = drivetrain.getTrajectoryTracker();
        this.waypoints = waypoints;
        this.startingVelocity = startingVelocity;
        this.endingVelocity = endingVelocity;
        this.reversed = reversed;

    }

    @Override
    protected void initialize() {
        if (waypoints != null) {
            waypoints.add(0, drivetrain.localization.getRobotPosition());
            this.trajectory = drivetrain.generateTrajectory(waypoints, startingVelocity, endingVelocity, reversed);

        }
        drivetrain.trajectoryTracker.reset(trajectory);
        LiveDashboard.INSTANCE.setFollowingPath(true);
    }

    @Override
    protected void execute() {
        TrajectoryTrackerOutput trackerOutput = drivetrain.trajectoryTracker.nextState(drivetrain.localization.getRobotPosition(), TimeUnitsKt.getSecond(Timer.getFPGATimestamp()));
        drivetrain.setOutput(trackerOutput);

        TrajectorySamplePoint<TimedEntry<Pose2dWithCurvature>> referencePoint = trajectoryTrack.getReferencePoint();

        if (referencePoint != null) {
            Pose2d referencePose = referencePoint.getState().getState().getPose();

            LiveDashboard.INSTANCE.setPathX(referencePose.getTranslation().getX().getFeet());
            LiveDashboard.INSTANCE.setPathY(referencePose.getTranslation().getY().getFeet());
            LiveDashboard.INSTANCE.setPathHeading(referencePose.getRotation().getRadian());
        }
    }

    @Override
    protected boolean isFinished() {
        return trajectoryTrack.isFinished();

    }

    @Override
    protected void end() {
        drivetrain.zeroOutputs();
        LiveDashboard.INSTANCE.setFollowingPath(false);
    }

    @Override
    protected void interrupted() {

    }
}
