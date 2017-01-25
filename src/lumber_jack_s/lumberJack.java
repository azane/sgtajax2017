package lumber_jack_s;
import battlecode.common.*;
import battlecode.world.TeamInfo;
import sjxbin.SjxMicrogradients;
import sjxbin.SjxYieldBytecode;

import java.awt.*;

public strictfp class lumberJack extends RobotPlayer{
    static RobotController rc = RobotPlayer.rc;

    public void mainMethod() throws GameActionException {
        // Get my location
        MapLocation myLoc = rc.getLocation();

        // Donate bullets on last round
        donateBullets();

        // Search for enemy archons
        searchForArchon();

        System.out.println(rc.readBroadcast(21));

        MapLocation myLocation = rc.getLocation();

        double[] gradient;

        gradient = SjxMicrogradients.instance.getMyGradient(myLocation, rc.senseNearbyRobots());

        // Add scaled gradient to myLocation coordinates.
        MapLocation gradientDestination = new MapLocation(
                myLocation.x + (float)gradient[0],
                myLocation.y + (float)gradient[1]
        );

        Direction d = myLocation.directionTo(gradientDestination);

        MapLocation closestTree = SjxMicrogradients.instance.getTreeLocation();
        if (closestTree != null) {
//            double dist = closestTree.distanceTo(myLocation);
//            boolean canDo = (dist <= GameConstants.LUMBERJACK_STRIKE_RADIUS);
//            if (canDo)
            if (rc.canShake(closestTree) && rc.senseTreeAtLocation(closestTree).getContainedBullets() > 0.)
                rc.shake(closestTree);
            else if (rc.canChop(closestTree) && !rc.hasAttacked()
                    && rc.senseTreeAtLocation(closestTree).getTeam() != myTeam)
                rc.chop(closestTree);
        }
        // Move toward the new vector.
        if (d != null)
            tryMove(d);





        //--- Lumberjack Chop/Shake Code
        //------------------------
        // Sense trees, get robots, get bullets, chop down
//        TreeInfo[] trees = rc.senseNearbyTrees(GameConstants.LUMBERJACK_STRIKE_RADIUS);
//        if (trees.length > 0 ) {
//            for (TreeInfo tree : trees) {
//                if (tree.getTeam() != myTeam){
//                    MapLocation treeLocation = tree.getLocation();
//                    // Chop down robot trees
//                    if (tree.getContainedRobot() != null && !rc.hasAttacked()) {
//                        rc.chop(treeLocation);
//                        break;
//                        // Shake bullet trees
//                    } else if (tree.getContainedBullets() > 0 && rc.canShake(treeLocation)) {
//                        rc.shake(treeLocation);
//                        break;
//                        // Chop down non friendly trees
//                    } else if (!rc.hasAttacked()) {
//                        rc.chop(treeLocation);
//                        break;
//                    }
//                }
//            }
//        }
        if (!rc.hasAttacked()) {
            RobotInfo[] robots = rc.senseNearbyRobots(GameConstants.LUMBERJACK_STRIKE_RADIUS, enemy);
            if(robots.length > 0) {
                rc.strike();
            }
        }
        //--- End Chop/Shake Code
        //------------------------


        //--- Lumberjack Move Code
        //------------------------
//        if (!rc.hasAttacked()){
//            trees = rc.senseNearbyTrees();
//
//            Direction dirToMove = randomDirection();
//            // Store closest archon's location. Trees and enemies will take priority over the archon.
//            MapLocation closestArchon = findClosestArchon();
//            if (closestArchon != null) {
//                dirToMove = myLoc.directionTo(closestArchon);
//            }
//
//            // Move toward first tree, if sensed
//            if (trees.length > 0) {
//                for (TreeInfo tree : trees){
//                    if (tree.getTeam() != myTeam){
//                        MapLocation treeLocation = tree.getLocation();
//                        dirToMove = myLoc.directionTo(treeLocation);
//                        break;
//                    }
//                }
//            } else {
//                RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);
//                if (robots.length > 0) {
//                    MapLocation robotLocation = robots[0].getLocation();
//                    dirToMove = myLoc.directionTo(robotLocation);
//                }
//            }
//            tryMove(dirToMove);
//        }
        //--- End Move Code
        //------------------------
    }

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
    **/
    @SuppressWarnings("unused")
    static void runLumberjack(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        lumberJack.rc = rc;
        
        System.out.println("I'm a lumberjack!");
        boolean archonNotFound = true;
        int enemyArchonX = rc.readBroadcast(10);
        int enemyArchonY = rc.readBroadcast(11);
        MapLocation enemyArchonLocation = new MapLocation((float)enemyArchonX, (float)enemyArchonY);


        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {

                RobotPlayer.rp.mainMethod();

            } catch (Exception e) {
                System.out.println("Lumberjack Exception");
                e.printStackTrace();
            }

            // .yield() yields the remainder of this bot's turn to army level tasks.
            SjxYieldBytecode.yield();
        }
    }
}