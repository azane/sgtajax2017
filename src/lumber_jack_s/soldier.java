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

                // See if there are any nearby enemy robots
//                RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);
//
//                // If there are some...
//                if (robots.length > 0) {
//                    // And we have enough bullets, and haven't attacked yet this turn...
//                    if (rc.canFireSingleShot()) {
//                        // ...Then fire a bullet in the direction of the enemy.
//                        rc.fireSingleShot(rc.getLocation().directionTo(robots[0].location));
//                    }
//                }
//
//                // If enemy archon is being broadcasted, go to that location -- 10 == x_value, 11 == y_value
//            	Direction dirToMove = randomDirection();
//                if (!RobotPlayer.foundEnemyArchon()) {
//                	dirToMove = RobotPlayer.huntEnemyArchon();
//                }
//                tryMove(dirToMove);
//
//                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
//                Clock.yield();

                // TODO figure out if this takes up too much bytecode, if it does, take a random sample of nearby bots.
                //          over a few turns, the collective result will effectively be an average over all bots.

                // Sense all nearby robots.
                RobotInfo[] robots = rc.senseNearbyRobots();

                // Init the gradient vector over which we'll sum.
                double[] gradient = new double[] {0., 0.};

                // Iterate nearby bots, calculating the gradient by which this bot should move.
                for (int i = 0; i < robots.length; ++i) {
                    if (robots[i].getTeam() == enemy) {
                        // Use the same doughnut function for all enemies, for now.
                        // Add the gradient from this bot.
                        SjxMath.elementwiseSum(
                                gradient,
                                SjxMath.doughnutDerivative(myLocation, robots[i].location,
                                        4, -10,
                                        10, 1, true),
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
                                        gradient,
                                        SjxMath.gaussianDerivative(myLocation, robots[i].location,
                                                5, 1),
                                        false
                                );
                                break;
                        }
                    }
                }

                // TODO Add the enemy archon location to the gradient.

                // TODO Move in the direction of the gradient.


                Clock.yield();

            } catch (Exception e) {
                System.out.println("Soldier Exception");
                e.printStackTrace();
            }
        }
    }
}