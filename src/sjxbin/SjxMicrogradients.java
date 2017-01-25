package sjxbin;

import battlecode.common.*;
import lumber_jack_s.RobotPlayer;
import scala.Tuple3;

import java.util.HashMap;

/**
 * Created by azane on 1/18/17.
 */
public strictfp class SjxMicrogradients {

    // singleton
    public static SjxMicrogradients instance;
    public SjxMicrogradients() {
        instance = this;
        setScales();
    }

    private final RobotController me = RobotPlayer.rc;
    private final RobotType myType = me.getType();
    private final double senseRToStdev = .7;
    private final double range = myType.sensorRadius*senseRToStdev;

    private double friendlyMilitaryAttractionScale = 1.;
    private double friendlyMilitaryRepulsionScale = 10.;
    private double friendlyRaiderScale = 1.;
    private double enemyMilitaryAttractionScale = 1.;
    private double enemyMilitaryRepulsionScale = 1.;
    private double fleeEnemyScale = 1.;
    private double treeScale = 2.;

    private double macroEconomicTargetScale = 1.;
    private double macroMilitaryTargetScale = 1.;
    private double macroDefenseTargetScale = 1.;

    public void setScales() {
        // TODO set the scales given broadcasted information and robot type.

        switch (myType) {
            case TANK:
            case SOLDIER:
                treeScale = 2.;
                break;
            case LUMBERJACK:
                treeScale = 3;
        }
    }

    public double[] getMyGradient() {
        // TODO given the robot's type, calculate it's gradient.
        return null;
    }

    public double[] macroGradient(MapLocation myLocation) {
        // TODO read target broadcast channels
        // TODO convert coords to location
        // TODO calculate the attraction/repulsion of each macro target given robot type.
        return null;
    }

    public boolean willShootFriends(
            double[][] friendlyMilitaryDonut, MapLocation myLocation, MapLocation targetLocation) {

        // Verify that the enemy gradient opposes the friendly gradient by at least 45 degrees.
        //  This *should* prevent friendly fire?
        // Note, the dot product is 0 when orthogonal (perpendicular), and 1 when parallel.

        double dp = SjxMath.dotProduct(friendlyMilitaryDonut[0], enemyGradient);
        return (dp < .5);
    }


    // Returns <outside, inside, outside-inside>
    public double[][] friendlyMilitaryDonutGradient(
            MapLocation myLocation, RobotInfo robot) {

        double outsideStandardDeviation = range;
        double insideStandardDeviation = myType.bodyRadius*3.;

        double[][] retval = new double[3][2];

        retval[0] = SjxMath.gaussianDerivative(
                myLocation, robot.location, outsideStandardDeviation, friendlyMilitaryAttractionScale);
        retval[1] = SjxMath.gaussianDerivative(
                myLocation, robot.location, insideStandardDeviation, friendlyMilitaryRepulsionScale);
        retval[2] = SjxMath.elementwiseSum(retval[0], retval[1], true);

        return retval;
    }
    public double[][] friendlyMilitaryDonutGradient(
            double[][] initialGradient, MapLocation myLocation, RobotInfo robot) {

        if (initialGradient == null) initialGradient = new double[3][2];

        double[][] thisGradient = friendlyMilitaryDonutGradient(myLocation, robot);

        for (int i = 0; i < initialGradient.length; i++) {
            thisGradient[i] = SjxMath.elementwiseSum(thisGradient[i], initialGradient[i], false);
        }

        return thisGradient;
    }

    public double[] friendlyRaiderGradient(MapLocation myLocation, RobotInfo robot) {

        return SjxMath.gaussianDerivative(
                myLocation, robot.location, range, friendlyRaiderScale);
    }
    public double[] friendlyRaiderGradient(
            double[] initialGradient, MapLocation myLocation, RobotInfo robot) {

        if (initialGradient == null) initialGradient = new double[2];

        return SjxMath.elementwiseSum(
                friendlyRaiderGradient(myLocation, robot), initialGradient, false);
    }


    // double[][] is <outside, inside>
    private HashMap<RobotType, double[]> kitingDeviations = createKitingDeviations();
    private HashMap<RobotType, double[]> createKitingDeviations()
    {
        HashMap<RobotType, double[]> myMap = new HashMap<RobotType, double[]>();
        if (myType == RobotType.SCOUT)
            myMap.put(RobotType.SCOUT, new double[] {range/2., myType.bodyRadius});
        else
            myMap.put(RobotType.SCOUT, new double[] {0., 0.});
        myMap.put(RobotType.LUMBERJACK, new double[] {range, RobotType.LUMBERJACK.strideRadius*2.});
        myMap.put(RobotType.SOLDIER, new double[] {range, range/2.});
        myMap.put(RobotType.TANK, new double[] {range, range/1.5});
        myMap.put(RobotType.ARCHON, new double[] {range, RobotType.ARCHON.strideRadius*1.5});
        myMap.put(RobotType.GARDENER, new double[] {range, RobotType.GARDENER.strideRadius*1.5});
        return myMap;
    }
    public double[] enemyKitingDonutGradient(MapLocation myLocation, RobotInfo robot) {

        double outsideStandardDeviation = kitingDeviations.get(robot.type)[0];
        double insideStandardDeviation = kitingDeviations.get(robot.type)[1];

        double[] outsideGradient = SjxMath.gaussianDerivative(
                myLocation, robot.location, outsideStandardDeviation, enemyMilitaryAttractionScale);
        double[] insideGradient = SjxMath.gaussianDerivative(
                myLocation, robot.location, insideStandardDeviation, enemyMilitaryRepulsionScale);

        return SjxMath.elementwiseSum(outsideGradient, insideGradient, true);

    }
    public double[] enemyKitingDonutGradient(
            double[] initialGradient, MapLocation myLocation, RobotInfo robot) {

        if (initialGradient == null) initialGradient = new double[2];

        return SjxMath.elementwiseSum(
                enemyKitingDonutGradient(myLocation, robot), initialGradient, false);
    }


    public double[] fleeEnemyGradient(MapLocation myLocation, RobotInfo robot) {

        return SjxMath.gaussianDerivative(
                myLocation, robot.location, range, fleeEnemyScale);

    }
    public double[] fleeEnemyGradient(
            double[] initialGradient, MapLocation myLocation, RobotInfo robot) {

        if (initialGradient == null) initialGradient = new double[2];

        return SjxMath.elementwiseSum(
                fleeEnemyGradient(myLocation, robot), initialGradient, false);
    }

    // This has all tree curves as positive. Just negate to avoid them.
    public double[] treeGradient(MapLocation myLocation, RobotController rc) {

        // Sense all nearby trees.
        TreeInfo[] trees = rc.senseNearbyTrees();

        // Init the gradient vector over which we'll sum..
        double[] gradient = new double[] {0., 0.};

        // Store the number of trees for normalizing (averaging).
        int numTrees = 0;

        // Iterate nearby trees
        for (int i = 0; i < trees.length; ++i) {

            double standardDeviation = trees[i].radius*1.2;

            // Each tree gets a gaussian with standard deviation a function of its radius.
            gradient = SjxMath.elementwiseSum(
                            gradient,
                            SjxMath.gaussianDerivative(myLocation, trees[i].location,
                                    standardDeviation, treeScale),
                            false
            );
            numTrees++;
        }

        // Normalize gradient over all nearby trees.
        if (numTrees > 0) {
            gradient[0] /= numTrees;
            gradient[1] /= numTrees;
        }

        return gradient;
    }
}
