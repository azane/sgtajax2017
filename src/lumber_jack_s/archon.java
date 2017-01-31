package lumber_jack_s;
import battlecode.common.*;
import sjxbin.SjxMath;
import sjxbin.SjxMicrogradients;
import sjxbin.SjxYieldBytecode;

public strictfp class archon extends RobotPlayer{
    static RobotController rc = RobotPlayer.rc;
    

    static int GARDENER_BUILD_LIMIT;
    boolean phaseOne = false;
    Direction east = Direction.getEast();
    Direction[] buildDirs = new Direction[]{east, east.rotateLeftDegrees(60), east.rotateLeftDegrees(120), 
    		east.rotateLeftDegrees(180), east.rotateLeftDegrees(240), east.rotateLeftDegrees(300)};

    public void essentialMethod() throws GameActionException {
    	super.essentialMethod();

		tryMove(randomDirection());
	}

    public void mainMethod() throws GameActionException {

    	friendlyBots.enqueueBatchTask(new RobotInfo[]
				{new RobotInfo(rc.getID(), rc.getTeam(), rc.getType(), rc.getLocation(),
						0, 0, 0)}, Clock.getBytecodesLeft());
    	
    	int initComplete = rc.readBroadcast(INIT_OFFSET);

		// Donate bullets on last round
		donateBullets();
    	
    	if (initComplete == 0){
    		runInit();
    	}
    	else {

			MapLocation myLocation = rc.getLocation();
	        
	        // Count the number of robots nearby.  Make sure there's at least one gardener
	        RobotInfo[] nearbyBots = rc.senseNearbyRobots(-1, myTeam);
	        int gardenerCount = 0;
	        int totalBotCount = 0;
		    for (RobotInfo bot : nearbyBots){
		        if (bot.type == RobotType.GARDENER){
		        	gardenerCount++;
		        }
		        totalBotCount++;
	        }

		    double[] gradient = SjxMicrogradients.instance.getMyGradient(myLocation,
					rc.senseNearbyRobots());

			double[] bdodge = dodgeIshBullets();
			gradient = SjxMath.elementwiseSum(gradient, bdodge, false);
			gradient = SjxMath.elementwiseSum(gradient, hateTheMapEdge(), false);

			// Add scaled gradient to myLocation coordinates.
			MapLocation gradientDestination = new MapLocation(
					myLocation.x + (float)gradient[0],
					myLocation.y + (float)gradient[1]
			);
	
		    if (gardenerCount < 6){
				// Attempt to build a gardener a random direction.
				Direction dir = randomDirection();
				if (rc.canHireGardener(dir) && totalBotCount < 10) {
					//getNumberRobotsBuilt(RobotType.GARDENER) < GARDENER_BUILD_LIMIT) {
					rc.hireGardener(dir);
					addOneRobotBuilt(RobotType.GARDENER);
				}
	        }
	
	        // Move
	        tryMove(myLocation.directionTo(gradientDestination));
	
	        // Broadcast archon's location for other robots on the team to know
	        rc.broadcast(0,(int)myLocation.x);
	        rc.broadcast(1,(int)myLocation.y);
    	}

		// Test display for queue reading.
//		SjxBytecodeTracker bct = new SjxBytecodeTracker();
//		bct.start(0);
//		bct.poll();
//		enemyRobots.globalPrepIter();
//		System.out.println("Enemy bot stack:" + enemyRobots.getNumElements());
//		while (enemyRobots.next() && enemyRobots.getCurrentIndex() < 20) {
//			MapLocation loc = enemyRobots.getLocation();
//			int age = enemyRobots.getInfoAge();
//
//			int color = 255 - age*20;
//			if (color < 0) color = 0;
//			rc.setIndicatorDot(loc, color, color, 0);
//
//			int itercost = bct.getCostSinceLastPoll();
//			bct.poll();
//		}
//
//		bct.end();
    }

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
    **/
    @SuppressWarnings("unused")
    static void runArchon(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its readCurrent status.
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

                RobotPlayer.rp.mainMethod(true);

            } catch (Exception e) {
                System.out.println("Archon Exception");
                e.printStackTrace();
            }

            // .yield() yields the remainder of this bot's turn to army level tasks.
            SjxYieldBytecode.yield();
        }
    }

	void runInit() throws GameActionException{
		if (!phaseOne){
			phaseOne = buildGardener();
		}		
		Direction moveDir = randomDirection();
		tryMove(moveDir);
	}
	
	boolean buildGardener() throws GameActionException{
		for (Direction dir : buildDirs) {
			if (rc.canHireGardener(dir) && getNumberRobotsBuilt(RobotType.GARDENER) < 1) {
		        rc.hireGardener(dir);
		        addOneRobotBuilt(RobotType.GARDENER);
		        return true;
			}
		}
		return false;
	}

}