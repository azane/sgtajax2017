package lumber_jack_s;
import battlecode.common.*;
import com.intellij.ui.GroupedElementsRenderer;
import sjxbin.SjxYieldBytecode;


public strictfp class scout extends RobotPlayer{
    static RobotController rc = RobotPlayer.rc;

    int startRound = rc.getRoundNum();

    MapLocation enemyArchonLocation = pickInitialArchon2();
    boolean foundEnemyArchon = false;



    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
    **/
    @SuppressWarnings("unused")

    public void mainMethod() throws GameActionException {

        MapLocation myLocation = rc.getLocation();
        Direction dirToMove = null;
        RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);
        TreeInfo[] trees = rc.senseNearbyTrees();

        //--- Scout Collect Bullets
        //-------------------------
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
        int rangedEnemyCount = 0;
        int enemyGardenerCount = 0;
        MapLocation enemyRanger = null;

        for (RobotInfo robot : robots) {
            RobotType botType = robot.getType();
            if (botType == RobotType.SOLDIER || botType == RobotType.TANK) {
                if (myLocation.distanceTo(robot.getLocation()) < 10) {
                    rangedEnemyCount++;
                    enemyRanger = robot.getLocation();
                }
            } else if (botType == RobotType.GARDENER) {
                enemyGardenerCount++;
            }
        }

        if (rangedEnemyCount == 1 && enemyGardenerCount >= 1) {
            for (RobotInfo robot : robots) {
                if (robot.getType() == RobotType.GARDENER) {
                    MapLocation gardenerLocation = robot.getLocation();
                    Direction angle = enemyRanger.directionTo(gardenerLocation);
                    float offset = 2.8f;

                    MapLocation destination = new MapLocation(angle.getDeltaX(offset), angle.getDeltaY(offset));
                    if (rc.canMove(destination)) {
                        rc.move(destination);
                    } else {
                        tryMove(myLocation.directionTo(destination));
                    }
                }
            }
        }

        //--- End Avoid Enemy Ranged Code
        //-------------------------------


        // ToDo Is this section necessary still??
        //--- Scout Search Code
        //---------------------
        if (searchForArchon() != null) {
            enemyArchonLocation = searchForArchon();
            foundEnemyArchon = true;
        } else {
            foundEnemyArchon = false;
        }
        //--- End Search Code
        //-------------------


        //--- Scout Attack Code   // Move toward gardener before trying to shoot them
        //---------------------
        if (robots.length > 0) {
            for (RobotInfo robot : robots) {
                if (robot.getType() == RobotType.GARDENER && rc.canFireSingleShot()) {
                    Direction towardGardener = myLocation.directionTo(robot.getLocation());
                    double distanceToGardener = myLocation.distanceTo(robot.getLocation());
                    if (distanceToGardener > 3){
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
        if (rc.hasAttacked() == false) {
            Direction towardsEnemyArchon = myLocation.directionTo(enemyArchonLocation);
            if (enemyArchonLocation != null)
                // Move towards the enemy archon or perpendicular to it
                if (foundEnemyArchon){
                    tryMove(towardsEnemyArchon.rotateLeftDegrees(90));
                } else {
                    tryMove(towardsEnemyArchon);
                }
        }
        //--- End Move Code
        //-----------------


/*
        //--- Scout Search Code
        //---------------------

        TreeInfo[] trees = rc.senseNearbyTrees();
        for (TreeInfo tree : trees) {
            if (tree.getContainedBullets() != 0) {
                if (rc.canShake(tree.getLocation())){
                    rc.shake(tree.getLocation());
                    break;
                } else {
                    tryMove(rc.getLocation().directionTo(tree.getLocation()));
                    return;
                }
            }
        }


        if (rc.getRoundNum() - startRound > 15) {
            if (searchForArchon() != null) {
                enemyArchonLocation = searchForArchon();
                foundEnemyArchon = true;
            } else {
                foundEnemyArchon = false;
            }
        }
        //--- End Search Code
        //-------------------


        //--- Scout Attack Code   // Move toward gardener before trying to shoot them
        //---------------------
        MapLocation myLocation = rc.getLocation();
        RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);
        if (robots.length > 0) {
            for (RobotInfo robot : robots) {
                if (robot.getType() == RobotType.GARDENER && rc.canFireSingleShot()) {
                    Direction towardGardener = myLocation.directionTo(robot.getLocation());
                    if (myLocation.distanceTo(robot.getLocation()) > 2.5){
                        tryMove(towardGardener);
                    }
                    rc.fireSingleShot(towardGardener);
                    break;
                }
            }
        }
        //--- End Attack Code
        //-------------------


        //--- Scout Move Code
        //-------------------
        if (rc.hasAttacked() == false) {
            Direction towardsEnemyArchon = myLocation.directionTo(enemyArchonLocation);
            if (towardsEnemyArchon != null)
                // Move towards the enemy archon or perpendicular to it
                if (foundEnemyArchon){
                    tryMove(towardsEnemyArchon.rotateLeftDegrees(90));
                } else {
                    tryMove(towardsEnemyArchon);
                }
        }
        //--- End Move Code
        //-----------------*/
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

                RobotPlayer.rp.mainMethod();

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

}