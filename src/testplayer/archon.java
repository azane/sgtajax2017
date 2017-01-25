package testplayer;
import battlecode.common.*;

public strictfp class archon extends RobotPlayer{
    static RobotController rc;
    

    static int GARDENER_BUILD_LIMIT;

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
    **/
    @SuppressWarnings("unused")
    static void runArchon(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        archon.rc = rc;
        System.out.println("I'm an archon!");
        MapLocation enemyArchonInitialLocation = rc.getInitialArchonLocations(rc.getTeam().opponent())[0];
		rc.broadcast(10, (int)enemyArchonInitialLocation.x);
		rc.broadcast(11, (int)enemyArchonInitialLocation.y);
		
        // Code above should be removed eventually. Code below broadcasts the enemy archon locations for scouts
        int initialArchonOffset = ARCHON_SEARCH_OFFSET;
        MapLocation[] enemyArchonInitialLocations = rc.getInitialArchonLocations(rc.getTeam().opponent());
        for (MapLocation enemyArchonStart : enemyArchonInitialLocations) {
            initialArchonOffset = initialArchonOffset + 1;
            rc.broadcast(initialArchonOffset, (int)enemyArchonStart.x);
            initialArchonOffset = initialArchonOffset + 1;
            rc.broadcast(initialArchonOffset, (int)enemyArchonStart.y);
            initialArchonOffset = initialArchonOffset + 1;
        }
		GARDENER_BUILD_LIMIT = 10;

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {

                // Donate bullets on last round
                donateBullets();

                // Generate a random direction
                Direction dir = randomDirection();

                // Randomly attempt to build a gardener in this direction
                if (rc.canHireGardener(dir) && Math.random() < .10 && getNumberRobotsBuilt(RobotType.GARDENER) < GARDENER_BUILD_LIMIT) {
                    rc.hireGardener(dir);
                    gardener.addOneRobotBuilt(RobotType.GARDENER);
                }

                // Move randomly
                tryMove(randomDirection());

                // Broadcast archon's location for other robots on the team to know
                MapLocation myLocation = rc.getLocation();
                rc.broadcast(0,(int)myLocation.x);
                rc.broadcast(1,(int)myLocation.y);

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Archon Exception");
                e.printStackTrace();
            }
        }
    }
}