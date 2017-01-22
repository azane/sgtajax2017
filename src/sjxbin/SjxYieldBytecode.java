package sjxbin;

import battlecode.common.Clock;
import lumber_jack_s.RobotPlayer;

/**
 * Created by azane on 1/22/17.
 */
public class SjxYieldBytecode {

    public static void yield(RobotPlayer rp) {

        // Get this info to check overflow after tasks complete.
        int startBytecodesLeft = Clock.getBytecodesLeft();

        int[] bytecodeAllotments = calculateBytecodeAllotments(rp);

        for (int i = 0; i < bytecodeAllotments.length; i++) {
            switch (i) {
                case(-1):
                    // See implementation for task example.
                    // Note that tasks don't have to be part of this class,
                    //  they can (maybe should) be from the RobotPlayer, "rp".
                    sampleTask(bytecodeAllotments[i]);
                    break;
                case(0):
                    //task0(bytecodeAllotments[i]);
                    break;
                case(1):
                    //task1(bytecodeAllotments[i]);
                    break;
                //etc.
            }
        }

        // If there are fewer bytecodes left than there were at the start, we probably didn't run over,
        //  so yield the turn to avoid the bot restarting its loop.
        int endBytecodesLeft = Clock.getBytecodesLeft();
        String message = "SjxYieldBytecode.yield() completed with " + endBytecodesLeft + " bytecodes remaining." +
                "\nOverrun very ";
        if (endBytecodesLeft < startBytecodesLeft) {
            System.out.println(message + "unlikely.");
            Clock.yield();
        }
        // Otherwise, bytecodes left is larger,  we probably did run over.
        // Don't yield as the bot needs to do its turn.
        else {
            System.out.println(message + "likely.");
        }

    }

    private static void sampleTask(int bytecodeAllotment) {

        // Get the bytecode amount that this task should not exceed, in absolute terms.
        // starting bytecode + allotment.
        // ALWAYS check starting bytecode in the task, not outside, as other tasks may have run over the limit.
        // Failing to check the starting bytecode in the task might result in a task "lapping" and taking an
        //  entire turn.
        int endingBytecode = Clock.getBytecodeNum() + bytecodeAllotment;

        // Do stuff until reaching the ending bytecode.
        while (Clock.getBytecodeNum() < endingBytecode) {
            // If there is a lot in here, the task could exceed its bytecode allotment.
            // This is more or less fine, because if the tasks exceed the bytecode limit, the bot will
            //  resume on its next turn, complete the small amount that remains of the tasks, and then
            //  continue its turn. In fact, slight overrun might actually be BETTER, because no bytecode
            //  will have been wasted. All that's required is that the bot has enough bytecode between
            //  the last turn's overrun and the turn limit, so it can do its turn, and then trigger tasks again.
            // To minimize overrun, these methods might be helpful:
            // 1. Keep an int that tracks how long each loop is taking, and don't enter another loop
            //      unless there is enough bytecode remaining to complete the loop and stay under
            //      the bytecode allotment.
            // 2. Make checks inside the main loop that can safely abort the task mid-loop, if the
            //      bytecode allotment is in danger of running over.

            // Sample task activities. : )
            double four = 2. + 2.;
        }
    }

    private static int[] calculateBytecodeAllotments(RobotPlayer rc) {

        int bytecodesLeft = Clock.getBytecodesLeft();

        double[] taskAllotmentRatios = calculateAllotmentRatios(rc);
        int[] taskBytecodeAllotments = new int[taskAllotmentRatios.length];

        double tot = (new Matrix(taskAllotmentRatios)).sumOver('N').getData(0, 0);
        for (int i = 0; i < taskAllotmentRatios.length; i++) {

            taskBytecodeAllotments[i] = (int)Math.floor(
                    (taskAllotmentRatios[i]/tot)*bytecodesLeft
            );
        }

        return taskBytecodeAllotments;
    }

    private static double[] calculateAllotmentRatios(RobotPlayer rc) {
        // Just return the task allotment for now, but we can do this formulaically too.
        // The indices of the return values should correspond to the indices of the methods
        //  called from the switch statement in yield();
        return new double[] {};
    }
}
