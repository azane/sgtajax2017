package testplayer;
import battlecode.common.*;

public strictfp class gardener extends RobotPlayer{
    static RobotController rc;
    
    static int GARDENER_HOME_RANGE = 40;
    
    static float MAX_PRODUCTION = GameConstants.BULLET_TREE_BULLET_PRODUCTION_RATE * GameConstants.BULLET_TREE_MAX_HEALTH;
    
    static MapLocation startLoc;
    static Team myTeam;
    static int treeRoundLimit;
    static RobotType[] robotTypeList = {RobotType.SCOUT, RobotType.LUMBERJACK, RobotType.SOLDIER, RobotType.TANK};

    int SCOUT_BUILD_LIMIT;
    int SOLDIER_BUILD_LIMIT;
    int LUMBERJACK_BUILD_LIMIT;
    int TANK_BUILD_LIMIT;
    float FRIENDLY_TREE_RADIUS;
    
    static float lookRadius = (RobotType.GARDENER.bodyRadius+GameConstants.GENERAL_SPAWN_OFFSET+GameConstants.BULLET_TREE_RADIUS)*1.2f;

    Direction robotBuildDir = Direction.getEast();
    Direction[] treeBuildDirs = {robotBuildDir.rotateLeftDegrees(60),
    		robotBuildDir.rotateLeftDegrees(120), robotBuildDir.rotateLeftDegrees(180),
    		robotBuildDir.rotateLeftDegrees(240), robotBuildDir.rotateLeftDegrees(300)};
    TreeInfo[] myTrees;
    boolean builtScout = false;
    boolean foundSpot = false;

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
    **/
    @SuppressWarnings("unused")
    void runGardener(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        gardener.rc = rc;
        treeRoundLimit = rc.getRoundLimit() - (int)MAX_PRODUCTION * (int)GameConstants.BULLET_TREE_COST;
        
        System.out.println("I'm a gardener!");
        startLoc = rc.getLocation();
        myTeam = rc.getTeam();
        
              

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
            	myTrees = rc.senseNearbyTrees(RobotType.GARDENER.bodyRadius+GameConstants.GENERAL_SPAWN_OFFSET+GameConstants.BULLET_TREE_RADIUS, myTeam);
            	System.out.println("Number of trees: "+myTrees.length);
            	if (!foundSpot){
            		System.out.println("Phase One");
            		phaseOneGardener();
            	}
            	else if (myTrees.length < 1){
            		//Build the first tree
                	System.out.println("Phase Two");
                	phaseTwoGardener();
            	} 
            	else if (myTrees.length == 1 && !builtScout){
            		//Build the initial scout
                	System.out.println("Phase Three");
                	phaseThreeGardener();
            	} 
            	else if (myTrees.length < treeBuildDirs.length){
            		//Build the rest of the trees
                	System.out.println("Phase Four");
                	phaseFourGardener();
            	} else {
            		//Start building random units
                	System.out.println("Phase Five");
            		phaseFiveGardener();
            	}

            	
                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Gardener Exception");
                e.printStackTrace();
            }
        }
    }

    void phaseOneGardener() throws GameActionException{
    	RobotInfo[] nearbyRobots = rc.senseNearbyRobots(lookRadius);
    	TreeInfo[] nearbyTrees = rc.senseNearbyTrees(lookRadius);
    	if (nearbyRobots.length > 0 || nearbyTrees.length > 0){
    		Direction moveDir = randomDirection();
    		tryMove(moveDir);
    	} else {
    		foundSpot = true;
    	}
    }
    
    
    void phaseTwoGardener() throws GameActionException{
    	// Build a single tree
    	
        //--- Gardener Build Code
        //-----------------------
        // Generate a random direction
    	//Code to instantly build a single tree
    	if (rc.isBuildReady()) {
    		for (Direction dir : treeBuildDirs){
	            if (rc.canPlantTree(dir)){
	            	// Count the trees around us to make sure we don't have too many clogging up the area
	            	rc.plantTree(dir);
	            	break;
	            	} 
    		}
    	}
        //--- End Build Code
        //------------------

        //--- Gardener Water Code
        //-----------------------
        float min_health = GameConstants.BULLET_TREE_MAX_HEALTH;
        int tree_id = -1;
        for (TreeInfo tree : myTrees){
            if (rc.canWater(tree.getLocation()) && tree.getHealth() < min_health){
            	min_health = tree.getHealth();
            	tree_id = tree.getID();
            }
        }
        if(rc.canWater(tree_id)){
        	rc.water(tree_id);
        }
        //--- End Water Code
        //------------------
    }

    void phaseThreeGardener() throws GameActionException{
    	// Code to build a scout as soon as possible
        
        
        //--- Gardener Build Code
        //-----------------------
        // Generate a random direction

        // First try and build a tree, if you cannot, then try and build robots
        if (rc.isBuildReady() && rc.canBuildRobot(RobotType.SCOUT, robotBuildDir)) {
            	rc.buildRobot(RobotType.SCOUT, robotBuildDir);
            	builtScout = true;
            }
        //--- End Build Code
        //------------------


        //--- Gardener Water Code
        //-----------------------
        float min_health = GameConstants.BULLET_TREE_MAX_HEALTH;
        int tree_id = -1;
        for (TreeInfo tree : myTrees){
            if (rc.canWater(tree.getLocation()) && tree.getHealth() < min_health){
            	min_health = tree.getHealth();
            	tree_id = tree.getID();
            }
        }
        if(rc.canWater()){
        	rc.water(tree_id);
        }
        //--- End Water Code
        //------------------
    }

    void phaseFourGardener() throws GameActionException{
    	
    	
        //--- Gardener Build Code
        //-----------------------
        // Generate a random direction
    	//Code to instantly build a single tree
    	if (rc.isBuildReady()) {
    		for (Direction dir : treeBuildDirs){
	            if (rc.canPlantTree(dir)){
	            	// Count the trees around us to make sure we don't have too many clogging up the area
	            	rc.plantTree(dir);
	            	break;
	            	} 
    		}
    	}
        //--- End Build Code
        //------------------

        //--- Gardener Water Code
        //-----------------------
        float min_health = GameConstants.BULLET_TREE_MAX_HEALTH;
        int tree_id = -1;
        for (TreeInfo tree : myTrees){
            if (rc.canWater(tree.getLocation()) && tree.getHealth() < min_health){
            	min_health = tree.getHealth();
            	tree_id = tree.getID();
            }
        }
        if(rc.canWater(tree_id)){
        	rc.water(tree_id);
        }
        //--- End Water Code
        //------------------
    }

    void phaseFiveGardener() throws GameActionException{
        SCOUT_BUILD_LIMIT = 3;
        SOLDIER_BUILD_LIMIT = 40;
        LUMBERJACK_BUILD_LIMIT = 45;
        TANK_BUILD_LIMIT = 15;
    	
    	
        //--- Gardener Build Code
        //-----------------------
        // Generate a random direction
    	//Code to instantly build a single tree
    	if (rc.isBuildReady()) {
            if (rc.canBuildRobot(RobotType.SOLDIER, robotBuildDir) && underBuildLimit(RobotType.SOLDIER)){
            	rc.buildRobot(RobotType.SOLDIER, robotBuildDir);
            	} 
    	}
        //--- End Build Code
        //------------------

        //--- Gardener Water Code
        //-----------------------
        float min_health = GameConstants.BULLET_TREE_MAX_HEALTH;
        int tree_id = -1;
        for (TreeInfo tree : myTrees){
            if (rc.canWater(tree.getLocation()) && tree.getHealth() < min_health){
            	min_health = tree.getHealth();
            	tree_id = tree.getID();
            }
        }
        if(rc.canWater()){
        	rc.water(tree_id);
        }
        //--- End Water Code
        //------------------
    }
    
    boolean underBuildLimit(RobotType type) throws GameActionException{
    	System.out.println("In underBuildLimit");
    	return (getNumberRobotsBuilt(type) < getBuildLimit(type));
    }
    
    int getBuildLimit(RobotType type) throws GameActionException{
    	int max = 0;
//    	System.out.println("In getBuildLimit");
    	switch (type) {
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
    		case ARCHON:
    			break;
    		case GARDENER:
    			break;
    	}
    	return max;
    }
}
    
