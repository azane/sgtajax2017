package sjxbin;

import battlecode.common.Clock;
import battlecode.common.RobotType;
import lumber_jack_s.RobotPlayer;

/**
 * Created by azane on 1/22/17.
 */
public class SjxYieldBytecode {

    public static void yield() {

        // TODO need to rethink allotment strategy given that tasks can call back to main method.

        int[] bytecodeAllotments = calculateBytecodeAllotments();

        for (int i = 0; i < bytecodeAllotments.length; i++) {
            switch (i) {
                case(-1):
                    // See implementation for task example.
                    // Note that tasks don't have to be part of this class.
                    sampleTask(bytecodeAllotments[i]);
                    break;

                case(0):
                    RobotPlayer.predictiveShooter.collectDataAndTrainModel(bytecodeAllotments[i]);
                    //Clock.yield();
                    break;
            }
        }
    }

    private static void sampleTask(int bytecodeAllotment) {

        // Using SjxBytecodeTracker makes this LOTS easier.
        SjxBytecodeTracker bct = new SjxBytecodeTracker();
        bct.start(bytecodeAllotment);

        // The BytecodeTracker tracks bytecode usage even across rounds.
        while (!bct.isAllotmentExceeded()) {
            // Sample task activities. : )
            double four = 2. + 2.;

            // Anywhere in the sampleTask, the task might exceed the bytecode allotment before it can
            //  check it again at the top of the loop.
            // If there is significant risk of this happening, the task should call the
            // robot player's main execution method.
            // When the execution method returns, the task will resume, and then exit at the top of the
            //  loop because it doesn't receive any new bytecode allotments.
            bct.yieldCheck();

            double five = four + 1;
        }

        bct.end();
    }

    private static int[] calculateBytecodeAllotments() {

        int bytecodesLeft = Clock.getBytecodesLeft();

        double[] taskAllotmentRatios = calculateAllotmentRatios();
        int[] taskBytecodeAllotments = new int[taskAllotmentRatios.length];

        double tot = (new Matrix(taskAllotmentRatios)).sumOver('N').getData(0, 0);
        for (int i = 0; i < taskAllotmentRatios.length; i++) {

            taskBytecodeAllotments[i] = (int)Math.floor(
                    (taskAllotmentRatios[i]/tot)*bytecodesLeft
            );
        }

        return taskBytecodeAllotments;
    }

    private static double[] calculateAllotmentRatios() {
        // Just return the task allotment for now, but we can do this formulaically too.
        // The indices of the return values should correspond to the indices of the methods
        //  called from the switch statement in yield();
        return new double[] {1.};
    }
}
