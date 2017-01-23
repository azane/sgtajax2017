package sjxbin;

import battlecode.common.*;

import lumber_jack_s.RobotPlayer;

import java.util.HashMap;
import java.util.LinkedList;


/**
 * Created by azane on 1/22/17.
 */
public class SjxPredictiveShooter {

    static int INPUT_POSITIONS = 5;
    static int OUTPUT_POSITIONS = 1;
    static int TRAIL_LENGTH = INPUT_POSITIONS + OUTPUT_POSITIONS + 1; // +1 to relativize the first input.
    static int DATA_STORE_LENGTH = 25;

    HashMap<Integer, LinkedList<MapLocation>> botTrails = new HashMap<>();
    int[] annShape = new int[] {INPUT_POSITIONS*2 +1, 8, 4, OUTPUT_POSITIONS*2}; // +1 on input for bias.
    Matrix inputs = new Matrix(DATA_STORE_LENGTH, annShape[0]);
    Matrix outputs = new Matrix(DATA_STORE_LENGTH, annShape[annShape.length-1]);
    int dataPointsStored = 0;
    SjxANN ann = new SjxANN(annShape, false);

    // True if we include their data in prediction.
    // Might want to add unit tracking options.
    boolean trackAllies;
    boolean trackEnemies;

    public SjxPredictiveShooter(boolean trackAllies, boolean trackEnemies) {
        this.trackAllies = trackAllies;
        this.trackEnemies = trackEnemies;
    }

    public void collectDataAndTrainModel(int bytecodeAllotment) {

        int startingBytecode = Clock.getBytecodeNum();
        int endingBytecode = startingBytecode + bytecodeAllotment;

        trackUnits();

        // Only train if we've got a full set of data.
        // TODO enable after testing unit tracking.
//        if (dataPointsStored >= DATA_STORE_LENGTH) {
//            while (Clock.getBytecodeNum() < endingBytecode && Clock.getBytecodeNum() > startingBytecode)
//                ann.trainBackprop(inputs, outputs, 1., false, 1);
//        }
    }

    public void gleanDataFromTrail(int id, LinkedList<MapLocation> trail) {

        if (trail.size() != TRAIL_LENGTH)
            throw new RuntimeException("'trail' is not the correct size for the network.");

        double[] singleInput = new double[annShape[0]]; // +1 for bias.
        double[] singleOutput = new double[annShape[annShape.length-1]];

        // Fill the data vectors with the trail data.

        // <Input Vector>
        // This will read left to right, newest to oldest, then the bias.
        int vi = singleInput.length -1; // The reverse indexer for the input vector. -1 for index.

        // Store the bias.
        singleInput[vi--] = 1.;

        MapLocation thisLoc = null;
        MapLocation lastLoc = trail.getLast(); // the oldest location for relativizing.
        // Iterate backwards from the second to last trail location. -1 for second to last, -1 for index.
        // Stop before the trail locations designated for the outputs.
        int i;
        for (i = trail.size() -2; i >= OUTPUT_POSITIONS; i--) {

            thisLoc = trail.get(i);

            // Store the input value from the bot trail.
            // Relativize from the last location.
            // TODO normalize to [-1,1] for ann.
            singleInput[vi--] = thisLoc.y - lastLoc.y;
            singleInput[vi--] = thisLoc.x - lastLoc.x;

            lastLoc = thisLoc;
        }
        // </Input Vector>

        // <Output Vector>
        int vo = annShape[annShape.length-1] -1; // The reverse indexer for the output vector. -1 for index.

        // Continue from the input loop index, i.
        for (int o = i; o >= 0; o--) {

            thisLoc = trail.get(o);

            // Store the input value from the bot trail.
            // Relativize from the last location.
            // TODO normalize to [-1,1] for ann.
            singleOutput[vo--] = thisLoc.y - lastLoc.y;
            singleOutput[vo--] = thisLoc.x - lastLoc.x;

            lastLoc = thisLoc;
        }

        // </Output Vector>

        // If the datasets are full, replace a random value in the array with the new data point.
        int index;
        if (dataPointsStored >= DATA_STORE_LENGTH) {
            index = (int)Math.round(Math.random()* DATA_STORE_LENGTH);
            dataPointsStored++;
        }
        // If not full, fill in order.
        else {
            index = dataPointsStored;
            dataPointsStored++;
        }

        inputs.assignRowInPlace(new Matrix(singleInput), index);
        outputs.assignRowInPlace(new Matrix(singleOutput), index);

    }

    public void trackUnit(RobotInfo robot) {

        if ((robot.getTeam() == RobotPlayer.rc.getTeam()) && !trackAllies)
            return;
        if ((robot.getTeam() != RobotPlayer.rc.getTeam()) && !trackEnemies)
            return;

        int id = robot.getID();

        botTrails.putIfAbsent(id, new LinkedList<>());

        LinkedList<MapLocation> trail = botTrails.get(id);
        trail.addFirst(robot.getLocation()); // left to right, newest to oldest.

        // Only if we have enough data to create a point.
        if (trail.size() == TRAIL_LENGTH+1) {
            trail.removeLast();

            // Only create a data point 1/(trail length) of the time we add a trail coord.
            if (Math.random() < 1./TRAIL_LENGTH) {

                gleanDataFromTrail(id, trail);
            }
        }
    }
    public void trackUnits(RobotInfo[] robots) {

        for (RobotInfo robot : robots) {
            trackUnit(robot);
        }
    }
    public RobotInfo[] trackUnits() {
        RobotInfo[] rinfo = RobotPlayer.rc.senseNearbyRobots();
        trackUnits(rinfo);
        return rinfo;
    }
}
