package sjxbin;

import battlecode.common.*;
import scala.Tuple3;

/**
 * Created by azane on 1/18/17.
 */
public strictfp class SjxMicrogradients {

    public static double[] macroGradient(MapLocation myLocation, RobotType myType) {
        // TODO read target broadcast channels
        // TODO convert coords to location
        // TODO calculate the attraction/repulsion of each macro target given type
    }

    public static boolean willShootFriends(
            Tuple3<double[], double[], double[]> friendlyMilitaryDonut, MapLocation myLocation) {
        // TODO
        return false;
    }

    // Returns <inside, outside, outside-inside>
    public static Tuple3<double[], double[], double[]> friendlyMilitaryDonutGradient(
            MapLocation myLocation, RobotInfo robot) {

        // TODO
        return null;
    }

    public static double[] friendlyRaiderGradient(MapLocation myLocation, RobotInfo robot) {

        if (robot.type != RobotType.SCOUT && robot.type != RobotType.LUMBERJACK) {
            return new double[2];
        }

        // TODO
        return null;
    }

    public static double[] enemyKitingDonutGradient(MapLocation myLocation, RobotInfo robot) {
        // TODO
        return null;
    }

    public static double[] fleeEnemyGradient(MapLocation myLocation, RobotInfo robot) {
        // TODO overload that takes a RobotInfo to excessive looping.
        // TODO
        return null;
    }

    // Overload for treeGradient, defaulting to avoiding tree, i.e. negating the derivative.
    public static double[] treeGradient(MapLocation myLocation, RobotController rc) {
        return treeGradient(myLocation, rc, false);
    }

    // This has all tree curves as positive. Just negate to avoid them.
    public static double[] treeGradient(MapLocation myLocation, RobotController rc, boolean seek) {

        // Sense all nearby trees.
        TreeInfo[] trees = rc.senseNearbyTrees();

        // Init the gradient vector over which we'll sum..
        double[] gradient = new double[] {0., 0.};

        // Store the number of trees for normalizing (averaging).
        int numTrees = 0;

        // Iterate nearby trees
        for (int i = 0; i < trees.length; ++i) {

            double standardDeviation = trees[i].radius * 1;

            // Each tree gets a gaussian with standard deviation a function of its radius.
            gradient = SjxMath.elementwiseSum(
                            gradient,
                            SjxMath.gaussianDerivative(myLocation, trees[i].location,
                                    standardDeviation, 2),
                            false
            );
            numTrees++;
        }

        // Normalize gradient over all nearby trees.
        if (numTrees > 0) {
            gradient[0] /= numTrees;
            gradient[1] /= numTrees;
        }

        if (!seek) {
            gradient[0] *= -1;
            gradient[1] *= -1;
        }

        return gradient;
    }
}
