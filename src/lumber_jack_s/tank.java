package lumber_jack_s;
import battlecode.common.*;
import sjxbin.SjxYieldBytecode;

public strictfp class tank extends RobotPlayer{
    static RobotController rc = RobotPlayer.rc;

    public void mainMethod() throws GameActionException {
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

        // Go toward enemy archon
        MapLocation enemyArchon = findClosestArchon();
        if (enemyArchon == null){
            Direction dirToMove = randomDirection();
            tryMove(dirToMove);
        } else {
            Direction dirToMove = myLocation.directionTo(enemyArchon);
            tryMove(dirToMove);
        }
    }

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
    **/
    @SuppressWarnings("unused")
    static void runTank(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        tank.rc = rc;
        System.out.println("I'm an tank!");
        Team enemy = rc.getTeam().opponent();
        boolean archonNotFound = true;

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {

                RobotPlayer.rp.mainMethod();

            } catch (Exception e) {
                System.out.println("Soldier Exception");
                e.printStackTrace();
            }

            // .yield() yields the remainder of this bot's turn to army level tasks.
            SjxYieldBytecode.yield();
        }
    }
}