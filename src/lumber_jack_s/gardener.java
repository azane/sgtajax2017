package lumber_jack_s;
import battlecode.common.*;

public strictfp class gardener extends RobotPlayer{
    static RobotController rc;
    
    static int GARDENER_BASE_OFFSET = 900;
    static int GARDENERS_BUILT_OFFSET = 0;
    static int LUMBERJACKS_BUILT_OFFSET = 1;
    static int SOLDIERS_BUILT_OFFSET = 2;
    static int SCOUTS_BUILT_OFFSET = 3;
    static int TANKS_BUILT_OFFSET = 4;
    
    static int GARDENER_BUILD_LIMIT = 10;
    static int SCOUT_BUILD_LIMIT = 10;
    static int SOLDIER_BUILD_LIMIT = 10;
    static int LUMBERJACK_BUILD_LIMIT = 75;
    static int TANK_BUILD_LIMIT = 75;
    
    static int GARDENER_HOME_RANGE = 50;
    
    static float MAX_PRODUCTION = GameConstants.BULLET_TREE_BULLET_PRODUCTION_RATE * GameConstants.BULLET_TREE_MAX_HEALTH;
    

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
    **/
    @SuppressWarnings("unused")
    static void runGardener(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        gardener.rc = rc;
        int treeRoundLimit = rc.getRoundLimit() - (int)MAX_PRODUCTION * (int)GameConstants.BULLET_TREE_COST;
        
        System.out.println("I'm a gardener!");
        MapLocation startLoc = rc.getLocation();
        Team myTeam = rc.getTeam();
        boolean scoutNotBuilt = true;
    	Direction buildDir = randomDirection();
        

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
            	while(scoutNotBuilt && underBuildLimit(RobotType.SCOUT)){
            		buildDir = randomDirection();
	                if (rc.canBuildRobot(RobotType.SCOUT, buildDir)) {
	                	rc.buildRobot(RobotType.SCOUT, buildDir);
	                	scoutNotBuilt = false;
                		addOneRobotBuilt(RobotType.SCOUT);
	                }
	                Clock.yield();
                }
            	

                // Donate bullets on last round
                donateBullets();

                // Listen for home archon's location
                MapLocation myLoc = rc.getLocation();
                Direction homeDir = myLoc.directionTo(startLoc);
                
                
                //--- Gardener Move Code
                //----------------------
                // Create a tether for all gardeners, just so they stay in one place. (20 distance from their spawn location)
                Direction dir = randomDirection();
                if (myLoc.distanceTo(startLoc) > GARDENER_HOME_RANGE){
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
                TreeInfo[] allTrees = rc.senseNearbyTrees(-1, myTeam);

                // First try and build a tree, if you cannot, then try and build robots
                if (rc.isBuildReady()) {
	                if (rc.canPlantTree(buildDir) && (rc.getRoundNum() < treeRoundLimit)){
	                	// Count the trees around us to make sure we don't have too many clogging up the area
	                	if (myTrees.length < 4 && allTrees.length < 6) {
		                    rc.plantTree(buildDir);
	                	}
	                	// TODO remove after testing.
	                	else if (rc.canBuildRobot(RobotType.SOLDIER, buildDir) && underBuildLimit(RobotType.SOLDIER)) {
	                		rc.buildRobot(RobotType.SOLDIER, buildDir);
	                		addOneRobotBuilt(RobotType.SOLDIER);
						}
	                	// If we have too many trees, try and build a lumberjack
	                	else if (rc.canBuildRobot(RobotType.LUMBERJACK, buildDir) && underBuildLimit(RobotType.LUMBERJACK)){
	                		System.out.println("Number of units built: "+getNumberRobotsBuilt(RobotType.LUMBERJACK)+"Build Limit: "+getBuildLimit(RobotType.LUMBERJACK));
	                		rc.buildRobot(RobotType.LUMBERJACK, buildDir);
	                		addOneRobotBuilt(RobotType.LUMBERJACK);	                		
	                	}
	                	
	                } 
	                // This is the robot building code
	                else {
						// Get a list of the robot types
	                	RobotType[] robotTypeList = {RobotType.LUMBERJACK, RobotType.TANK};
	                	for (RobotType robotType : robotTypeList) {
		            		int numRobots = getNumberRobotsBuilt(robotType);
		                	if (rc.canBuildRobot(robotType, buildDir) && underBuildLimit(robotType)) {
		                		rc.buildRobot(robotType, buildDir);
		                		addOneRobotBuilt(robotType);
		                	}
	                	}
	                }
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

    static int getBuildLimit(RobotType type) throws GameActionException{
    	int max = 0;
//    	System.out.println("In getBuildLimit");
    	switch (type) {
    		case GARDENER:
    			max = GARDENER_BUILD_LIMIT;
    			break;
    		case SCOUT:
    			max = SCOUT_BUILD_LIMIT;
    			break;
    		case SOLDIER:
    			max = SOLDIER_BUILD_LIMIT;
    			break;
    		case LUMBERJACK:
    			max = LUMBERJACK_BUILD_LIMIT;
    			break;
    		case TANK:
    			max = TANK_BUILD_LIMIT;
    			break;
    	}
    	return max;
    }
    
    static void addOneRobotBuilt(RobotType type) throws GameActionException{
//    	System.out.println("In addOneRobotBuilt");
    	int num_bots = getNumberRobotsBuilt(type);
    	num_bots++;
    	setNumberRobotsBuilt(type, num_bots);
    }
    
    static boolean underBuildLimit(RobotType type) throws GameActionException{
    	System.out.println("In underBuildLimit");
    	return (getNumberRobotsBuilt(type) < getBuildLimit(type));
    }
}