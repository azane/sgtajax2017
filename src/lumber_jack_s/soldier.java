package lumber_jack_s;
import battlecode.common.*;
import sjxbin.SjxMath;
import sjxbin.SjxMicrogradients;
import sjxbin.SjxYieldBytecode;

public strictfp class soldier extends RobotPlayer{
    static RobotController rc = RobotPlayer.rc;

    public void mainMethod() throws GameActionException {
        // Donate bullets on last round
        donateBullets();

        MapLocation myLocation = rc.getLocation();

        // Sense all nearby robots.
        //RobotInfo[] robots = rc.senseNearbyRobots();

        double[] gradient = new double[2];

        // Store the closest enemy for updating.
        //RobotInfo closestEnemy = null;

        gradient = SjxMicrogradients.instance.getMyGradient(myLocation, rc.senseNearbyRobots());

        // Add scaled gradient to myLocation coordinates.
        MapLocation gradientDestination = new MapLocation(
                myLocation.x + (float)gradient[0],
                myLocation.y + (float)gradient[1]
        );

        Direction d = myLocation.directionTo(gradientDestination);
        // Move toward the new vector.
        if (d != null)
            tryMove(d);

        RobotInfo targetBot = SjxMicrogradients.instance.getShotLocation();

        if (targetBot != null && !rc.hasAttacked()) {
            if (myLocation.distanceTo(targetBot.getLocation()) < targetBot.getType().bodyRadius * 2.5 && rc.canFirePentadShot())
                rc.firePentadShot(myLocation.directionTo(targetBot.getLocation()));
            else if (myLocation.distanceTo(targetBot.getLocation()) < targetBot.getType().bodyRadius * 4.
                    && rc.canFireTriadShot())
                rc.fireTriadShot(myLocation.directionTo(targetBot.getLocation()));
            else if (rc.canFireSingleShot())
                rc.fireSingleShot(myLocation.directionTo(targetBot.getLocation()));
        }

    }

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
    **/
    @SuppressWarnings("unused")
    static void runSoldier(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        soldier.rc = rc;
        System.out.println("I'm a soldier!");
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