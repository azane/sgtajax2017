package sjxbin;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import lumber_jack_s.RobotPlayer;

/**
 * Created by azane on 1/23/17.
 */
public class SjxBytecodeTracker {

    static int numTrackers;

    private int turnLimit;

    private int start;
    private int roundStart;

    private boolean running;
    public boolean isRunning() {
        return running;
    }

    private int totalCost;
    public int getTotalCost() {
        return totalCost;
    }

    private int costSinceLastPoll;
    public int getCostSinceLastPoll() {
        return costSinceLastPoll;
    }

    private int bytecodeAllotment;
    public int getBytecodeAllotment() {
        return bytecodeAllotment;
    }

    private static int mainMethodCost = 0;
    public static int getMainMethodCost() {
        return mainMethodCost;
    }
    public void setMainMethodCost() {
        mainMethodCost = totalCost;
    }

    public SjxBytecodeTracker() {
        start = -1;
        turnLimit = Clock.getBytecodeNum() + Clock.getBytecodesLeft();;
        roundStart = -1;
        running = false;
        totalCost = -1;
        costSinceLastPoll = -1;
        bytecodeAllotment = 0;

        numTrackers++;
    }

    public void start(int bytecodeAllotment) {
        if (running)
            throw new RuntimeException("Must call .end() before starting again.");

        running = true;
        roundStart = RobotPlayer.rc.getRoundNum();

        costSinceLastPoll = 0;

        this.bytecodeAllotment = bytecodeAllotment;

        start = Clock.getBytecodeNum();
    }

    public int poll() {
        if (!running)
            throw new RuntimeException("Must call .start() before polling.");

        int lastTotalCost = totalCost;
        // The number of rounds passed * the bytecode turn limit
        //  + the difference between the readCurrent bytecode position and the start.
        // NOTE the latter may be a positive number if lapping is occurring.
        totalCost = (RobotPlayer.rc.getRoundNum()-roundStart)*turnLimit
                + Clock.getBytecodeNum()-start;

        costSinceLastPoll = totalCost - lastTotalCost;

        return totalCost;
    }

    public int end() {
        int cost = poll();
        running = false;
        numTrackers--;
        return cost;
    }

    public void yieldCheck() {
        // ?? If we check total cost, we risk looping back to the bots turn sooner than their
        //      abilities could refresh.
        // ?? If we go off of cost since last poll, we risk running over their turn if they need more time.
        //      Also, the longer the task, the more time it gets.

        poll();

        // Check change in cost, assuming same allotment.
        if (totalCost > bytecodeAllotment)// && costSinceLastPoll > bytecodeAllotment)
            yieldToMain();
    }

    public void yieldToMain() {

        try {
            RobotPlayer.rp.mainMethod(true);
        }
        catch (GameActionException e) {
            System.out.println("RobotPlayer's main method failed to complete.");
        }
    }

    private String yieldBytecodeNumbers = "";
    public void yieldForBroadcast() {

        // If we have enough bytecode to run the main method, and then yield, do that.
        if (Clock.getBytecodesLeft() > mainMethodCost*1.2)
            yieldToMain();
        else {
            System.out.println("Not yielding before broadcast. Not enough bytecode.");
            int remainder = Clock.getBytecodesLeft();
            yieldBytecodeNumbers += remainder + "\n";
            if (RobotPlayer.rc.getRoundNum() > 700)
                System.out.println();
        }

        // Subtract the bytecode from the rest of the turn from the total cost.
//        totalCost -= remainder;
//        costSinceLastPoll -= remainder;
        Clock.yield();
        //this.poll();
        //System.out.println();
    }

    public boolean isAllotmentExceeded() {
        return (totalCost > bytecodeAllotment);
    }
}
