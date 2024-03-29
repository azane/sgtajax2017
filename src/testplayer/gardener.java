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
    static int PERSONALITY;

    int SCOUT_BUILD_LIMIT;
    int SOLDIER_BUILD_LIMIT;
    int LUMBERJACK_BUILD_LIMIT;
    int TANK_BUILD_LIMIT;
    float FRIENDLY_TREE_RADIUS;
    
    static RobotType[] ROBOT_BUILD_ORDER;
    

    Direction robotBuildDir = Direction.getEast();
    Direction[] treeBuildDirs;
    
    TreeInfo[] myTrees;
    boolean builtScout = false;
    boolean foundSpot = false;
    int buildNum;

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
    **/
    @SuppressWarnings("unused")
    void runGardener(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its readCurrent status.
        gardener.rc = rc;
        treeRoundLimit = rc.getRoundLimit() - (int)MAX_PRODUCTION * (int)GameConstants.BULLET_TREE_COST;
        
        System.out.println("I'm a gardener!");
        startLoc = rc.getLocation();
        myTeam = rc.getTeam();
        buildNum = 0;
        
        
        PERSONALITY = getPersonality();
              
        switch(PERSONALITY){
	        case 1:
	        	System.out.println("I'm a fort Gardener");
	        	fortGardenerLoop();
	        case 2:
	        	System.out.println("I'm a unit Gardener");
	        	unitGardenerLoop();
	        case 3:
	        	System.out.println("I'm a trap Gardener");
	        	trapGardenerLoop();
        }
    }

    void fortGardenerLoop() throws GameActionException{
        treeBuildDirs = new Direction[]{robotBuildDir.rotateLeftDegrees(60), robotBuildDir.rotateLeftDegrees(120), robotBuildDir.rotateLeftDegrees(240), robotBuildDir.rotateLeftDegrees(300)};
        ROBOT_BUILD_ORDER = new RobotType[]{RobotType.LUMBERJACK, RobotType.SOLDIER, RobotType.SOLDIER, 
        		RobotType.SOLDIER, RobotType.LUMBERJACK, RobotType.LUMBERJACK, RobotType.LUMBERJACK, RobotType.SOLDIER, RobotType.SOLDIER, 
        		RobotType.SOLDIER, RobotType.LUMBERJACK, RobotType.LUMBERJACK, RobotType.TANK};
        
        boolean builtSoldier = false;

	    // The code you want your robot to perform every round should be in this loop
	    while (true) {
	
	        // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
	        try {
	        	myTrees = rc.senseNearbyTrees(RobotType.GARDENER.bodyRadius+GameConstants.GENERAL_SPAWN_OFFSET+GameConstants.BULLET_TREE_RADIUS, myTeam);
	        	System.out.println("Number of trees: "+myTrees.length);
	        	if (!foundSpot){
	        		//Find empty spot
	        		System.out.println("Phase One");
	        		findEmptySpot();
	        	}
	        	else if (myTrees.length < 1){
	        		//Build the first tree
	            	System.out.println("Phase Two");
	            	buildTree();
	        	} 
	        	else if (myTrees.length == 1 && !builtScout){
	        		//Build the initial scout
	            	System.out.println("Phase Three");
	            	buildScout();
	        	} 
	        	else if (myTrees.length < 3){
	        		//Build the rest of the trees
	            	System.out.println("Phase Four");
	            	buildTree();
	        	} else if (!builtSoldier){
	        		//Build a soldier
	            	System.out.println("Phase Five");
	        		phaseFiveGardener();
	        	} else if (myTrees.length < 4) {
	        		//Build a soldier
	            	System.out.println("Phase Six");
	        		phaseSixGardener();
	        	}
	
	        	
	            // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
	            Clock.yield();
	
	        } catch (Exception e) {
	            System.out.println("Gardener Exception");
	            e.printStackTrace();
	        }
	    }
    }


    void unitGardenerLoop() throws GameActionException{
    	treeBuildDirs = new Direction[]{robotBuildDir.rotateLeftDegrees(60), robotBuildDir.rotateLeftDegrees(240)};
        ROBOT_BUILD_ORDER = new RobotType[]{RobotType.LUMBERJACK, RobotType.SOLDIER, RobotType.SOLDIER, 
        		RobotType.SOLDIER, RobotType.LUMBERJACK, RobotType.LUMBERJACK, RobotType.LUMBERJACK, RobotType.SOLDIER, RobotType.SOLDIER, 
        		RobotType.SOLDIER, RobotType.LUMBERJACK, RobotType.LUMBERJACK, RobotType.TANK};

	    // The code you want your robot to perform every round should be in this loop
	    while (true) {
	
	        // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
	        try {
	        	myTrees = rc.senseNearbyTrees(RobotType.GARDENER.bodyRadius+GameConstants.GENERAL_SPAWN_OFFSET+GameConstants.BULLET_TREE_RADIUS, myTeam);
	        	System.out.println("Number of trees: "+myTrees.length);
	        	if (!foundSpot){
	        		System.out.println("Phase One");
	        		findEmptySpot();
	        	}
	        	else if (myTrees.length < 1){
	        		//Build the first tree
	            	System.out.println("Phase Two");
	            	buildTree();
	        	} 
	        	else if (myTrees.length == 1 && !builtScout){
	        		//Build the initial scout
	            	System.out.println("Phase Three");
	            	buildScout();
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

    void trapGardenerLoop() throws GameActionException{
    	treeBuildDirs = new Direction[]{robotBuildDir.rotateLeftDegrees(60), robotBuildDir.rotateLeftDegrees(240)};
        ROBOT_BUILD_ORDER = new RobotType[]{RobotType.LUMBERJACK, RobotType.SOLDIER, RobotType.SOLDIER, 
        		RobotType.SOLDIER, RobotType.LUMBERJACK, RobotType.LUMBERJACK, RobotType.LUMBERJACK, RobotType.SOLDIER, RobotType.SOLDIER, 
        		RobotType.SOLDIER, RobotType.LUMBERJACK, RobotType.LUMBERJACK, RobotType.TANK};

	    // The code you want your robot to perform every round should be in this loop
	    while (true) {
	
	        // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
	        try {
	        	myTrees = rc.senseNearbyTrees(RobotType.GARDENER.bodyRadius+GameConstants.GENERAL_SPAWN_OFFSET, myTeam);
	        	System.out.println("Number of trees: "+myTrees.length);
	        	if (!foundSpot){
	        		System.out.println("Phase One");
	        		findEmptySpot();
	        	}
	        	else if (myTrees.length < 1){
	        		//Build the first tree
	            	System.out.println("Phase Two");
	            	buildTree();
	        	} 
	        	else if (myTrees.length == 1 && !builtScout){
	        		//Build the initial scout
	            	System.out.println("Phase Three");
	            	buildScout();
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
    
    
    void findEmptySpot() throws GameActionException{
    	//Move code first.
    	Direction moveDir = randomDirection();
    	tryMove(moveDir);

    	boolean possibleSpot = true;
    	// Have we found the right spot?
    	Direction[] allDirections = {robotBuildDir, robotBuildDir.rotateLeftDegrees(60), robotBuildDir.rotateLeftDegrees(120), 
        		robotBuildDir.rotateLeftDegrees(240), robotBuildDir.rotateLeftDegrees(300), robotBuildDir.rotateLeftDegrees(360)};
    	for (Direction buildDir : allDirections){
    		if (!rc.canPlantTree(buildDir)){
    			System.out.println("Cannot build tree.");
    			possibleSpot = false;
    		}
    	}
    	foundSpot = possibleSpot;
    }
    
    
    void buildTree() throws GameActionException{
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
    	waterFunc();
        //--- End Water Code
        //------------------
    }

    void buildScout() throws GameActionException{
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
    	waterFunc();
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
    	waterFunc();
        //--- End Water Code
        //------------------
    }

    void phaseFiveGardener() throws GameActionException{
        SCOUT_BUILD_LIMIT = 3;
        SOLDIER_BUILD_LIMIT = 40;
        LUMBERJACK_BUILD_LIMIT = 45;
        TANK_BUILD_LIMIT = 15;
        
        RobotType robotToBuild = ROBOT_BUILD_ORDER[buildNum %ROBOT_BUILD_ORDER.length];
    	
    	
        //--- Gardener Build Code
        //-----------------------
        // Generate a random direction
    	//Code to instantly build a single tree
    	if (rc.isBuildReady()) {
            if (rc.canBuildRobot(robotToBuild, robotBuildDir) && underBuildLimit(robotToBuild)){
            	rc.buildRobot(robotToBuild, robotBuildDir);
            	++buildNum;
            } else if (rc.canBuildRobot(robotToBuild, robotBuildDir.opposite()) && underBuildLimit(robotToBuild)){
            	rc.buildRobot(robotToBuild, robotBuildDir.opposite());
            	++buildNum;
            }
    	}
        //--- End Build Code
        //------------------

        //--- Gardener Water Code
        //-----------------------
    	waterFunc();
        //--- End Water Code
        //------------------
    }

    void phaseSixGardener() throws GameActionException{
        SCOUT_BUILD_LIMIT = 3;
        SOLDIER_BUILD_LIMIT = 40;
        LUMBERJACK_BUILD_LIMIT = 45;
        TANK_BUILD_LIMIT = 15;
        
        RobotType robotToBuild = ROBOT_BUILD_ORDER[buildNum %ROBOT_BUILD_ORDER.length];
    	
    	
        //--- Gardener Build Code
        //-----------------------
        // Generate a random direction
    	//Code to instantly build a single tree
    	if (rc.isBuildReady()) {
            if (rc.canBuildRobot(robotToBuild, robotBuildDir) && underBuildLimit(robotToBuild)){
            	rc.buildRobot(robotToBuild, robotBuildDir);
            	++buildNum;
            } else if (rc.canBuildRobot(robotToBuild, robotBuildDir.opposite()) && underBuildLimit(robotToBuild)){
            	rc.buildRobot(robotToBuild, robotBuildDir.opposite());
            	++buildNum;
            }
    	}
        //--- End Build Code
        //------------------

        //--- Gardener Water Code
        //-----------------------
    	waterFunc();
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
    
    void waterFunc() throws GameActionException{
        float min_health = GameConstants.BULLET_TREE_MAX_HEALTH-GameConstants.WATER_HEALTH_REGEN_RATE;
        TreeInfo tree_to_water = null;
        MapLocation tree_loc;
        for (TreeInfo tree : myTrees){
        	tree_loc = tree.getLocation();
            rc.setIndicatorDot(tree_loc, 255, 0, 0);
            if (rc.canWater(tree.getLocation()) && tree.getHealth() < min_health){
            	min_health = tree.getHealth();
            	tree_to_water = tree;
            }
        }
        if (tree_to_water != null){
	        if(rc.canWater()){
	        	rc.water(tree_to_water.getID());
	        }
        }
    }
    
	int getPersonality() throws GameActionException{
		//If we have lots of space around us return 1 for fort gardener
		RobotInfo [] nearbyRobots = rc.senseNearbyRobots();
		TreeInfo [] nearbyTrees = rc.senseNearbyTrees();
		int numUnits =  nearbyRobots.length + nearbyTrees.length;
		if(numUnits < 4){
			return 1;
		}
		else if (Math.random() < 0.90){
			//If we have lots of things around us return 2 for unit gardener
			return 2;
		} else {
			//Not sure when, but return 3 for trap gardener
			return 3;
		}
	}
}
