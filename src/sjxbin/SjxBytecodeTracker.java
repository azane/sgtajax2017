package sjxbin;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import lumber_jack_s.RobotPlayer;

/**
 * Created by azane on 1/23/17.
 */
public class SjxBytecodeTracker {

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

    public SjxBytecodeTracker() {
        start = -1;
        turnLimit = Clock.getBytecodeNum() + Clock.getBytecodesLeft();;
        roundStart = -1;
        running = false;
        totalCost = -1;
        costSinceLastPoll = -1;
        bytecodeAllotment = 0;
    }

    public void start(int bytecodeAllotment) {
        if (running) throw new RuntimeException("Must call .end() before starting again.");

        running = true;
        roundStart = RobotPlayer.rc.getRoundNum();

        costSinceLastPoll = 0;

        this.bytecodeAllotment = bytecodeAllotment;

        start = Clock.getBytecodeNum();
    }

    public int poll() {
        if (!running) throw new RuntimeException("Must call .start() before polling.");

        int lastTotalCost = totalCost;
        // The number of rounds passed * the bytecode turn limit
        //  + the difference between the current bytecode position and the start.
        // NOTE the latter may be a positive number if lapping is occurring.
        totalCost = (RobotPlayer.rc.getRoundNum()-roundStart)*turnLimit
                + Clock.getBytecodeNum()-start;

        costSinceLastPoll = totalCost - lastTotalCost;

        return totalCost;
    }

    public int end() {
        int cost = poll();
        running = false;
        return cost;
    }

    public void yieldCheck() {
        // ?? If we check total cost, we risk looping back to the bots turn sooner than their
        //      abilities could refresh.
        // ?? If we go off of cost since last poll, we risk running over their turn if they need more time.
        //      Also, the longer the task, the more time it gets.
        poll();
        try {
            // Check change in cost, assuming same allotment.
            if (totalCost > bytecodeAllotment)// && costSinceLastPoll > bytecodeAllotment)
                RobotPlayer.rp.mainMethod();
        }
        catch(GameActionException e) {
            System.out.println("Task could not return to RobotPlayer's main method.");
        }
    }

    public boolean isAllotmentExceeded() {
        return (totalCost > bytecodeAllotment);
    }
}