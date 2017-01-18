package lumber_jack_s;
import battlecode.common.*;

public strictfp class RobotPlayer {
    static RobotController rc;
    static int ARCHON_SEARCH_OFFSET = 20; //Other stuff is hardcoded into this :(
    
    // Unit building offsets
    static int GARDENER_BASE_OFFSET = 900;
    static int GARDENERS_BUILT_OFFSET = 0;
    static int LUMBERJACKS_BUILT_OFFSET = 1;
    static int SOLDIERS_BUILT_OFFSET = 2;
    static int SCOUTS_BUILT_OFFSET = 3;
    static int TANKS_BUILT_OFFSET = 4;

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
    **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;

		// Here, we've separated the controls into a different method for each RobotType.
        // You can add the missing ones or rewrite this into your own control structure.
        switch (rc.getType()) {
            case ARCHON:
                archon.runArchon(rc);
                break;
            case GARDENER:
                gardener gardObject = new gardener();
                gardObject.runGardener(rc);
                break;
            case SOLDIER:
                soldier.runSoldier(rc);
                break;
            case TANK:
                tank.runTank(rc);
                break;
            case SCOUT:
            	scout.runScout(rc);
                break;
            case LUMBERJACK:
                lumberJack.runLumberjack(rc);
                break;
        }
	}
    /**
     * Returns a random Direction
     * @return a random Direction
     */
    static Direction randomDirection() {
        return new Direction((float)Math.random() * 2 * (float)Math.PI);
    }

    /**
     * Attempts to move in a given direction, while avoiding small obstacles directly in the path.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir) throws GameActionException {
        return tryMove(dir,25,3);
    }

    /**
     * Attempts to move in a given direction, while avoiding small obstacles direction in the path.
     *
     * @param dir The intended direction of movement
     * @param degreeOffset Spacing between checked directions (degrees)
     * @param checksPerSide Number of extra directions checked on each side, if intended direction was unavailable
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir, float degreeOffset, int checksPerSide) throws GameActionException {

        // First, try intended direction
        if (rc.canMove(dir)) {
            rc.move(dir);
            return true;
        }

        // Now try a bunch of similar angles
        boolean moved = false;
        int currentCheck = 1;

        while(currentCheck<=checksPerSide) {
            // Try the offset of the left side
            if(rc.canMove(dir.rotateLeftDegrees(degreeOffset*currentCheck))) {
                rc.move(dir.rotateLeftDegrees(degreeOffset*currentCheck));
                return true;
            }
            // Try the offset on the right side
            if(rc.canMove(dir.rotateRightDegrees(degreeOffset*currentCheck))) {
                rc.move(dir.rotateRightDegrees(degreeOffset*currentCheck));
                return true;
            }
            // No move performed, try slightly further
            currentCheck++;
        }

        // A move never happened, so return false.
        return false;
    }

    /**
     * A slightly more complicated example function, this returns true if the given bullet is on a collision
     * course with the current robot. Doesn't take into account objects between the bullet and this robot.
     *
     * @param bullet The bullet in question
     * @return True if the line of the bullet's path intersects with this robot's current position.
     */
    static boolean willCollideWithMe(BulletInfo bullet) {
        MapLocation myLocation = rc.getLocation();

        // Get relevant bullet information
        Direction propagationDirection = bullet.dir;
        MapLocation bulletLocation = bullet.location;

        // Calculate bullet relations to this robot
        Direction directionToRobot = bulletLocation.directionTo(myLocation);
        float distToRobot = bulletLocation.distanceTo(myLocation);
        float theta = propagationDirection.radiansBetween(directionToRobot);

        // If theta > 90 degrees, then the bullet is traveling away from us and we can break early
        if (Math.abs(theta) > Math.PI/2) {
            return false;
        }

        // distToRobot is our hypotenuse, theta is our angle, and we want to know this length of the opposite leg.
        // This is the distance of a line that goes from myLocation and intersects perpendicularly with propagationDirection.
        // This corresponds to the smallest radius circle centered at our location that would intersect with the
        // line that is the path of the bullet.
        float perpendicularDist = (float)Math.abs(distToRobot * Math.sin(theta)); // soh cah toa :)

        return (perpendicularDist <= rc.getType().bodyRadius);
    }

    public static void donateBullets() throws GameActionException{
        // Donate bullets on last round
        // This needs spread to all robots eventually
        int total_rounds = rc.getRoundLimit();
        int current_round = rc.getRoundNum();

        if (((total_rounds - current_round) < 2) || (rc.getTeamBullets() >= 10000)){
            float team_bullets = rc.getTeamBullets();
                rc.donate(team_bullets);
        }
    }

    public static Direction huntEnemyArchon() throws GameActionException{
        MapLocation myLoc = rc.getLocation();

//    	// First, look if there is an archon in range --- this is useless
//    	RobotInfo[] nearbyEnemies = rc.senseNearbyRobots();
//    	for (RobotInfo enemyRobot : nearbyEnemies){
//        	if (enemyRobot.getType() == RobotType.ARCHON){
//            	MapLocation enemyArchonLocation = enemyRobot.getLocation();
//            	return myLoc.directionTo(enemyArchonLocation);
//        	}
//    	}

        // If enemy archon is being broadcasted, go to that location -- 10 == x_value, 11 == y_value
        int enemyArchonX = rc.readBroadcast(10);
        int enemyArchonY = rc.readBroadcast(11);
        if (enemyArchonX != 0 && enemyArchonY != 0) {
            MapLocation enemyArchonLocation = new MapLocation((float)enemyArchonX, (float)enemyArchonY);
            return myLoc.directionTo(enemyArchonLocation);
        }

        // return a random direction if we don't know where the archon is -- this point should never be reached
        return randomDirection();
    }
    
    public static boolean foundEnemyArchon() throws GameActionException{
        // Return true if archon is sensed or robot is within 20 units of the archon location, else return false

    	// First, look if there is an archon in range
    	RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
    	for (RobotInfo enemyRobot : nearbyEnemies){
        	if (enemyRobot.getType() == RobotType.ARCHON){
        		return true;
        	}
    	}
    	    	
    	// If enemy archon is being broadcasted, go to that location -- 10 == x_value, 11 == y_value
        int enemyArchonX = rc.readBroadcast(10);
        int enemyArchonY = rc.readBroadcast(11);
        if (enemyArchonX != 0 && enemyArchonY != 0) {
        	MapLocation enemyArchonLocation = new MapLocation((float)enemyArchonX, (float)enemyArchonY);
        	if (enemyArchonLocation.distanceTo(rc.getLocation()) < 20){
        		return true;
        	}
        }
        return false;
    }


    public static MapLocation searchForArchon() throws GameActionException{
//        int bytesUsed = Clock.getBytecodeNum();
//        System.out.println(bytesUsed);

    	// If an archon is in range, broadcast archon's ID and coordinates
    	RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
    	for (RobotInfo enemyRobot : nearbyEnemies){
        	if (enemyRobot.getType() == RobotType.ARCHON){
            	MapLocation enemyArchonLocation = enemyRobot.getLocation();
            	int enemyArchonID = enemyRobot.getID();
            	int archonSearch = ARCHON_SEARCH_OFFSET;
            	while (rc.readBroadcast(archonSearch) != 0 && rc.readBroadcast(archonSearch) != enemyArchonID) {
            	    archonSearch = archonSearch + 3;
                }
                // If archon is about to die (<= 10 health) coordinates are called as zero to mark archon as dead
                if (enemyRobot.getHealth() <= 10) {
                    rc.broadcast(archonSearch, enemyArchonID);
                    rc.broadcast(archonSearch + 1, 0);
                    rc.broadcast(archonSearch + 2, 0);
                    return enemyArchonLocation;
                } else {
                    rc.broadcast(archonSearch, enemyArchonID);
                    rc.broadcast(archonSearch + 1, (int)enemyArchonLocation.x);
                    rc.broadcast(archonSearch + 2, (int)enemyArchonLocation.y);
                    return enemyArchonLocation;
                }

        	}
    	}
        return null;
//        int bytesUsedNew = Clock.getBytecodeNum();
//        System.out.println(bytesUsedNew);
    }
    public static MapLocation findClosestArchon() throws GameActionException{

        // Need to add something to tell it where to go when all archons are dead


        // Read enemy archon's coordinates, return closest
        // Needs updated to account for dead archons
        MapLocation myLoc = rc.getLocation();
        int archonSearchX = ARCHON_SEARCH_OFFSET + 1;
        int archonSearchY = ARCHON_SEARCH_OFFSET + 2;

        while (rc.readBroadcast(archonSearchX) == 0){
            archonSearchX = archonSearchX + 3;
            archonSearchY = archonSearchY + 3;
        }
        MapLocation closestArchon = new MapLocation((float)rc.readBroadcast(archonSearchX), (float)rc.readBroadcast(archonSearchY));

        while(archonSearchX < ARCHON_SEARCH_OFFSET + 8) {
            float distToArchon = myLoc.distanceTo(closestArchon);
            archonSearchX = archonSearchX + 3;
            archonSearchY = archonSearchY + 3;
            MapLocation nextArchon = new MapLocation((float)rc.readBroadcast(archonSearchX), (float)rc.readBroadcast(archonSearchY));
            if (rc.readBroadcast(archonSearchX) != 0 && myLoc.distanceTo(nextArchon) < distToArchon) {
                closestArchon = nextArchon;
            }
        }
        if (closestArchon.x != 0) {
            return closestArchon;
        } else {
            return null;
        }

    }
    static int getNumberRobotsBuilt(RobotType type) throws GameActionException{
    	int channel = 0;
//    	System.out.println("In getNumberRobotsBuilt for type: "+type.toString());
    	switch (type) {
    		case GARDENER:
    			channel = GARDENER_BASE_OFFSET + GARDENERS_BUILT_OFFSET;
    			break;
    		case SCOUT:
    			channel = GARDENER_BASE_OFFSET + SCOUTS_BUILT_OFFSET;
    			break;
    		case SOLDIER:
    			channel = GARDENER_BASE_OFFSET + SOLDIERS_BUILT_OFFSET;
    			break;
    		case LUMBERJACK:
    			channel = GARDENER_BASE_OFFSET + LUMBERJACKS_BUILT_OFFSET;
    			break;
    		case TANK:
    			channel = GARDENER_BASE_OFFSET + TANKS_BUILT_OFFSET;
    			break;
    		case ARCHON:
    			return rc.getInitialArchonLocations(RobotPlayer.rc.getTeam()).length;
    	}
    	int numBuilt = RobotPlayer.rc.readBroadcast(channel);
//    	System.out.println("Number of "+type+" built: "+numBuilt);
    	return numBuilt;
    }

    static void setNumberRobotsBuilt(RobotType type, int value) throws GameActionException{
    	int channel = 0;
//    	System.out.println("In setNumberRobotsBuilt");
    	switch (type) {
    		case GARDENER:
    			channel = GARDENER_BASE_OFFSET + GARDENERS_BUILT_OFFSET;
    			break;
    		case SCOUT:
    			channel = GARDENER_BASE_OFFSET + SCOUTS_BUILT_OFFSET;
    			break;
    		case SOLDIER:
    			channel = GARDENER_BASE_OFFSET + SOLDIERS_BUILT_OFFSET;
    			break;
    		case LUMBERJACK:
    			channel = GARDENER_BASE_OFFSET + LUMBERJACKS_BUILT_OFFSET;
    			break;
    		case TANK:
    			channel = GARDENER_BASE_OFFSET + TANKS_BUILT_OFFSET;
    			break;
    	}
    	RobotPlayer.rc.broadcast(channel, value);
    }

    static void addOneRobotBuilt(RobotType type) throws GameActionException{
//    	System.out.println("In addOneRobotBuilt");
    	int num_bots = getNumberRobotsBuilt(type);
    	num_bots++;
    	setNumberRobotsBuilt(type, num_bots);
    }
}

