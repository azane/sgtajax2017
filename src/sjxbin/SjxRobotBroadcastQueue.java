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
    private final static String validdataLbl = "validdata";

    public final static String[] ROBOTLABELS = new String[] {
            idLbl,
            xLbl,
            yLbl,
            typeLbl,
            turnsensedLbl,
            validdataLbl
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

        dict.put(idLbl, robot.getID());

        MapLocation loc = robot.getLocation();
        dict.put(xLbl, (int)(loc.x * locationPrecisionMultiplier));
        dict.put(yLbl, (int)(loc.y * locationPrecisionMultiplier));

        dict.put(typeLbl, typeToCode.get(robot.type));

        dict.put(turnsensedLbl, rc.getRoundNum());

        // All enqueued data starts valid.
        dict.put(validdataLbl, 1);

        return dict;
    }

    private final SjxBroadcastQueue queue;
    private final RobotController rc;

    public SjxRobotBroadcastQueue(RobotController rc,
                                  int size, int startingChannel, int positionIndicatorChannel,
                                  int numberElementsChannel) {

        this.rc = rc;
        queue = new SjxBroadcastQueue(ROBOTLABELS, this.rc, size,
                startingChannel, positionIndicatorChannel, numberElementsChannel,
                true);

    }

    public int getNumElements() {
        return queue.getNumElements();
    }

    private boolean isCurrentValidAsFarAsWeKnow() {
        // If we can sense the location, but can't sense the robot, this data is invalid.
        // Also require that the data is reasonably old. We can use recent data for
        //  predictive shooting.
        // These are in order of most likely to fail, so we can avoid checking if we don't
        //  have to.
        if (getInfoAge() > SjxPredictiveShooter.TRAIL_LENGTH + 3
                && rc.canSenseLocation(getLocation())
                && !rc.canSenseRobot(getId())) {
            // Draw a line from the invalidator to the location being invalidated.
            rc.setIndicatorLine(rc.getLocation(), this.getLocation(), 255, 0, 0);
            return false;
        }
        else
            return true;
    }

    public void popInvalidsTask(int bytecodeallotment, boolean yieldForBroadcast,
                                 boolean readWriteMetaData) {
        // first for some popping.
        queue.first();

        if (readWriteMetaData)
            queue.readMetadata();

        // Cleaning up the data is not time essential, and will happen when people
        //  aren't needing to shoot their guns.
        SjxBytecodeTracker bct = new SjxBytecodeTracker();
        bct.start(bytecodeallotment);
        bct.poll();
        // Pop invalidated elements, by others or us, till we get to the bottom!
        while (!queue.isEmpty() && !bct.isAllotmentExceeded()
                && (!getValidity() || !isCurrentValidAsFarAsWeKnow())) {
            // Draw a blue line to the location that's being popped.
            rc.setIndicatorLine(rc.getLocation(), getLocation(), 0, 0, 255);
            //System.out.println("Popping queue!");
            queue.pop();
            bct.poll();
        }

        if (readWriteMetaData)
            // Write the meta data after popping.
            queue.writeMetadata();

        if (yieldForBroadcast)
            bct.yieldForBroadcast();
        bct.end();
    }

    public void globalPrepIter() {
        queue.readMetadata();
        nullifyCache();

        // Now call prep iter, as we're ready to iter!
        queue.prepIter();
        nullifyCache();
    }

    public boolean next() {
        // If there is no next, return false.
        if (!queue.next())
            return false;

        nullifyCache();

        // We can't pop elements here, because we might not be at the top of the stack.
        // Instead, we can just update the validity of the entry.
        // Make sure it hasn't already been invalidated.
        // Also, it's not really important that this get updated on THIS turn, as invalid is invalid.
        //  i.e. once invalid, always invalid. no race conditions.
        if (getValidity() && !isCurrentValidAsFarAsWeKnow()) {
            queue.writeCurrent(validdataLbl, 0);
            cachedValidity = false;
        }

        return true;
    }
    private void nullifyCache() {
        cachedLocation = null;
        cachedX = null;
        cachedY = null;
        cachedType = null;
        cachedTurnSensed = null;
        cachedValidity = null;
        cachedId = null;
    }
    public boolean nextExists() {
        return queue.nextExists();
    }
    public int getSize() {
        return queue.size;
    }
    public int getCurrentIndex() {
        return queue.getCurrentIndex();
    }

    private Boolean cachedValidity = null;
    public boolean getValidity() {
        if (cachedValidity == null) {
            int intbool = queue.readCurrent(validdataLbl);
            if (intbool == 0)
                cachedValidity = false;
            else if (intbool == 1)
                cachedValidity = true;
            else
                throw new RuntimeException("bot validity was neither 0 nor 1.");
        }
        return cachedValidity;
    }

    private Integer cachedId = null;
    public int getId() {
        if (cachedId == null)
            cachedId = queue.readCurrent(idLbl);
        return cachedId;
    }

    private MapLocation cachedLocation = null;
    private Float cachedX = null;
    private Float cachedY = null;
    public MapLocation getLocation() {
        if (cachedLocation == null)
            cachedLocation = new MapLocation(getX(), getY());
        return cachedLocation;
    }
    public float getX() {
        if (cachedX == null)
            cachedX = ((float)queue.readCurrent(xLbl))/locationPrecisionMultiplier;
        return cachedX;
    }
    public float getY() {
        if (cachedY == null)
            cachedY = ((float)queue.readCurrent(yLbl))/locationPrecisionMultiplier;
        return cachedY;
    }

    private RobotType cachedType = null;
    public RobotType getType() {
        if (cachedType == null)
            cachedType = codeToType.get(queue.readCurrent(typeLbl));
        return cachedType;
    }

    private Integer cachedTurnSensed = null;
    public int getTurnSensed() {
        if (cachedTurnSensed == null)
            cachedTurnSensed = queue.readCurrent(turnsensedLbl);
        return cachedTurnSensed;
    }
    public int getInfoAge() {
        return rc.getRoundNum() - getTurnSensed();
    }

    public RobotInfo getRobot() {
        return new RobotInfo(getId(), rc.getTeam().opponent(), getType(), getLocation(),
                0,0,0);
    }

    // Takes care of updating the queue position (read/write), queuing the array,
    //  and yielding for the broadcast.
    public void enqueueBatchTask(RobotInfo[] robots, int bytecodeallotment) {

        // Note: this task can abort at any time, so there's no need to yield
        //  to the main method, we can just return entirely.
        // It does, however require a yield for broadcast, and will advance the turn.
        // TODO find a way to tell the tasker that a broadcast write is needed before
        //  the turn is up, so other tasks can continue, and do one write at the end.

        SjxBytecodeTracker bct = new SjxBytecodeTracker();
        // Reserve some space to make sure of the write/yield at the end.
        bct.start(Math.max(bytecodeallotment-1000, 0));
        bct.poll();
        if (bct.isAllotmentExceeded())
            return;

        queue.readMetadata();

        // Run the popping task a bit first, give it everything robots is empty.
        // Note that this probably won't run to its full allotment anyway.
        int popTaskBytecodeAllotment;
        if (robots.length == 0)
            popTaskBytecodeAllotment = bct.getBytecodeAllotment();
        else
            popTaskBytecodeAllotment = bct.getBytecodeAllotment()/3;
        // Don't yield or read/write, we're taking care of that out here.
        popInvalidsTask(popTaskBytecodeAllotment, false, false);

        for (RobotInfo robot : robots) {
            if (bct.isAllotmentExceeded())
                break;
            queue.enqueue(convert(robot, rc));
            bct.poll();
        }
        queue.writeMetadata();
        bct.yieldForBroadcast();
        bct.end();
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
                10, 500, 498,
                499);

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
        queue.enqueueBatchTask(robots, 50000);

        // Iterate through the queue to make sure it's content is accurate.
        queue.globalPrepIter();
        for (int i = max-1; i > max - queue.getSize() - 1; i--) {

            queue.next();

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
            if (!queue.getValidity())
                return false;
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
