
/* Copyright (c) 2017-2018 FIRST. All Rights Reserved.                        */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

package robot;



/**
 * This class is the glue that binds the controls on the physical operator
 * interface to the commands and command groups that allow control of the robot.
 */
public class OI {
    private XboxController xbox = new XboxController(2);
    private Button A = new JoystickButton(xbox, 1);

    public OI() {
        A.whenPressed(new trajectoryTrackerCommand(Test.points, 0, 0, false));
    }


}