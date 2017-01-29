package lumber_jack_s;
import battlecode.common.*;
import sjxbin.SjxYieldBytecode;
import sjxbin.SjxMath;

import java.util.Random;

public strictfp class gardener extends RobotPlayer{static RobotController rc = RobotPlayer.rc;

static int GARDENER_HOME_RANGE = 40;

static float MAX_PRODUCTION = GameConstants.BULLET_TREE_BULLET_PRODUCTION_RATE * GameConstants.BULLET_TREE_MAX_HEALTH;

static RobotType SOLDIER = RobotType.SOLDIER;
static RobotType SCOUT = RobotType.SCOUT;
static RobotType LUMBERJACK = RobotType.LUMBERJACK;
static RobotType TANK = RobotType.TANK;
static RobotType ARCHON = RobotType.ARCHON;
static RobotType GARDENER = RobotType.GARDENER;

static MapLocation startLoc;
static Team myTeam;
static int treeRoundLimit;
static RobotType[] robotTypeList = {SCOUT, LUMBERJACK, SOLDIER, TANK};
static int PERSONALITY;

int SCOUT_BUILD_LIMIT;
int SOLDIER_BUILD_LIMIT;
int LUMBERJACK_BUILD_LIMIT;
int TANK_BUILD_LIMIT;
float FRIENDLY_TREE_RADIUS;

static RobotType[] EARLY_ROBOT_BUILD_ORDER = {TANK, LUMBERJACK, LUMBERJACK, SOLDIER, SOLDIER, LUMBERJACK};
static RobotType[] LATE_ROBOT_BUILD_ORDER = {SOLDIER, LUMBERJACK, TANK, SOLDIER, SOLDIER, TANK};


Direction east = Direction.getEast();
Direction[] treeBuildDirs = new Direction[]{east, east.rotateLeftDegrees(60), east.rotateLeftDegrees(120), 
		east.rotateLeftDegrees(180), east.rotateLeftDegrees(240), east.rotateLeftDegrees(300)}; 

TreeInfo[] myTrees;
boolean phaseOne = false;
boolean phaseTwo = false;
boolean foundSpot = false;
int buildNum = 0;

public void mainMethod() throws GameActionException {
	
	int initComplete = rc.readBroadcast(INIT_OFFSET);
	
	if (initComplete == 0){
		runInit();
	}
	else {
	    switch(PERSONALITY){
	        case 1:
	        	fortGardenerLoop();
	        	break;
	        case 2:
	        	unitGardenerLoop();
	        	break;
	    }
		// Search for enemy archons
		searchForArchon();
	}
}
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
    
    PERSONALITY = getPersonality();

    // The code you want your robot to perform every round should be in this loop
    while (true) {

        // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
        try {

            RobotPlayer.rp.mainMethod(true);

        } catch (Exception e) {
            System.out.println("Gardener Exception");
            e.printStackTrace();
        }

        try {
			// .yield() yields the remainder of this bot's turn to army level tasks.
			SjxYieldBytecode.yield();
		}
		catch (Exception e) {
        	System.out.println(e.getMessage());
		}
    }
}

void runInit() throws GameActionException{
	if (!phaseOne){
		phaseOne = buildRobot(RobotType.SCOUT);
	}
	else {
		int numTrees = rc.senseNearbyTrees().length;
		if (numTrees < 3){
			phaseTwo = buildTree();
		}
		else {
			phaseTwo = buildRobot(RobotType.LUMBERJACK);
		}
	}
	if (phaseTwo) {
		rc.broadcast(INIT_OFFSET, 1);
	}
	Direction moveDir = randomDirection();
	tryMove(moveDir);
	
}


void fortGardenerLoop() throws GameActionException{
	rc.setIndicatorDot(rc.getLocation(), 0, 255, 0);
	if (!foundSpot){
		//Find empty spot
		System.out.println("Phase One");
		findEmptySpot();
	}
	else {
		buildTree();
	}
    //--- Gardener Water Code
    //-----------------------
	waterFunc();
    //--- End Water Code
    //------------------
	
}

void unitGardenerLoop() throws GameActionException{
	//Build robots and water trees if you see them.  Currently just move randomly
	rc.setIndicatorDot(rc.getLocation(), 255, 0, 0);
	buildRobotInOrder();
	waterFunc();

	MapLocation myLocation = rc.getLocation();
	
	//Move away from trees
	TreeInfo[] trees = rc.senseNearbyTrees();
	float treeX = 0;
	float treeY = 0;
	if (trees.length != 0){
		for (TreeInfo tree : trees){
			double[] dxdy = SjxMath.gaussianDerivative(myLocation, tree.getLocation(), tree.radius*2, 1.);
			treeX = treeX + (float)dxdy[0]; //Add all x's and y's
			treeY = treeY + (float)dxdy[1]; 
		}
		treeX = treeX/trees.length;
		treeY = treeY/trees.length;
	}
	
	//Move away from other robots
	RobotInfo[] robots = rc.senseNearbyRobots();
	float robotX = 0;
	float robotY = 0;
	if (robots.length != 0){
		for (RobotInfo robot : robots){
			double[] dxdy = SjxMath.gaussianDerivative(myLocation, robot.getLocation(), robot.getRadius()*2, 5.);
			robotX = robotX + (float)dxdy[0]; //Add all x's and y's
			robotY = robotY + (float)dxdy[1]; 
		}
		robotX = robotX/trees.length;
		robotY = robotY/trees.length;
	}
	
	float newX = treeX + robotX;
	float newY = treeY + robotY;
	
	
	MapLocation plopSpot = new MapLocation(myLocation.x - newX, myLocation.y - newY);
	rc.setIndicatorDot(plopSpot, 0, 0, 0); // Set an indicator to see where the dot is going.
	
	
	// Finally, move to that new location
	Direction moveDir = myLocation.directionTo(plopSpot);
	tryMove(moveDir);
	
}

