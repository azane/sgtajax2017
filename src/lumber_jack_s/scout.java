package lumber_jack_s;
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
        MapLocation enemyArchonLocation = rc.getInitialArchonLocations(enemy)[0];
        boolean foundEnemyArchon = false;

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
            	
            	//--- Scout Search Code
            	//---------------------
                RobotInfo[] nearbyEnemyRobots = rc.senseNearbyRobots(-1, enemy);
                for (RobotInfo enemyRobot : nearbyEnemyRobots){
                	if (enemyRobot.getType() == RobotType.ARCHON){
                		foundEnemyArchon = true;
                		enemyArchonLocation = enemyRobot.getLocation();
                		rc.broadcast(10, (int)enemyArchonLocation.x);
                		rc.broadcast(11, (int)enemyArchonLocation.y);
                	}
                }
                //--- End Search Code
                //-------------------

                
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
    
}