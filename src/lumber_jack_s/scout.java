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
                    tryMove(dirToMove);
                    break;
                }
            }
        }
        //-----------------------
        //--- End Collect Bullets


        //--- Scout Avoid Enemy Ranged Code
        //---------------------------------
        int damagingEnemyCount = 0;
        int enemyGardenerCount = 0;
        MapLocation enemyRanger = null;

        // Count number of enemy units that can damage and number of gardeners
        for (RobotInfo robot : robots) {
            RobotType botType = robot.getType();
            if (botType == RobotType.SOLDIER || botType == RobotType.TANK || botType == RobotType.LUMBERJACK) {
                if (myLocation.distanceTo(robot.getLocation()) < 10) {
                    damagingEnemyCount++;
                    enemyRanger = robot.getLocation();
                }
            } else if (botType == RobotType.GARDENER) {
                enemyGardenerCount++;
            }
        }

        // If only one enemy that can damage scout and at least one gardener, make a line with the gardener between the enemy and scout
        if (damagingEnemyCount == 1 && enemyGardenerCount >= 1) {
            for (RobotInfo robot : robots) {
                if (robot.getType() == RobotType.GARDENER) {
                    MapLocation gardenerLocation = robot.getLocation();
                    Direction angle = enemyRanger.directionTo(gardenerLocation);
                    float offset = 2.8f;

                    MapLocation destination = new MapLocation(angle.getDeltaX(offset), angle.getDeltaY(offset));
                    if (rc.canMove(destination) && !rc.hasMoved()) {
                        rc.move(destination);
                    } else {
                        tryMove(myLocation.directionTo(destination));
                    }
                }
            }
        // If at least one enemy that can hurt us and no gardeners to attack, go sit in the closest tree
        } else if (damagingEnemyCount >= 1) {
            for (TreeInfo tree : trees) {
                if (tree.getRadius() > 1){
                    if (rc.canMove(tree.getLocation()) && !rc.hasMoved()) {
                        rc.move(tree.getLocation());
                    } else {
                        tryMove(myLocation.directionTo(tree.getLocation()));
                    }
                }
            }
        }

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
                    if (distanceToGardener > 2.75){
                        tryMove(towardGardener);
                    }
                    if (distanceToGardener < 5) {
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
                    tryMove(towardsEnemyArchon.rotateLeftDegrees(90));
                } else {
                    tryMove(towardsEnemyArchon);
                }
            }
        } else if (enemyArchonsDead()){
            Direction towardCenter = myLocation.directionTo(mapCenter);
            if (atCenter) {
                tryMove(towardCenter.rotateLeftDegrees(100));
            } else {
                tryMove(towardCenter);
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

}