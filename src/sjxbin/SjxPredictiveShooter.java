package sjxbin;

import battlecode.common.*;

import lumber_jack_s.RobotPlayer;

import java.util.HashMap;
import java.util.LinkedList;


/**
 * Created by azane on 1/22/17.
 */
public class SjxPredictiveShooter {

    static int INPUT_POSITIONS = 4;
    static int OUTPUT_POSITIONS = 1;
    static int TRAIL_LENGTH = INPUT_POSITIONS + OUTPUT_POSITIONS + 1; // +1 to relativize the first input.
    static int DATA_STORE_LENGTH = 5;

    static final int startingChannel = 102;
    static final int weightBits = 32;
    // The channel on which the flag 1/0 is stored indicating if the network has
    //  been initialized.
    // TODO make an integer a flag int for bools.
    static int networkInitializedChannel = 101;
    static int networkProcessingIDChannel = 100;

    int trainingIters = 0;

    HashMap<Integer, LinkedList<MapLocation>> botTrails = new HashMap<>();
    int[] annShape = new int[] {INPUT_POSITIONS*2 +1, 6, 4, OUTPUT_POSITIONS*2}; // +1 on input for bias.
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

    // Pre-store to avoid garbage collection.
    private SjxBytecodeTracker bct = new SjxBytecodeTracker();
    public void collectDataAndTrainModel(int bytecodeAllotment) {

        bct.start(bytecodeAllotment);

        // TODO might want to take advantage of the bot looping nearby robots in its main turn
        //  by calling trackUnit(RobotInfo robot) per loop. Otherwise, we loop the same array twice.
        trackUnits(bct);

        bct.yieldCheck();

        // Only train if we've got a full set of data.
        if (dataPointsStored >= DATA_STORE_LENGTH) {

            try{
                // TODO use 'networkProcessingIDChannel' to denote that this bot is processing the network.
                // TODO check to make sure that either this bot is processing, or that no bot is processing (-1);
                if (RobotPlayer.rc.readBroadcast(networkInitializedChannel) == 1)
                    ann.readBroadcastWeights(startingChannel, weightBits);
            }
            catch (GameActionException e) {
                System.out.println("Could not determine if predictive shooter network was initialized!");
            }

            while (!bct.isAllotmentExceeded()) {
                try {
                    //if (RobotPlayer.rc.getType() == RobotType.SCOUT)
                    ann.trainBackprop(inputs, outputs, 1., true, 1, bct);
                }
                catch (Exception e) {
                    System.out.println("The backprop method crashed!");
                }
                ++trainingIters;
            }

            ann.broadcastWeights(startingChannel, weightBits);

            try {
                RobotPlayer.rc.broadcast(networkInitializedChannel, 1);
                // TODO denote that this bot is finished processing the network by setting the id channel to -1;
            }
            catch (GameActionException e) {
                System.out.println("Could not broadcast indication that the predictive shooter " +
                        "network has valid weights in it!");
            }
        }

        // Debug
        if (trainingIters > 200) {
            bct.poll();
        }

        bct.end();
    }

    private boolean stationaryOrNonSequential(double[] trail, boolean hasBias,
                                              boolean checkStationarity) throws Exception{

        if (ann.bias > 1.)
            throw new Exception("The network bias must be within normalization range.");

        boolean allZero = checkStationarity; // If true, normal. If false, we assume there is a non zero.
        for (int j = 0; j < trail.length; j++) {
            if (allZero && 0 != trail[j]
                    && hasBias && j != trail.length-1) { // Don't check last one, that's the bias.
                allZero = false;
            }
            // If the change in position is too large.
            if (Math.abs(trail[j]) > 1.) {
                return true;
            }
        }
        return allZero;
    }

    // Pre declaration to keep memory location.
    double[] singleInput = new double[annShape[0]];
    double[] singleOutput = new double[annShape[annShape.length-1]];
    public void gleanDataFromTrail(int id, LinkedList<MapLocation> trail) {

        if (trail.size() != TRAIL_LENGTH)
            throw new RuntimeException("'trail' is not the correct size for the network.");

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
            index = (int)Math.floor(Math.random()* DATA_STORE_LENGTH);
        }
        // If not full, fill in order.
        else {
            index = dataPointsStored;
        }

        try {
            // Check to make sure the bot isn't stationary and that we don't have a coverage gap in the trail.
            // If total of the input is the bias (1.) it's baddy.
            if (stationaryOrNonSequential(singleInput, true, true)
                    || stationaryOrNonSequential(singleOutput, false, false)) {
                System.out.println("The targets trail has a coverage gap or is stationary.");
                trail.clear();
            } else {
                inputs.assignRowInPlace(singleInput, index);
                outputs.assignRowInPlace(singleOutput, index);
                dataPointsStored++;
            }
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }


    }

    private void trackUnit(RobotInfo robot) {

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

    public void trackUnits(RobotInfo[] robots, SjxBytecodeTracker bct) {

        for (RobotInfo robot : robots) {
            trackUnit(robot);
            bct.yieldCheck();
        }
    }

    // Only track robots once per round.
    private int lastRound;
    private int thisRound = RobotPlayer.rc.getRoundNum();
    public void trackUnits(SjxBytecodeTracker bct) {

        lastRound = thisRound;
        thisRound = RobotPlayer.rc.getRoundNum();
        if (lastRound == thisRound) return;

        RobotInfo[] rinfo = RobotPlayer.rc.senseNearbyRobots();
        trackUnits(rinfo, bct);
    }
}
