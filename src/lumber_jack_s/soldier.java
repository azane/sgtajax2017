package lumber_jack_s;
import battlecode.common.*;

public strictfp class soldier extends RobotPlayer{
    static RobotController rc;

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
    **/
    @SuppressWarnings("unused")
    static void runSoldier(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        soldier.rc = rc;
        System.out.println("I'm an soldier!");
        Team enemy = rc.getTeam().opponent();
        boolean archonNotFound = true;

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {

                // Donate bullets on last round
                donateBullets();

                MapLocation myLocation = rc.getLocation();

//                // If there are some...
//                if (robots.length > 0) {
//                    // And we have enough bullets, and haven't attacked yet this turn...
//                    if (rc.canFireSingleShot()) {
//                        // ...Then fire a bullet in the direction of the enemy.
//                        rc.fireSingleShot(rc.getLocation().directionTo(robots[0].location));
//                    }
//                }

                // TODO figure out if this takes up too much bytecode, if it does, take a random sample of nearby bots.
                //          over a few turns, the collective result will effectively be an average over all bots.
                // TODO softcode all the curve parameters.

                // Sense all nearby robots.
                RobotInfo[] robots = rc.senseNearbyRobots();

                // Init the gradient vector over which we'll sum. Keep track of enemy/friendly so we know
                //  if it's safe to shoot, and in which direction.
                double[] friendlyGradient = new double[] {0., 0.};
                double[] enemyGradient = new double[] {0., 0.};

                // Iterate nearby bots, calculating the gradient by which this bot should move.
                for (int i = 0; i < robots.length; ++i) {
                    if (robots[i].getTeam() == enemy) {
                        // Use the same doughnut function for all enemies, for now.
                        // Add the gradient from this bot.
                        SjxMath.elementwiseSum(
                                enemyGradient,
                                SjxMath.doughnutDerivative(myLocation, robots[i].location,
                                        2, -10,
                                        7, 2, true),
                                false
                        );
                    }
                    else {
                        // Only school with other military units.
                        switch (rc.getType()) {
                            case SOLDIER:
                            case TANK:
                                // Use a standard gaussian curve for friendlies.
                                SjxMath.elementwiseSum(
                                        friendlyGradient,
                                        SjxMath.gaussianDerivative(myLocation, robots[i].location,
                                                7, 1.5),
                                        false
                                );
                                break;
                        }
                    }
                }

                // Add the enemy archon location to the gradient. Use a wide gaussian.
                MapLocation enemyArchonLocation = getEnemyArchonLocation();
                if (enemyArchonLocation != null) {
                    SjxMath.elementwiseSum(
                            enemyGradient,
                            SjxMath.gaussianDerivative(myLocation, getEnemyArchonLocation(),
                                    50, 6),
                            false
                    );
                }


                // TODO Verify that the enemy gradient opposes the friendly gradient by at least 45 degrees.
                // TODO  Fire toward the gradient if so.


                // Move in the direction of the gradient.

                // Convert and scale.
                double[] gradient = SjxMath.elementwiseSum(enemyGradient, friendlyGradient, false);
                double s = .3;
                float x = (float) (gradient[0] * s);
                float y = (float) (gradient[1] * s);

                // Add scaled gradient to myLocation coordinates.
                MapLocation gradientDestination = new MapLocation(myLocation.x + x, myLocation.y + y);

                // Move toward the new vector.
                tryMove(myLocation.directionTo(gradientDestination));

                Clock.yield();

            } catch (Exception e) {
                System.out.println("Soldier Exception");
                e.printStackTrace();
            }
        }
    }
}