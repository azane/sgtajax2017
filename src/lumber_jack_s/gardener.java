package lumber_jack_s;
import battlecode.common.*;
import sjxbin.SjxYieldBytecode;

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
    Direction buildDir;
    float FRIENDLY_TREE_RADIUS;
    

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
            	int roundNum = rc.getRoundNum();
            	if (roundNum < rc.getRoundLimit()*.10){
            		//Run early game code
            		phaseOneGardener(rc);
            	} 
            	else if (roundNum < rc.getRoundLimit()*.25){
            		//Run mid-early game code
            		phaseTwoGardener(rc);
            	} 
            	else if (roundNum < rc.getRoundLimit()*.50){
            		//Run mid-game code
            		phaseThreeGardener(rc);
            	} 
            	else if (roundNum < rc.getRoundLimit()*.75){
            		//Run mid-late game code
            		phaseFourGardener(rc);
            	} 
            	else {
            		//Run late-game code
            		phaseFiveGardener(rc);
            	}

                // .yield() yields the remainder of this bot's turn to army level tasks.
                SjxYieldBytecode.yield();

            } catch (Exception e) {
                System.out.println("Gardener Exception");
                e.printStackTrace();
            }
        }
    }

    void phaseOneGardener(RobotController rc) throws GameActionException{
    	// Build limits for each phase
        SCOUT_BUILD_LIMIT = 2;
        SOLDIER_BUILD_LIMIT = 0;
        LUMBERJACK_BUILD_LIMIT = 10;
        TANK_BUILD_LIMIT = 0;
        FRIENDLY_TREE_RADIUS = RobotType.GARDENER.sensorRadius/2;

        // Listen for home archon's location
        MapLocation myLoc = rc.getLocation();
        Direction homeDir = myLoc.directionTo(startLoc);
        boolean didBuildRobot = false;
        
        
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
        TreeInfo[] myTrees = rc.senseNearbyTrees(FRIENDLY_TREE_RADIUS, myTeam);
        TreeInfo[] allTrees = rc.senseNearbyTrees(-1, myTeam);

        // First try and build a tree, if you cannot, then try and build robots
        if (rc.isBuildReady()) {
            if (rc.canPlantTree(buildDir) && (rc.getRoundNum() < treeRoundLimit)){
            	// Count the trees around us to make sure we don't have too many clogging up the area
            	if (myTrees.length < 2 && allTrees.length < 5) {
                    rc.plantTree(buildDir);
            	} 
            	// If we have too many trees, try and build a lumberjack
            	else if (rc.canBuildRobot(RobotType.LUMBERJACK, buildDir) && underBuildLimit(RobotType.LUMBERJACK)){
            		rc.buildRobot(RobotType.LUMBERJACK, buildDir);
            		addOneRobotBuilt(RobotType.LUMBERJACK);	                		
            	}
            } 
            // This is the robot building code
            else {
            	didBuildRobot = tryBuildRobot();
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
    }

    void phaseTwoGardener(RobotController rc) throws GameActionException{
    	// Build limits for each phase
        SCOUT_BUILD_LIMIT = 3;
        SOLDIER_BUILD_LIMIT = 10;
        LUMBERJACK_BUILD_LIMIT = 10;
        TANK_BUILD_LIMIT = 0;
        FRIENDLY_TREE_RADIUS = RobotType.GARDENER.sensorRadius/2;

        // Listen for home location
        MapLocation myLoc = rc.getLocation();
        Direction homeDir = myLoc.directionTo(startLoc);
        boolean didBuildRobot = false;
        
        
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
        TreeInfo[] myTrees = rc.senseNearbyTrees(FRIENDLY_TREE_RADIUS, myTeam);
        TreeInfo[] allTrees = rc.senseNearbyTrees(-1, myTeam);

        // First try and build a tree, if you cannot, then try and build robots
        if (rc.isBuildReady()) {
            if (rc.canPlantTree(buildDir) && (rc.getRoundNum() < treeRoundLimit)){
            	// Count the trees around us to make sure we don't have too many clogging up the area
            	if (myTrees.length < 3 && allTrees.length < 5) {
                    rc.plantTree(buildDir);
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
            	didBuildRobot = tryBuildRobot();
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
    }
    

    void phaseThreeGardener(RobotController rc) throws GameActionException{
    	// Build limits for each phase
        SCOUT_BUILD_LIMIT = 3;
        SOLDIER_BUILD_LIMIT = 15;
        LUMBERJACK_BUILD_LIMIT = 40;
        TANK_BUILD_LIMIT = 1;
        FRIENDLY_TREE_RADIUS = RobotType.GARDENER.sensorRadius/2;
        
        // Donate bullets on last round or enough bullets to win outright
        donateBullets();

        // Listen for home location
        MapLocation myLoc = rc.getLocation();
        Direction homeDir = myLoc.directionTo(startLoc);
        boolean didBuildRobot = false;
        
        
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

        // Don't build any more trees, just upkeep and build units
        if (rc.isBuildReady()) {
        	// Loop through a list of the robot types
        	didBuildRobot = tryBuildRobot();
        }
        // If we didn't build a unit, this probably means we don't have enough bullets, so build a tree.
        if (!didBuildRobot) {
            TreeInfo[] myTrees = rc.senseNearbyTrees(FRIENDLY_TREE_RADIUS, myTeam);
            TreeInfo[] allTrees = rc.senseNearbyTrees(-1, myTeam);
            if (rc.canPlantTree(buildDir) && (rc.getRoundNum() < treeRoundLimit)){
            	// Count the trees around us to make sure we don't have too many clogging up the area
            	if (myTrees.length < 3 && allTrees.length < 5) {
                    rc.plantTree(buildDir);
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
    }

    void phaseFourGardener(RobotController rc) throws GameActionException{
    	// Build limits for each phase
        SCOUT_BUILD_LIMIT = 3;
        SOLDIER_BUILD_LIMIT = 40;
        LUMBERJACK_BUILD_LIMIT = 45;
        TANK_BUILD_LIMIT = 15;
        FRIENDLY_TREE_RADIUS = RobotType.GARDENER.sensorRadius/2;
        
        // Donate bullets on last round or enough bullets to win outright
        donateBullets();
        
        // Listen for home location
        MapLocation myLoc = rc.getLocation();
        Direction homeDir = myLoc.directionTo(startLoc);
        boolean didBuildRobot = false;
        
        
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

        // Don't build any more trees, just upkeep and build units
        if (rc.isBuildReady()) {
        	didBuildRobot = tryBuildRobot();
        }
        // If we didn't build a unit, this probably means we don't have enough bullets, so build a tree.
        if (!didBuildRobot) {
            TreeInfo[] myTrees = rc.senseNearbyTrees(FRIENDLY_TREE_RADIUS, myTeam);
            TreeInfo[] allTrees = rc.senseNearbyTrees(-1, myTeam);
            if (rc.canPlantTree(buildDir) && (rc.getRoundNum() < treeRoundLimit)){
            	// Count the trees around us to make sure we don't have too many clogging up the area
            	if (myTrees.length < 3 && allTrees.length < 5) {
                    rc.plantTree(buildDir);
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
    }

    void phaseFiveGardener(RobotController rc) throws GameActionException{
    	// Build limits for each phase
        SCOUT_BUILD_LIMIT = 3;
        SOLDIER_BUILD_LIMIT = 45;
        LUMBERJACK_BUILD_LIMIT = 45;
        TANK_BUILD_LIMIT = 30;
        FRIENDLY_TREE_RADIUS = RobotType.GARDENER.sensorRadius;
        
        // Donate bullets on last round or enough bullets to win outright
        donateBullets();
        
        // Listen for home location
        MapLocation myLoc = rc.getLocation();
        Direction homeDir = myLoc.directionTo(startLoc);
        boolean didBuildRobot = false;
        
        
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
        TreeInfo[] myTrees = rc.senseNearbyTrees(FRIENDLY_TREE_RADIUS, myTeam);
        TreeInfo[] allTrees = rc.senseNearbyTrees(-1, myTeam);

        // First try and build a tree, if you cannot, then try and build robots
        if (rc.isBuildReady()) {
            if (rc.canPlantTree(buildDir) && (rc.getRoundNum() < treeRoundLimit)){
            	// Count the trees around us to make sure we don't have too many clogging up the area
            	if (myTrees.length < 4 && allTrees.length < 6) {
                    rc.plantTree(buildDir);
            	}            	
            } 
            // This is the robot building code
            else {
            	didBuildRobot = tryBuildRobot();
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
    }
      
    boolean tryBuildRobot() throws GameActionException{
    	buildDir = randomDirection();
    	float degreeOffset = 20;
    	int checksPerSide = 3;
    	
    	
    	for (RobotType robotType : robotTypeList) {
        	if (underBuildLimit(robotType) && rc.getTeamBullets() > robotType.bulletCost) {

		        // First, try intended direction
		        if (rc.canBuildRobot(robotType, buildDir)) {
		        	rc.buildRobot(robotType, buildDir);
            		addOneRobotBuilt(robotType);
		            return true;
		        }

		        // Now try a bunch of similar angles
		        int currentCheck = 1;

		        while(currentCheck<=checksPerSide) {
		            // Try the offset of the left side
		            if(rc.canMove(buildDir.rotateLeftDegrees(degreeOffset*currentCheck))) {
		            	rc.buildRobot(robotType, buildDir);
		            	addOneRobotBuilt(robotType);
		                return true;
		            }
		            // Try the offset on the right side
		            if(rc.canMove(buildDir.rotateRightDegrees(degreeOffset*currentCheck))) {
		            	rc.buildRobot(robotType, buildDir);
	            		addOneRobotBuilt(robotType);
		                return true;
		            }
		            // No build performed, try slightly further
		            currentCheck++;
        		}
        	}
    	}
    	return false;
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
    
