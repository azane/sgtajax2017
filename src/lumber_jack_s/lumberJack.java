package lumber_jack_s;
import battlecode.common.*;
import sjxbin.SjxMath;
import sjxbin.SjxMicrogradients;
import sjxbin.SjxYieldBytecode;

public strictfp class lumberJack extends RobotPlayer{
    static RobotController rc = RobotPlayer.rc;

    private TreeInfo lastTree = null;
    public void essentialMethod() throws GameActionException {
        // Call the super (dodges bullets)
        super.essentialMethod();

        if (!rc.hasAttacked()) {
            RobotInfo[] robots = rc.senseNearbyRobots(GameConstants.LUMBERJACK_STRIKE_RADIUS, enemy);
            if(robots.length > 0) {
                rc.strike();
            }
        }

        if (lastTree != null) {
            MapLocation loc = lastTree.getLocation();
            if (rc.canShake(loc) && lastTree.getContainedBullets() > 0.)
                rc.shake(loc);
            else if (rc.canChop(loc) && !rc.hasAttacked()
                    && lastTree.getTeam() != myTeam)
                rc.chop(loc);
        }
    }

    public void mainMethod() throws GameActionException {
        // Get my location
        MapLocation myLoc = rc.getLocation();

        // Donate bullets on last round
        donateBullets();

        // Search for enemy archons
        //searchForArchon();

        System.out.println(rc.readBroadcast(21));

        MapLocation myLocation = rc.getLocation();

        double[] gradient;

        gradient = SjxMicrogradients.instance.getMyGradient(myLocation, rc.senseNearbyRobots());

        // Prioritize essentials, like attacking
        if (!rc.hasAttacked()) {
            RobotInfo[] robots = rc.senseNearbyRobots(GameConstants.LUMBERJACK_STRIKE_RADIUS, enemy);
            if(robots.length > 0) {
                rc.strike();
            }
        }

        double[] bdodge = dodgeIshBullets();
        gradient = SjxMath.elementwiseSum(gradient, bdodge, false);

        // Add scaled gradient to myLocation coordinates.
        MapLocation gradientDestination = new MapLocation(
                myLocation.x + (float)gradient[0],
                myLocation.y + (float)gradient[1]
        );

        Direction d = myLocation.directionTo(gradientDestination);

        TreeInfo closestTree = SjxMicrogradients.instance.getTreeLocation();
        if (closestTree != null) {
            MapLocation loc = closestTree.getLocation();
            if (rc.canShake(loc) && closestTree.getContainedBullets() > 0.)
                rc.shake(loc);
            else if (rc.canChop(loc) && !rc.hasAttacked()
                    && closestTree.getTeam() != myTeam)
                if (rc.senseNearbyRobots(GameConstants.LUMBERJACK_STRIKE_RADIUS, myTeam).length > 0)
                    rc.chop(loc);
                else
                    rc.strike();
        }
        // Store for essentialMethod.
        lastTree = closestTree;

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
//            // Move toward globalFirst tree, if sensed
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
        // and to get information on its readCurrent status.
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

                RobotPlayer.rp.mainMethod(true);

            } catch (Exception e) {
                System.out.println("Lumberjack Exception");
            }

            // .yield() yields the remainder of this bot's turn to army level tasks.
            SjxYieldBytecode.yield();
        }
    }
}