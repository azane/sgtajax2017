package lumber_jack_s;
import battlecode.common.*;

public strictfp class lumberJack extends RobotPlayer{
    static RobotController rc;

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
        Team myTeam = rc.getTeam();
        Team enemy = rc.getTeam().opponent();

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                // Get my location
                MapLocation myLoc = rc.getLocation();

                // Donate bullets on last round
                RobotPlayer.donateBullets();
                

                //--- Lumberjack Chop/Shake Code
                //------------------------
                // Sense trees, get robots, get bullets, chop down
                TreeInfo[] trees = rc.senseNearbyTrees(GameConstants.LUMBERJACK_STRIKE_RADIUS);
                if (trees.length > 0 ) {
                    for (TreeInfo tree : trees) {
                    	if (tree.getTeam() != myTeam){
	                        MapLocation treeLocation = tree.getLocation();
	                        // Chop down robot trees
	                        if (tree.getContainedRobot() != null && !rc.hasAttacked()) {
	                            rc.chop(treeLocation);
	                            break;
	                            // Shake bullet trees
	                        } else if (tree.getContainedBullets() > 0 && rc.canShake(treeLocation)) {
	                            rc.shake(treeLocation);
	                            break;
	                            // Chop down non friendly trees
	                        } else if (!rc.hasAttacked()) {
	                            rc.chop(treeLocation);
	                            break;
	                        }
                        }
                    }
                }
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
                if (!rc.hasAttacked()){
	                trees = rc.senseNearbyTrees();
	                
	                Direction dirToMove = randomDirection();
	                
	                // Move toward first tree, if sensed
	                if (trees.length > 0) {
	                    for (TreeInfo tree : trees){
	                    	if (tree.getTeam() != myTeam){
	                    		MapLocation treeLocation = tree.getLocation();
	                            dirToMove = myLoc.directionTo(treeLocation);
	                            break;
	                    		}
	                    	}
                    } else {
                        RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);
                        if (robots.length > 0) {
                            MapLocation robotLocation = robots[0].getLocation();
                            dirToMove = myLoc.directionTo(robotLocation);
                        }
                    }
	                RobotPlayer.tryMove(dirToMove);
                }
                //--- End Move Code
                //------------------------
                
                Clock.yield();


            } catch (Exception e) {
                System.out.println("Lumberjack Exception");
                e.printStackTrace();
            }
        }
    }
}