package lumber_jack_s;
import battlecode.common.*;

public strictfp class RobotPlayer {
    static RobotController rc;

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
                runArchon();
                break;
            case GARDENER:
                runGardener();
                break;
            case SOLDIER:
                runSoldier();
                break;
            case LUMBERJACK:
                runLumberjack();
                break;
        }
	}

    static void runArchon() throws GameActionException {
        System.out.println("I'm an archon!");

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {

                // Donate bullets on last round
                donateBullets();

                // Generate a random direction
                Direction dir = randomDirection();

                // Randomly attempt to build a gardener in this direction
                if (rc.canHireGardener(dir) && Math.random() < .10) {
                    rc.hireGardener(dir);
                }

                // Move randomly
                tryMove(randomDirection());

                // Broadcast archon's location for other robots on the team to know
                MapLocation myLocation = rc.getLocation();
                rc.broadcast(0,(int)myLocation.x);
                rc.broadcast(1,(int)myLocation.y);

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Archon Exception");
                e.printStackTrace();
            }
        }
    }

	static void runGardener() throws GameActionException {
        System.out.println("I'm a gardener!");
        int xPos = rc.readBroadcast(0);
        int yPos = rc.readBroadcast(1);
        MapLocation archonLoc = new MapLocation(xPos,yPos);
        Team myTeam = rc.getTeam();

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {

                // Donate bullets on last round
                donateBullets();

                // Listen for home archon's location
                MapLocation myLoc = rc.getLocation();
                Direction dir = myLoc.directionTo(archonLoc);
                
                // Get all robots on my team
                RobotInfo[] nearbyRobots = rc.senseNearbyRobots(-1, myTeam);
                
                
                //--- Gardener Move Code
                //----------------------
                // Find a "home" archon
                for (RobotInfo robot : nearbyRobots){
                	if (robot.getType() == RobotType.ARCHON)
                	{
                		archonLoc = robot.getLocation();
                		dir = randomDirection();
                		break;
                	}
                }
                // either move randomly or towards an archon if we are not within a sensing radius of it
                tryMove(dir); 
                //--- End Move Code
                //-----------------
                
                
                //--- Gardener Build Code
                //-----------------------
                // Generate a random direction
                Direction buildDir = randomDirection();
                float bullets = rc.getTeamBullets();

                // If we have twice the cost of a lumberjack, build a lumberjack, else try to build a tree
                if (rc.canBuildRobot(RobotType.LUMBERJACK, buildDir) && rc.isBuildReady() && bullets > RobotType.LUMBERJACK.bulletCost * 2) {
                    rc.buildRobot(RobotType.LUMBERJACK, buildDir);
                } else if (rc.canPlantTree(buildDir)) {
                    rc.plantTree(buildDir);
                }
                //--- End Build Code
                //------------------

                //--- Gardener Water Code
                //-----------------------
                TreeInfo[] myTrees = rc.senseNearbyTrees(RobotType.GARDENER.bodyRadius+1, myTeam);
                float min_health = GameConstants.BULLET_TREE_MAX_HEALTH;
                int tree_id = -1;
                for (TreeInfo tree : myTrees){
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

    static void runSoldier() throws GameActionException {
        System.out.println("I'm an soldier!");
        Team enemy = rc.getTeam().opponent();

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {

                // Donate bullets on last round
                donateBullets();

                MapLocation myLocation = rc.getLocation();

                // See if there are any nearby enemy robots
                RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);

                // If there are some...
                if (robots.length > 0) {
                    // And we have enough bullets, and haven't attacked yet this turn...
                    if (rc.canFireSingleShot()) {
                        // ...Then fire a bullet in the direction of the enemy.
                        rc.fireSingleShot(rc.getLocation().directionTo(robots[0].location));
                    }
                }

                // Move randomly
                tryMove(randomDirection());

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Soldier Exception");
                e.printStackTrace();
            }
        }
    }

    static void runLumberjack() throws GameActionException {
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
                donateBullets();

                //--- Lumberjack Chop/Shake Code
                //------------------------
                // Sense trees, get robots, get bullets, chop down
                TreeInfo[] trees = rc.senseNearbyTrees(RobotType.LUMBERJACK.bodyRadius+GameConstants.LUMBERJACK_STRIKE_RADIUS);
                if (trees.length > 0) {
                    for (TreeInfo tree : trees) {
                    	if (tree.getTeam() == myTeam){
                    		continue;
                    	}
                        MapLocation treeLocation = tree.getLocation();
                        // Chop down robot trees
                        if (tree.getContainedRobot() != null && !rc.hasAttacked()) {
                            rc.chop(treeLocation);
                            // Shake bullet trees
                        } else if (tree.getContainedBullets() > 0 && rc.canShake(treeLocation)) {
                            rc.shake(treeLocation);
                            // Chop down non friendly trees
                        } else if (!rc.hasAttacked()) {
                            rc.chop(treeLocation);
                        }
                        // Sense full radius, move toward first tree sensed
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
	                    }
	                tryMove(dirToMove);
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
        return tryMove(dir,20,3);
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

        if (total_rounds - current_round < 2){
            System.out.println("if statement works.");
            float team_bullets = rc.getTeamBullets();
                rc.donate(team_bullets);
        }
    }
}
