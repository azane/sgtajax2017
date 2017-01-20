package testplayer;
import battlecode.common.*;



public strictfp class scout extends RobotPlayer{
    static RobotController rc;
    static Team enemy;

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
    **/
    @SuppressWarnings("unused")
    static void runScout(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        scout.rc = rc;
        System.out.println("I'm an scout!");
        Team enemy = rc.getTeam().opponent();
        int startRound = rc.getRoundNum();

        MapLocation enemyArchonLocation = pickInitialArchon();
        boolean foundEnemyArchon = false;

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
            	
            	//--- Scout Search Code
            	//---------------------
                if (rc.getRoundNum() - startRound > 15) {
                    if (searchForArchon() != null) {
                        enemyArchonLocation = searchForArchon();
                        foundEnemyArchon = true;
                    } else {
                        foundEnemyArchon = false;
                    }
                }

/*                RobotInfo[] nearbyEnemyRobots = rc.senseNearbyRobots(-1, enemy);
                for (RobotInfo enemyRobot : nearbyEnemyRobots){
                	if (enemyRobot.getType() == RobotType.ARCHON){
                		foundEnemyArchon = true;
                		enemyArchonLocation = enemyRobot.getLocation();
                		rc.broadcast(10, (int)enemyArchonLocation.x);
                		rc.broadcast(11, (int)enemyArchonLocation.y);
                	}
                }*/
                //--- End Search Code
                //-------------------

/*                //--- Scout Attack Code   // Attack is expensive and not effective
                //---------------------

                // If enemy archon is found, fire at it, don't fire too early or scout will run into his own bullets
                MapLocation myLocation = rc.getLocation();
                Direction towardsEnemyArchon = myLocation.directionTo(enemyArchonLocation);

                if (foundEnemyArchon && rc.canFireSingleShot()) {
                    rc.fireSingleShot(towardsEnemyArchon);
                }
                //--- End Attack Code
                //-------------------
*/

                
                //--- Scout Move Code
                //-------------------
                MapLocation myLocation = rc.getLocation();
                Direction towardsEnemyArchon = myLocation.directionTo(enemyArchonLocation);

                // Move towards the enemy archon or perpendicular to it
                if (foundEnemyArchon){
                	tryMove(towardsEnemyArchon.rotateLeftDegrees(90));
                } else {
                	tryMove(towardsEnemyArchon);
                }
                //--- End Move Code
                //-----------------

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Soldier Exception");
                e.printStackTrace();
            }
        }
    }

    // Read different enemy archon location for each scout
    static MapLocation pickInitialArchon() throws GameActionException{
        int scoutChannel = gardener.GARDENER_BASE_OFFSET + gardener.SCOUTS_BUILT_OFFSET;
        if (rc.readBroadcast(scoutChannel) == 1) {
            int enemyArchonX = rc.readBroadcast(21);
            int enemyArchonY = rc.readBroadcast(22);
            MapLocation enemyArchonLocation = new MapLocation((float)enemyArchonX, (float)enemyArchonY);
            return enemyArchonLocation;
        } else if (rc.readBroadcast(scoutChannel) == 2) {
                if (rc.readBroadcast(24) != 0) {
                    int enemyArchonX = rc.readBroadcast(24);
                    int enemyArchonY = rc.readBroadcast(25);
                    MapLocation enemyArchonLocation = new MapLocation((float)enemyArchonX, (float)enemyArchonY);
                    return enemyArchonLocation;
                } else {
                    int enemyArchonX = rc.readBroadcast(21);
                    int enemyArchonY = rc.readBroadcast(22);
                    MapLocation enemyArchonLocation = new MapLocation((float)enemyArchonX, (float)enemyArchonY);
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
}