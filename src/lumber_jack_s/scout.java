package lumber_jack_s;
import battlecode.common.*;
import sjxbin.SjxYieldBytecode;


public strictfp class scout extends RobotPlayer{
    static RobotController rc = RobotPlayer.rc;

    int startRound = rc.getRoundNum();
    MapLocation enemyArchonLocation = pickInitialArchon2();
    boolean foundEnemyArchon = false;
    boolean atCenter = false;



    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
    **/
    @SuppressWarnings("unused")

    // Test comment
    public void mainMethod() throws GameActionException {

        // ToDo the mapCenter function can be moved out of the loop but it gave an error
        MapLocation mapCenter = findMapCenter();


        MapLocation myLocation = rc.getLocation();
        Direction dirToMove = null;
        RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);
        TreeInfo[] trees = rc.senseNearbyTrees();

        //--- Scout Collect Bullets
        //-------------------------

        // Top priority is collecting all the bullets from trees
        for (TreeInfo tree : trees) {
            if (tree.getContainedBullets() != 0) {
                if (rc.canShake(tree.getLocation())){
                    rc.shake(tree.getLocation());
                } else {
                    dirToMove = myLocation.directionTo(tree.getLocation());
                    tryMoveWithDodge(dirToMove);
                    break;
                }
            }
        }
        //-----------------------
        //--- End Collect Bullets


        //--- Scout Avoid Enemy Ranged Code
        //---------------------------------

        // Count number of enemy units that can damage and number of gardeners
        for (RobotInfo robot : robots) {
            RobotType botType = robot.getType();
            if (botType == RobotType.SOLDIER || botType == RobotType.TANK || botType == RobotType.LUMBERJACK) {
                if (myLocation.distanceTo(robot.getLocation()) < 15) {
                    tryMoveWithDodge(myLocation.directionTo(robot.getLocation()).opposite());
                    break;
                }
            }
        }
        // ToDo Add in code to flee from enemies at all costs

        //--- End Avoid Enemy Ranged Code
        //-------------------------------


        // ToDo Is this section necessary still??
        //--- Scout Search Code
        //---------------------

        // Looks for archon and broadcasts location, the broadcast queue might eliminate this?
        if (searchForArchon() != null) {
            enemyArchonLocation = searchForArchon();
            foundEnemyArchon = true;
        } else {
            foundEnemyArchon = false;
        }
        //--- End Search Code
        //-------------------


        //--- Scout Attack Code
        //---------------------

        // Attack gardeners, get close to them but not too close, don't fire from far away
        if (robots.length > 0) {
            for (RobotInfo robot : robots) {
                if (robot.getType() == RobotType.GARDENER && rc.canFireSingleShot()) {
                    Direction towardGardener = myLocation.directionTo(robot.getLocation());
                    double distanceToGardener = myLocation.distanceTo(robot.getLocation());
                    if (distanceToGardener > 2.6){
                        tryMoveWithDodge(towardGardener);
                    }
                    if (distanceToGardener < 4) {
                        rc.fireSingleShot(towardGardener);
                    }
                    break;
                }
            }
        }
        //--- End Attack Code
        //-------------------


        //--- Scout Move Code
        //-------------------

        // If the enemy archons are not dead, keep circling them
        if (rc.hasAttacked() == false && !enemyArchonsDead()) {
            Direction towardsEnemyArchon = myLocation.directionTo(enemyArchonLocation);
            if (enemyArchonLocation != null) {
                // Move towards the enemy archon or perpendicular to it
                if (foundEnemyArchon) {
                    tryMoveWithDodge(towardsEnemyArchon.rotateLeftDegrees(90));
                } else {
                    tryMoveWithDodge(towardsEnemyArchon);
                }
            }
        } else if (enemyArchonsDead()){
            Direction towardCenter = myLocation.directionTo(mapCenter);
            if (atCenter) {
                tryMoveWithDodge(towardCenter.rotateLeftDegrees(100));
            } else {
                tryMoveWithDodge(towardCenter);
            }

            if (myLocation.distanceTo(mapCenter) < 10) {
                atCenter = true;
            }
            // ToDo set atCenter to false if stuck
        }
        //--- End Move Code
        //-----------------

    }

    static void runScout(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its readCurrent status.
        scout.rc = rc;
        System.out.println("I'm an scout!");

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {

                RobotPlayer.rp.mainMethod(true);

            } catch (Exception e) {
                System.out.println("Soldier Exception");
                e.printStackTrace();
            }

            // .yield() yields the remainder of this bot's turn to army level tasks.
            SjxYieldBytecode.yield();
        }
    }

    // Read different enemy archon location for each scout
    static MapLocation pickInitialArchon() {
        try {
            int scoutChannel = gardener.GARDENER_BASE_OFFSET + gardener.SCOUTS_BUILT_OFFSET;
            if (rc.readBroadcast(scoutChannel) == 1) {
                int enemyArchonX = rc.readBroadcast(21);
                int enemyArchonY = rc.readBroadcast(22);
                MapLocation enemyArchonLocation = new MapLocation((float) enemyArchonX, (float) enemyArchonY);
                return enemyArchonLocation;
            } else if (rc.readBroadcast(scoutChannel) == 2) {
                if (rc.readBroadcast(24) != 0) {
                    int enemyArchonX = rc.readBroadcast(24);
                    int enemyArchonY = rc.readBroadcast(25);
                    MapLocation enemyArchonLocation = new MapLocation((float) enemyArchonX, (float) enemyArchonY);
                    return enemyArchonLocation;
                } else {
                    int enemyArchonX = rc.readBroadcast(21);
                    int enemyArchonY = rc.readBroadcast(22);
                    MapLocation enemyArchonLocation = new MapLocation((float) enemyArchonX, (float) enemyArchonY);
                    return enemyArchonLocation;
                }
            } else if (rc.readBroadcast(scoutChannel) == 3) {
                if (rc.readBroadcast(27) != 0) {
                    int enemyArchonX = rc.readBroadcast(27);
                    int enemyArchonY = rc.readBroadcast(28);
                    MapLocation enemyArchonLocation = new MapLocation((float) enemyArchonX, (float) enemyArchonY);
                    return enemyArchonLocation;
                } else {
                    int enemyArchonX = rc.readBroadcast(21);
                    int enemyArchonY = rc.readBroadcast(22);
                    MapLocation enemyArchonLocation = new MapLocation((float) enemyArchonX, (float) enemyArchonY);
                    return enemyArchonLocation;
                }
            } else {
                int enemyArchonX = rc.readBroadcast(21);
                int enemyArchonY = rc.readBroadcast(22);
                MapLocation enemyArchonLocation = new MapLocation((float) enemyArchonX, (float) enemyArchonY);
                return enemyArchonLocation;
            }
        }
        catch (GameActionException e) {
            throw new RuntimeException("pickInitialArchonLocation CRAAASHHHED.");
        }
    }

    // Read different enemy archon location for each scout
    static MapLocation pickInitialArchon2() {
        try {
            MapLocation[] enemyInitialArchons = rc.getInitialArchonLocations(rc.getTeam().opponent());

            int scoutChannel = gardener.GARDENER_BASE_OFFSET + gardener.SCOUTS_BUILT_OFFSET;
            int scoutNumber = rc.readBroadcast(scoutChannel);
            if (scoutNumber == 1) {
                MapLocation enemyArchon = enemyInitialArchons[0];
                return enemyArchon;
            } else if (scoutNumber == 2) {
                if (enemyInitialArchons.length >= 2) {
                    MapLocation enemyArchon = enemyInitialArchons[1];
                    return enemyArchon;
                } else {
                    MapLocation enemyArchon = enemyInitialArchons[0];
                    return enemyArchon;
                }
            } else if (scoutNumber == 3) {
                if (enemyInitialArchons.length >= 3) {
                    MapLocation enemyArchon = enemyInitialArchons[2];
                    return enemyArchon;
                } else {
                    MapLocation enemyArchon = enemyInitialArchons[0];
                    return enemyArchon;
                }
            } else {
                MapLocation enemyArchon = enemyInitialArchons[0];
                return enemyArchon;
            }
        } catch (GameActionException e) {
            throw new RuntimeException("pickInitialArchonLocation2 crashed and burned. :( ");
        }
    }

    public MapLocation findMapCenter() throws GameActionException {
        try{
            MapLocation[] initialEnemyArchons = rc.getInitialArchonLocations(rc.getTeam().opponent());
            MapLocation[] initialFriendlyArchons = rc.getInitialArchonLocations(rc.getTeam());
            float centerX = 0.0f;
            float centerY = 0.0f;
            int archonCount = 0;

            for (MapLocation enemyArchon: initialEnemyArchons) {
                centerX = centerX + enemyArchon.x;
                centerY = centerY + enemyArchon.y;
                archonCount++;
            }
            for (MapLocation friendlyArchon: initialFriendlyArchons) {
                centerX = centerX + friendlyArchon.x;
                centerY = centerY + friendlyArchon.y;
                archonCount++;
            }

            MapLocation mapCenter = new MapLocation(centerX/archonCount,centerY/archonCount);
            return mapCenter;

        } catch (Exception e) {
            System.out.println("MapCenter Exception");
            return null;
        }
    }

    public void tryMoveWithDodge(Direction dir) throws GameActionException {
        MapLocation myLocation = rc.getLocation();
        MapLocation destination = new MapLocation(myLocation.x + dir.getDeltaX(1.25f), myLocation.y + dir.getDeltaY(1.25f));
        double[] gradient = RobotPlayer.rp.dodgeIshBullets();
        MapLocation smartDestination = new MapLocation((float)(destination.x + gradient[0]), (float)(destination.y + gradient[1]));

        if (rc.canMove(smartDestination)) {
            rc.move(smartDestination);
        }
    }

}