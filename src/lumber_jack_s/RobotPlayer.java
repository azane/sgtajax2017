package lumber_jack_s;
import battlecode.common.*;
import sjxbin.*;


public strictfp class RobotPlayer {
    public static RobotController rc;
    // Make this a 'singleton'.
    public static RobotPlayer rp;

    public static SjxPredictiveShooter predictiveShooter;

    public static SjxRobotBroadcastQueue enemyRobots;
    public static SjxRobotBroadcastQueue friendlyBots;

    static int ARCHON_SEARCH_OFFSET = 20; //Other stuff is hardcoded into this :(
    
    // Unit building offsets
    static int GARDENER_BASE_OFFSET = 900;
    static int GARDENERS_BUILT_OFFSET = 0;
    static int LUMBERJACKS_BUILT_OFFSET = 1;
    static int SOLDIERS_BUILT_OFFSET = 2;
    static int SCOUTS_BUILT_OFFSET = 3;
    static int TANKS_BUILT_OFFSET = 4;
    static int GARDENER_ARRAY_OFFSET = 920;
    static int INIT_OFFSET = 950;

    protected Team myTeam = rc.getTeam();
    protected Team enemy = myTeam.opponent();

    // The instance constructor sets the static method to the single instance.
    // If children override their constructor, they must either do this or call
    //  this base constructor, as per usual.
    public RobotPlayer() {
        try {

            rp = this;

            double[] centerSum = new double[2];
            int numArchons = 0;
            for (MapLocation loc : rc.getInitialArchonLocations(myTeam)) {
                centerSum[0] += loc.x;
                centerSum[1] += loc.y;
                numArchons++;
            }
            for (MapLocation loc : rc.getInitialArchonLocations(enemy)) {
                centerSum[0] += loc.x;
                centerSum[1] += loc.y;
                numArchons++;
            }

            if (numArchons > 0) {
                centerSum[0] /= numArchons;
                centerSum[1] /= numArchons;
            }

            mapCenter = new MapLocation((float)centerSum[0], (float)centerSum[1]);
            rc.setIndicatorDot(mapCenter, 100, 100, 100);
        }
        catch (Exception e) {
            System.out.println("Crashed on setting the instance to the static field.");
        }
    }

    private MapLocation mapCenter;
    public MapLocation getMapCenter() {
        return mapCenter;
    }

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
    **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its readCurrent status.
        RobotPlayer.rc = rc;

        try {
            RobotPlayer.predictiveShooter = new SjxPredictiveShooter(false, true);
        }
        catch (Exception e) {
            System.out.println("Predictive shooter failed to initialize!");
        }

//        try {
//            rc.broadcast(9999, 1);
//            Clock.yield();
//            if (rc.readBroadcast(9999) != 1)
//                System.out.println("The broadcast limit changed!");
//        }
//        catch (GameActionException e) {
//            System.out.println("The broadcast limit changed!");
//        }

        //SjxBroadcastQueue.test(rc);
        //SjxRobotBroadcastQueue.test(rc);

        RobotPlayer.enemyRobots = new SjxRobotBroadcastQueue(rc,
                998, 1002, 1000,
                1001);

        RobotPlayer.friendlyBots = new SjxRobotBroadcastQueue(rc,
                // size of array.
                rc.getInitialArchonLocations(rc.getTeam()).length,
                enemyRobots.getLastWriteableChannel() + 3,
                enemyRobots.getLastWriteableChannel() + 1,
                enemyRobots.getLastWriteableChannel() + 2);

        // Instantiate for the singleton.
        new SjxMicrogradients();


		// Here, we've separated the controls into a different method for each RobotType.
        // You can add the missing ones or rewrite this into your own control structure.
        switch (rc.getType()) {
            case ARCHON:
                // Note that the constructor for RobotPlayer sets RobotPlayer.rp.
                //  As long as children call the base constructor, we be fine.
                new archon();
                archon.runArchon(rc);
                break;
            case GARDENER:
                gardener gardObject = new gardener();
                gardObject.runGardener(rc);
                break;
            case SOLDIER:
                new soldier();
                soldier.runSoldier(rc);
                break;
            case TANK:
                new tank();
                tank.runTank(rc);
                break;
            case SCOUT:
                try {
                    new scout();
                }
                catch (Exception e) {
                    System.out.println("Scout DEAD!");
                }
            	scout.runScout(rc);
                break;
            case LUMBERJACK:
                new lumberJack();
                lumberJack.runLumberjack(rc);
                break;
        }
	}

	public void mainMethod() throws GameActionException {
        throw new RuntimeException("This method is not implemented in the parent class!");
    }
    public void mainMethod(boolean measure) throws GameActionException{
        SjxBytecodeTracker bct = new SjxBytecodeTracker();
        bct.start(0);
        bct.poll();
        mainMethod();
        bct.poll();
        bct.setMainMethodCost();
        bct.end();
    }
    // This method can be overriden to hold the turn-essential code like shooting or striking.
    // Turn skipping actions like yielding should call this if they can't call the full main.
    // If a unit type does not override, it just dodges bullets. Overrides should call the super.
    // Note, this and mainMethod should not be nested. Functionality and ordering are different, even
    //  though they perform similar tasks.
    public void essentialMethod() throws GameActionException {

        // Bullet dodging is essential. And we need to force a move so we don't walk on our own bullets.
        double[] bdodge = RobotPlayer.dodgeIshBullets();

        MapLocation myLoc = rc.getLocation();

        tryMove(myLoc.directionTo(new MapLocation(
                (float)(myLoc.x + bdodge[0]), (float)(myLoc.y + bdodge[1]))));
    }

    static void shootEmUp(MapLocation myLocation, RobotInfo targetBot) throws GameActionException{

        // Leave a 5% chance for them to still shoot if below value.
        // Limit to 5 trees.
        if (rc.getTeamBullets() < 100 && Math.random() < .95 && rc.getTreeCount() < 4
                // TODO check to see if we even have any farmers out that might be
                //  trying to build.
                && !(rc.senseNearbyTrees().length > 3))
            return;

        if (targetBot != null && !rc.hasAttacked()) {
            if (myLocation.distanceTo(targetBot.getLocation())
                    < ((targetBot.getType().bodyRadius * 2.5) + rc.getType().bodyRadius)
                    && rc.canFirePentadShot())
                rc.firePentadShot(myLocation.directionTo(targetBot.getLocation()));
            else if (myLocation.distanceTo(targetBot.getLocation())
                    < ((targetBot.getType().bodyRadius * 3.5) + rc.getType().bodyRadius)
                    && rc.canFireTriadShot())
                rc.fireTriadShot(myLocation.directionTo(targetBot.getLocation()));
            else if (rc.canFireSingleShot())
                rc.fireSingleShot(myLocation.directionTo(targetBot.getLocation()));
        }
    }

    static double[] dodgeIshBullets() throws GameActionException {

        int sampleSize = 10;

        BulletInfo[] bullets = rc.senseNearbyBullets(rc.getType().bodyRadius*4);

        double[] gradient = new double[2];

        for (int i = 0; i < Math.min(sampleSize, bullets.length); i++) {
            // Sample a random bullet.
            BulletInfo bullet = bullets[(int) Math.floor(Math.random() * bullets.length)];

            MapLocation dontbehere = bullet.location.add(bullet.dir, bullet.speed);

            double stdev = rc.getType().bodyRadius*1.4;

//            rc.setIndicatorLine(dontbehere,
//                    dontbehere.add(bullet.dir, (float)(stdev)),
//                    255, 255, 255);
//            rc.setIndicatorDot(dontbehere, 255, 255, 255);

            gradient = SjxMath.elementwiseSum(
                    gradient,
                    SjxMath.gaussianDerivative(rc.getLocation(), dontbehere,
                            stdev, 0.1),
                    true
            );
        }
        return gradient;
        //return new double[2];
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

        if (rc.hasMoved() || dir == null) {
            return false;
        }

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
                try {
                    rc.move(dir.rotateLeftDegrees(degreeOffset*currentCheck));
                    return true;
                }
                catch (GameActionException e) {
                    return false;
                }
            }
            // Try the offset on the right side
            if(rc.canMove(dir.rotateRightDegrees(degreeOffset*currentCheck))) {
                try {
                    rc.move(dir.rotateRightDegrees(degreeOffset * currentCheck));
                    return true;
                }
                catch (GameActionException e) {
                    return false;
                }
            }
            // No move performed, try slightly further
            currentCheck++;
        }

        // A move never happened, so return false.
        return false;
    }

    /**
     * A slightly more complicated example function, this returns true if the given bullet is on a collision
     * course with the readCurrent robot. Doesn't take into account objects between the bullet and this robot.
     *
     * @param bullet The bullet in question
     * @return True if the line of the bullet's path intersects with this robot's readCurrent position.
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
        double bulletsNeededToWin = 1000*(7.5+(12.5*current_round/3000));

        if (((total_rounds - current_round) < 7) || (rc.getTeamBullets() >= bulletsNeededToWin)){
            rc.donate(rc.getTeamBullets());
        }
    }

/*    public static Direction huntEnemyArchon() throws GameActionException{
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
    }*/


    public static MapLocation searchForArchon() throws GameActionException{

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
    	if (rc.readBroadcast(ARCHON_SEARCH_OFFSET + 1) == 0 && rc.readBroadcast(ARCHON_SEARCH_OFFSET + 4) == 0 && rc.readBroadcast(ARCHON_SEARCH_OFFSET + 7) == 0){
    	    for (RobotInfo enemyRobot : nearbyEnemies) {
    	        MapLocation enemyLocation = enemyRobot.getLocation();
    	        rc.broadcast(ARCHON_SEARCH_OFFSET + 1, (int)enemyLocation.x);
                rc.broadcast(ARCHON_SEARCH_OFFSET + 2, (int)enemyLocation.y);
                return enemyLocation;
            }
        }
        return null;
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
    
	
	static int countNearbyRobotsOfType(RobotType type) throws GameActionException{
		//Sense all nearby robots and return the number of gardeners.
        RobotInfo[] nearbyBots = rc.senseNearbyRobots();
        int gardenerCount = 0;
	    for (RobotInfo bot : nearbyBots){
	        if (bot.getType() == type){
	        	gardenerCount++;
	        }
        }
        return gardenerCount;
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


    static boolean enemyArchonsDead () throws GameActionException{
        int archon1x = rc.readBroadcast(ARCHON_SEARCH_OFFSET+1);
        int archon2x = rc.readBroadcast(ARCHON_SEARCH_OFFSET+3);
        int archon3x = rc.readBroadcast(ARCHON_SEARCH_OFFSET+6);

        if (archon1x==0 && archon2x==0 && archon3x==0) {
            return true;
        } else {
            return false;
        }
    }


}

