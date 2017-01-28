package sjxbin;

import battlecode.common.*;

import java.util.HashMap;
import java.util.Random;

/**
 * Created by azane on 1/28/17.
 */
public class SjxRobotBroadcastQueue {

    private final static String idLbl = "id";
    private final static String xLbl = "x";
    private final static String yLbl = "y";
    private final static String typeLbl = "type";
    private final static String turnsensedLbl = "turnsensed";

    public final static String[] ROBOTLABELS = new String[] {
            idLbl,
            xLbl,
            yLbl,
            typeLbl,
            turnsensedLbl
    };

    private final static HashMap<RobotType, Integer> typeToCode = new HashMap<>();
    private final static HashMap<Integer, RobotType> codeToType = new HashMap<>();
    static {
        int code = 0;
        for (RobotType type : RobotType.values()) {
            typeToCode.put(type, code);
            codeToType.put(code, type);
            code++;
        }
    }

    private final static float locationPrecisionMultiplier = 100000;

    private static HashMap<String, Integer> convert(RobotInfo robot, RobotController rc) {

        HashMap<String, Integer> dict = new HashMap<>();

        dict.put(idLbl, robot.ID);

        MapLocation loc = robot.getLocation();
        dict.put(xLbl, (int)(loc.x * locationPrecisionMultiplier));
        dict.put(yLbl, (int)(loc.y * locationPrecisionMultiplier));

        dict.put(typeLbl, typeToCode.get(robot.type));

        dict.put(turnsensedLbl, rc.getRoundNum());

        return dict;
    }

    private final SjxBroadcastQueue queue;
    private final RobotController rc;

    public SjxRobotBroadcastQueue(RobotController rc,
                                  int size, int startingChannel, int positionIndicatorChannel) {

        this.rc = rc;
        queue = new SjxBroadcastQueue(ROBOTLABELS, this.rc, size,
                startingChannel, positionIndicatorChannel, true);

    }

    public void globalFirst() {
        queue.readQueuePosition();
        queue.first();
    }
    public void next() {
        queue.next();
    }
    public boolean nextExists() {
        return queue.nextExists();
    }
    public int getSize() {
        return queue.size;
    }

    public int getId() {
        return queue.readCurrent(idLbl);
    }
    public MapLocation getLocation() {
        return new MapLocation(getX(), getY());
    }
    public float getX() {
        return ((float)queue.readCurrent(xLbl))/locationPrecisionMultiplier;
    }
    public float getY() {
        return ((float)queue.readCurrent(yLbl))/locationPrecisionMultiplier;
    }
    public RobotType getType() {
        return codeToType.get(queue.readCurrent(typeLbl));
    }
    public int getTurnSensed() {
        return queue.readCurrent(turnsensedLbl);
    }

    // Takes care of updating the queue position (read/write), queuing the array,
    //  and yielding for the broadcast.
    public void enqueueBatch(RobotInfo[] robots) {
        // Track each iteration's bytecode use, yield if necessary for broadcast.
        SjxBytecodeTracker bct = new SjxBytecodeTracker();
        bct.start(Clock.getBytecodesLeft());

        queue.readQueuePosition();
        for (RobotInfo robot : robots) {
            if (bct.getCostSinceLastPoll() > Clock.getBytecodesLeft())
                queue.yieldForBroadcast();
            bct.poll();
            queue.enqueue(convert(robot, rc));
        }
        queue.writeQueuePosition();
        // TODO add a bct tracker to the robot player that tracks the cost of the
        //  main method. If we have enough bytecode to run the main method, run it.
        // else, yield.
        queue.yieldForBroadcast();
    }

    public static boolean test(RobotController rc) {
        return TestSjxRobotBroadcastQueue.test(rc);
    }
}

class TestSjxRobotBroadcastQueue {
    public static boolean test(RobotController rc) {

        int lockChannel = 999;
        // Only allow one bot to run this test at a time.
        try {
            Clock.yield();
            if (rc.readBroadcast(lockChannel) == 1)
                return true;
            else {
                rc.broadcast(lockChannel, 1);
                Clock.yield(); //Guarantee broadcast to other bots.
            }
        }
        catch (GameActionException e) {
            return false;
        }


        SjxRobotBroadcastQueue queue = new SjxRobotBroadcastQueue(rc,
                10, 500, 499);

        long rseed = 1986058301;
        Random r = new Random();
        r.setSeed(rseed);

        int max = queue.getSize() + queue.getSize()/2;
        RobotInfo[] robots = new RobotInfo[max];
        for (int i = 0; i < max; i++) {
            try {
                robots[i] = new RobotInfo(r.nextInt(32000), rc.getTeam(),
                        RobotType.values()[r.nextInt(1000) % RobotType.values().length],
                        new MapLocation(r.nextFloat(), r.nextFloat()),
                        5, 0, 0);
            }
            catch (Exception e) {
                return false;
            }
        }
        int turnSensed = rc.getRoundNum();
        queue.enqueueBatch(robots);

        // Iterate through the queue to make sure it's content is accurate.
        queue.globalFirst();
        for (int i = max-1; i > max - queue.getSize() - 1; i--) {
            if (queue.getId() != robots[i].getID())
                return false;
            if (Math.abs(queue.getX() - robots[i].getLocation().x) > 0.001)
                return false;
            if (Math.abs(queue.getY() - robots[i].getLocation().y) > 0.001)
                return false;
            if (queue.getType() != robots[i].getType())
                return false;
            if (Math.abs(turnSensed - queue.getTurnSensed()) > 5)
                return false;

            queue.next();
        }

        // Release the test to others.
        try {

            rc.broadcast(lockChannel, 0);
            Clock.yield(); //Guarantee broadcast to other bots.

        }
        catch (GameActionException e) {
            return false;
        }

        return true;
    }
}