void findEmptySpot() throws GameActionException{
	MapLocation myLocation = rc.getLocation();
	
	//Move away from trees
	TreeInfo[] trees = rc.senseNearbyTrees();
	float treeX = 0;
	float treeY = 0;
	if (trees.length != 0){
		for (TreeInfo tree : trees){
			double[] dxdy = SjxMath.gaussianDerivative(myLocation, tree.getLocation(), 2., tree.radius*4);
			treeX = treeX + (float)dxdy[0]; //Add all x's and y's
			treeY = treeY + (float)dxdy[1]; 
		}
		treeX = treeX/trees.length;
		treeY = treeY/trees.length;
	}
	
	//Move away from other robots
	RobotInfo[] robots = rc.senseNearbyRobots();
	float robotX = 0;
	float robotY = 0;
	if (robots.length != 0){
		for (RobotInfo robot : robots){
			if (robot.getType() == RobotType.GARDENER){
				double[] dxdy = SjxMath.gaussianDerivative(myLocation, robot.getLocation(), 1., robot.getRadius()*2);
				robotX = robotX + (float)dxdy[0]; //Add all x's and y's
				robotY = robotY + (float)dxdy[1]; 
			}
		}
		robotX = robotX/trees.length;
		robotY = robotY/trees.length;
	}
	
	float newX = treeX + robotX;
	float newY = treeY + robotY;
	
	
	MapLocation plopSpot = new MapLocation(myLocation.x - newX, myLocation.y - newY);
	rc.setIndicatorDot(plopSpot, 0, 0, 0); // Set an indicator to see where the dot is going.
	
	
	// Finally, move to that new location
	Direction moveDir = myLocation.directionTo(plopSpot);
	tryMove(moveDir);

	int emptySpots = treeBuildDirs.length;
	foundSpot = false;
	MapLocation treeLocation;
	// Have we found the right spot?
	for (Direction buildDir : treeBuildDirs){
		treeLocation = myLocation.add(buildDir, GameConstants.GENERAL_SPAWN_OFFSET+GameConstants.BULLET_TREE_RADIUS*2+RobotType.GARDENER.bodyRadius);
		rc.setIndicatorDot(treeLocation, 255, 255, 255);
		if (rc.isLocationOccupied(treeLocation)){
			System.out.println("Cannot build tree at: "+treeLocation.toString());
			emptySpots -= 1;
		}
	}
	
	if(emptySpots > 4){
		foundSpot = true;
	}	

}


boolean buildTree() throws GameActionException{
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
            	return true;
            	} 
		}
	}
	return false;
    //--- End Build Code
    //------------------

}

boolean buildRobot(RobotType robot) throws GameActionException{
	// Code to build a scout as soon as possible

    // Try and build a scout in all directions
	for (Direction robotBuildDir : treeBuildDirs){
	    if (rc.isBuildReady() && rc.canBuildRobot(robot, robotBuildDir)) {
	        	rc.buildRobot(robot, robotBuildDir);
	        	addOneRobotBuilt(robot);
	        	return true;
	        }
	}
	return false;
}

void buildRobotInOrder() throws GameActionException{
	RobotType robotToBuild;
	if (rc.getRoundNum() < rc.getRoundLimit()/2){
		robotToBuild = EARLY_ROBOT_BUILD_ORDER[buildNum %EARLY_ROBOT_BUILD_ORDER.length];
	}
	else {
		robotToBuild = LATE_ROBOT_BUILD_ORDER[buildNum %LATE_ROBOT_BUILD_ORDER.length];
	}
    
    // If we can build a robot, look all directions and try and build a specific robot in that direction
	if (rc.isBuildReady()) {
		for (Direction robotBuildDir : treeBuildDirs){
	        if (rc.canBuildRobot(robotToBuild, robotBuildDir) && underBuildLimit(robotToBuild)){
	        	rc.buildRobot(robotToBuild, robotBuildDir);
	        	addOneRobotBuilt(robotToBuild);
	        	++buildNum;
	        	break;
	        }
		}
	}
    //--- End Build Code
    //------------------
}

boolean underBuildLimit(RobotType type) throws GameActionException{
	System.out.println("In underBuildLimit");
	return (getNumberRobotsBuilt(type) < getBuildLimit(type));
}

int getBuildLimit(RobotType type) throws GameActionException{
	int max = 0;
//	System.out.println("In getBuildLimit");
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
	myTrees = rc.senseNearbyTrees(RobotType.GARDENER.bodyRadius+GameConstants.GENERAL_SPAWN_OFFSET+GameConstants.BULLET_TREE_RADIUS, myTeam);
    float min_health = GameConstants.BULLET_TREE_MAX_HEALTH-GameConstants.WATER_HEALTH_REGEN_RATE;
    TreeInfo tree_to_water = null;
    for (TreeInfo tree : myTrees){
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
	TreeInfo [] nearbyTrees = rc.senseNearbyTrees();

	if(nearbyTrees.length < 5){
		//Fort Gardener
		return 1;
	}
	else {
		//Unit Gardener
	    SCOUT_BUILD_LIMIT = 20;
	    SOLDIER_BUILD_LIMIT = 150;
	    LUMBERJACK_BUILD_LIMIT = 200;
	    TANK_BUILD_LIMIT = 50;
		return 2;
	}
}
}
