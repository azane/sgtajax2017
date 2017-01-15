package lumber_jack_s;
import battlecode.common.*;

public strictfp class gardener extends RobotPlayer{
    static RobotController rc;

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
    **/
    @SuppressWarnings("unused")
    static void runGardener(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        gardener.rc = rc;
        System.out.println("I'm a gardener!");
        MapLocation startLoc = rc.getLocation();
        Team myTeam = rc.getTeam();

        Direction buildDir = randomDirection();
        if (rc.canBuildRobot(RobotType.SCOUT, buildDir)) {
        	rc.buildRobot(RobotType.SCOUT, buildDir);
        }

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {

                // Donate bullets on last round
                donateBullets();

                // Listen for home archon's location
                MapLocation myLoc = rc.getLocation();
                Direction homeDir = myLoc.directionTo(startLoc);
                
                
                //--- Gardener Move Code
                //----------------------
                // Create a tether for all gardeners, just so they stay in one place. (20 distance from their spawn location)
                Direction dir = randomDirection();
                if (myLoc.distanceTo(startLoc) > 20){
                	dir = homeDir;
                }
                tryMove(dir); 
                //--- End Move Code
                //-----------------
                
                
                //--- Gardener Build Code
                //-----------------------
                // Generate a random direction
                buildDir = randomDirection();
                float bullets = rc.getTeamBullets();
                TreeInfo[] myTrees = rc.senseNearbyTrees(-1, myTeam);

                // If we have twice the cost of a lumberjack, build a lumberjack, else try to build a tree
                if (rc.canPlantTree(buildDir) && myTrees.length < 5) {
                    rc.plantTree(buildDir);
                } else if (rc.canBuildRobot(RobotType.LUMBERJACK, buildDir) && rc.isBuildReady() && bullets > RobotType.LUMBERJACK.bulletCost * 1.5 && rc.getRobotCount() < 200) {
                    rc.buildRobot(RobotType.LUMBERJACK, buildDir);
                } else if (rc.canBuildRobot(RobotType.TANK, buildDir) && rc.isBuildReady() && rc.getRobotCount() > 200) {
                    rc.buildRobot(RobotType.TANK, buildDir);
                }
                //--- End Build Code
                //------------------

                //--- Gardener Water Code
                //-----------------------
                TreeInfo[] myLocalTrees = rc.senseNearbyTrees(RobotType.GARDENER.bodyRadius+1, myTeam);
                float min_health = GameConstants.BULLET_TREE_MAX_HEALTH;
                int tree_id = -1;
                for (TreeInfo tree : myLocalTrees){
	                if (rc.canWater(tree.getLocation()) && tree.getHealth() < min_health){
	                	min_health = tree.getHealth();
	                	tree_id = tree.getID();
	                }
                }
                if (tree_id != -1){
                	rc.water(tree_id);
                }
                //--- End Water Code
                //------------------
                
                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Gardener Exception");
                e.printStackTrace();
            }
        }
    }
}