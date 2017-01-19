package lumber_jack_s;
import battlecode.common.*;
import sjxbin.SjxMath;
import sjxbin.SjxMicrogradients;

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
        System.out.println("I'm a soldier!");
        Team enemy = rc.getTeam().opponent();
        boolean archonNotFound = true;

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {

                // Donate bullets on last round
                donateBullets();

                MapLocation myLocation = rc.getLocation();

                // TODO figure out if this takes up too much bytecode, if it does, take a random sample of nearby bots.
                //          over a few turns, the collective result will effectively be an average over all bots.
                // TODO softcode all the curve parameters. optimize them?

                // Sense all nearby robots.
                RobotInfo[] robots = rc.senseNearbyRobots();

                // Init the gradient vector over which we'll sum. Keep track of enemy/friendly so we know
                //  if it's safe to shoot, and in which direction.
                double[] friendlyGradient = new double[] {0., 0.};
                double[] enemyGradient = new double[] {0., 0.};

                // Store the closest enemy for updating.
                RobotInfo closestEnemy = null;

                // Store the number of enemies and allies for normalizing (averaging).
                int numEnemies = 0;
                int numAllies = 0;

                // Iterate nearby bots, calculating the gradient by which this bot should move.
                for (int i = 0; i < robots.length; ++i) {
                    if (robots[i].getTeam() == enemy
                            // Ignore scouts, they fly over friendlies and cause friendly fire.
                            && robots[i].type != RobotType.SCOUT) {
                        // Use the same doughnut function for all enemies, for now.
                        // Add the gradient from this bot.
                        enemyGradient = SjxMath.elementwiseSum(
                                            enemyGradient,
                                            SjxMath.doughnutDerivative(myLocation, robots[i].location,
                                            6, 10,
                                            10, 2, true),
                                            false
                                        );
                        numEnemies++;

                        // Update the closest enemy.
                        if (closestEnemy == null) {
                            closestEnemy = robots[i];
                        }
                        else if (robots[i].location.distanceSquaredTo(myLocation)
                                < closestEnemy.location.distanceSquaredTo(myLocation)) {
                            closestEnemy = robots[i];
                        }
                    }
                    else {
                        double standardDeviation = 9;
                        double scale = .6;
                        // Only school with other military units.
                        switch (rc.getType()) {
                            // TODO Use a doughnut shape for soldiers and tanks.
                            //  The effect will be to circle enemies instead of heavy clustering.
                            case SOLDIER:
                            case TANK:
                            case LUMBERJACK:
                                // Use a standard gaussian curve for friendlies.
                                friendlyGradient = SjxMath.elementwiseSum(
                                        friendlyGradient,
                                        SjxMath.gaussianDerivative(myLocation, robots[i].location,
                                                standardDeviation, scale),
                                        false
                                );
                                numAllies++;
                                break;
                        }
                    }
                }

                // Normalize so each macro element of micro sets (sets of enemies, friendlies) is weighted correctly
                //  against other macro elements (archon loc).
                if (numAllies > 0) {
                    friendlyGradient[0] /= numAllies;
                    friendlyGradient[1] /= numAllies;
                }
                if (numEnemies > 0) {
                    enemyGradient[0] /= numEnemies;
                    enemyGradient[1] /= numEnemies;
                }

                // Add the enemy archon location to the gradient. Use a wide gaussian.
                MapLocation archonLoc = findClosestArchon();
                if (archonLoc != null)
                {
                    enemyGradient = SjxMath.elementwiseSum(
                            enemyGradient,
                            SjxMath.gaussianDerivative(myLocation, findClosestArchon(),
                                    50, 10),
                            false
                    );
                }

                // Verify that the enemy gradient opposes the friendly gradient by at least 45 degrees.
                //  This *should* prevent friendly fire?
                // Note, the dot product is 0 when orthogonal (perpendicular), and 1 when parallel.
                double dp = SjxMath.dotProduct(friendlyGradient, enemyGradient);
                if (dp < .5 && rc.canFireSingleShot() && closestEnemy != null) {
                    rc.fireSingleShot(rc.getLocation().directionTo(closestEnemy.location));
                }

                // Get tree gradient.
                double[] treeGradient = SjxMicrogradients.treeGradient(myLocation, rc);

                // Move in the direction of the gradient.

                // Convert and apply universal scale.
                double[] gradient = SjxMath.elementwiseSum(enemyGradient, friendlyGradient, false);
                gradient = SjxMath.elementwiseSum(gradient, treeGradient, false);
                double s = .1;
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