package sjxbin;

import battlecode.common.*;
import lumber_jack_s.RobotPlayer;

import java.util.Arrays;
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
    private final Team myTeam = me.getTeam();

    public enum RobotGroup {
        ENEMYMILITARY, FRIENDLYMILITARY,
        FRIENDLYRAIDER, ENEMYRAIDER,
        ENEMYECONOMIC, FRIENDLYECONOMIC,
        NOGROUP
    }

    private RobotGroup getRobotGroup(RobotInfo robot) {
        switch (robot.type) {
            case ARCHON:
            case GARDENER:
                if (robot.getTeam() == myTeam)
                    return RobotGroup.FRIENDLYECONOMIC;
                else
                    return RobotGroup.ENEMYECONOMIC;
            case SCOUT:
            case LUMBERJACK:
                if (robot.getTeam() == myTeam)
                    return RobotGroup.FRIENDLYRAIDER;
                else
                    return RobotGroup.ENEMYRAIDER;
            case TANK:
            case SOLDIER:
                if (robot.getTeam() == myTeam)
                    return RobotGroup.FRIENDLYMILITARY;
                else
                    return RobotGroup.ENEMYMILITARY;
            default:
                return RobotGroup.NOGROUP;
        }
    }

    private final double senseRToStdev = 1.;
    private final double range = myType.sensorRadius*senseRToStdev;

    // These should all be positive! Negation is taken care of in the methods below,
    //  and differs by unit type sometimes.
    private double friendlyMilitaryAttractionScale = 6.;
    private double friendlyMilitaryRepulsionScale = 7.;
    private double friendlyRaiderRepulsionScale = 6.;
    // The scale at which you pursue normal duties related to the enemy military.
    private double enemyMilitaryDutyScale = 7.;
    // The scale at which you respond to danger related to enemy military.
    private double enemyMilitaryDangerScale = 5.;
    private double enemyEconomicAttractionScale = 6.;
    private double treeScale = 3.;

    private double macroEconomicTargetScale = 5.;
    private double macroMilitaryTargetScale = 6.;
    private double macroDefenseTargetScale = 6.;

    public void setScales() {
        // TODO set the scales given broadcasted information and robot type.

        switch (myType) {
            case TANK:
            case SOLDIER:
                treeScale = 1.;
                break;
            case LUMBERJACK:
                treeScale = 5.;
                friendlyRaiderRepulsionScale = 5.;
                enemyMilitaryDutyScale = 8.;
                break;
            case SCOUT:
                break;

        }
    }

    private RobotInfo target = null;
    // TODO use predictive aiming on said target.
    public MapLocation getShotLocation() {
        if (target == null)
            return null;
        else {
            // Release the target
            MapLocation loc = target.location;
            target = null;
            return loc;
        }
    }
    // TODO make this more advanced.
    private void updateTarget(MapLocation myLocation, RobotInfo robot) {
        // Pick the closest
            if (!willShootFriends(myLocation, robot.location))
                if (target == null
                        || (getRobotGroup(robot) != RobotGroup.ENEMYECONOMIC
                            && getRobotGroup(target) == RobotGroup.ENEMYECONOMIC)
                        || robot.location.distanceSquaredTo(myLocation) < target.location.distanceSquaredTo(myLocation))
                    target = robot;
    }

    private TreeInfo closestTree = null;
    public MapLocation getTreeLocation() {
        if (closestTree == null)
            return null;
        else {
            // Release the target
            MapLocation loc = closestTree.location;
            closestTree = null;
            return loc;
        }
    }
    private void updateClosestTree(MapLocation myLocation, TreeInfo tree) {
        if (closestTree == null
                ||
                tree.location.distanceSquaredTo(myLocation)
                        < closestTree.location.distanceSquaredTo(myLocation))
            closestTree = tree;
    }

    public double[] getMyGradient(MapLocation myLocation, RobotInfo robot) {

        double[] gradient = new double[2];

        RobotGroup rg = getRobotGroup(robot);

        switch (myType) {
            case ARCHON:
            case GARDENER:
                // Avoid enemy military and raiders.
                if (rg == RobotGroup.ENEMYMILITARY
                        || rg == RobotGroup.ENEMYRAIDER)
                    gradient = SjxMath.elementwiseSum(
                            fleeEnemyGradient(myLocation, robot),
                            gradient, false);
                break;
            case LUMBERJACK:
                // TODO add a gamestate where lumberjacks exhibit minimal friendlydonut and
                // TODO     charge the enemy.
                // Melee kite enemy ranged military. Kiting is inverted for melee units.
                // Flee when out of melee range, attack when inside.
                // Include scouts.
                if (rg == RobotGroup.ENEMYMILITARY || robot.type == RobotType.SCOUT)
                    gradient = SjxMath.elementwiseSum(
                            enemyKitingDonutGradient(myLocation, robot),
                            gradient, false);
                // Target enemy economic.
                if (rg == RobotGroup.ENEMYECONOMIC)
                    gradient = SjxMath.elementwiseSum(
                            chargeEnemyGradient(myLocation, robot),
                            gradient, false);
                // Avoid friendly raiders.
                if (rg == RobotGroup.FRIENDLYRAIDER)
                    gradient = SjxMath.elementwiseSum(
                            avoidFriendlyRaiderGradient(myLocation, robot),
                            gradient, true);
                break;
            case SCOUT:
                // Avoid enemy military.
                if (rg == RobotGroup.ENEMYMILITARY)
                    gradient = SjxMath.elementwiseSum(
                            fleeEnemyGradient(myLocation, robot),
                            gradient, false);
                // Target enemy economic.
                if (rg == RobotGroup.ENEMYECONOMIC)
                    gradient = SjxMath.elementwiseSum(
                            chargeEnemyGradient(myLocation, robot),
                            gradient, false);
                // Avoid friendly raiders.
                if (rg == RobotGroup.FRIENDLYRAIDER)
                    gradient = SjxMath.elementwiseSum(
                            avoidFriendlyRaiderGradient(myLocation, robot),
                            gradient, true);
                break;
            case SOLDIER:
            case TANK:
                if (robot.team != myTeam) {
                    // Kite all enemies.

                    gradient = SjxMath.elementwiseSum(
                            enemyKitingDonutGradient(myLocation, robot),
                            gradient, false);

                    // Update firing target.
                    updateTarget(myLocation, robot);
                }
                else
                    if (rg == RobotGroup.FRIENDLYMILITARY) {
                        gradient = SjxMath.elementwiseSum(
                                friendlyDonutGradient(myLocation, robot),
                                gradient, false);
                    }
                    else
                        // Just add to shooting gradients.
                        addToShootingGradients(myLocation, robot);
                break;
            default:
                gradient = new double[2];
        }

        botscounted++;

        // Add the macro elements.
        return gradient;

    }

    // normalizing counters
    private int friendlyMilitaryCounted = 0;
    private int enemyMilitaryCounted = 0;
    private int botscounted = 0;
    public void resetBotsCounted() {
        botscounted = 0;

        friendlyMilitaryCounted = 0;
        enemyMilitaryCounted = 0;
        lastEnemyGradient = new double[2];
        lastFriendlyGradient = new double[2];
    }
    public int getBotsCounted() {
        return botscounted;
    }

    public double[] getMyGradient(MapLocation myLocation, RobotInfo[] robots) {

        // Reset normalizing counter.
        resetBotsCounted();

        double[] gradient = new double[2];

        if (robots.length == 0) {
            System.out.println("Not sensing any bots.");
            return SjxMath.elementwiseSum(gradient, macroGradient(myLocation), false);
        }

        for (RobotInfo robot : robots)
            gradient = SjxMath.elementwiseSum(getMyGradient(myLocation, robot), gradient, false);

        gradient[0] /= (double)getBotsCounted();
        gradient[1] /= (double)getBotsCounted();

        System.out.println("My bot gradient is: " + Arrays.toString(gradient));
        if (Double.isNaN(gradient[0]))
            System.out.println("Why you NaN?");

        // Add the macro level gradients.
        return SjxMath.elementwiseSum(gradient, macroGradient(myLocation), false);
    }

    public double[] macroGradient(MapLocation myLocation) {
        // TODO read target broadcast channels
        // TODO convert coords to location
        // TODO calculate the attraction/repulsion of each macro target given robot type.
        // TODO include treees?

        double[] gradient = new double[2];

        switch (myType) {
            case SOLDIER:
                gradient = SjxMath.elementwiseSum(gradient, avoidTreesGradient(myLocation), false);
                System.out.println("My tree gradient is: " + Arrays.toString(gradient));
            case TANK:
                // IF there's someone to shoot, ignore this.
                //if (target != null) break;
                try {
                    MapLocation archonLoc = RobotPlayer.findClosestArchon();
                    if (archonLoc != null) {
                        // Large and small, stacked.
                        double[] archonGradient = SjxMath.gaussianDerivative(myLocation, archonLoc,
                                        75, macroEconomicTargetScale);
//                        archonGradient = SjxMath.elementwiseSum(archonGradient,
//                                SjxMath.gaussianDerivative(myLocation, archonLoc,
//                                        20, macroEconomicTargetScale/2.),
//                                false
//                        );
//                        archonGradient[0] /= 2.;
//                        archonGradient[1] /= 2.;
                        System.out.println("My archon gradient is: " + Arrays.toString(archonGradient));
                        gradient = SjxMath.elementwiseSum(archonGradient, gradient, false);
                    }
                }
                catch (GameActionException e) {
                    System.out.println(e.getMessage());
                }
                break;
            case LUMBERJACK:
                gradient = SjxMath.elementwiseSum(gradient, seekTreesGradient(myLocation), false);
                System.out.println("My tree gradient is: " + Arrays.toString(gradient));
                try {
                    MapLocation archonLoc = RobotPlayer.findClosestArchon();
                    if (archonLoc != null) {
                        double[] archonGradient = SjxMath.gaussianDerivative(myLocation, archonLoc,
                                75, macroEconomicTargetScale);
                        System.out.println("My archon gradient is: " + Arrays.toString(archonGradient));
                        gradient = SjxMath.elementwiseSum(archonGradient, gradient, false);
                    }
                }
                catch (GameActionException e) {
                    System.out.println(e.getMessage());
                }
                break;
        }

        System.out.println("My total gradient is " + Arrays.toString(gradient));
        return gradient;
    }

    private double[] lastFriendlyGradient = new double[2];
    private double[] lastEnemyGradient = new double[2];
    private boolean willShootFriends(MapLocation myLocation, MapLocation targetLocation) {

        // Verify that the enemy gradient opposes the friendly gradient by at least 45 degrees.
        //  This *should* prevent friendly fire?

        double mag = SjxMath.vectorMagnitude(lastFriendlyGradient)
                * SjxMath.vectorMagnitude(lastEnemyGradient);
        double r = Math.acos(SjxMath.dotProduct(lastFriendlyGradient, lastEnemyGradient)/mag);
        return (r > (11.*Math.PI)/6.) || (r < Math.PI/3.);
    }
    private void updateLastFriendlyGradient(double[] gradient) {

        friendlyMilitaryCounted++;
        lastFriendlyGradient = updateLastGradient(lastFriendlyGradient, gradient, friendlyMilitaryCounted);

    }
    private void updateLastEnemyGradient(double[] gradient) {

        enemyMilitaryCounted++;
        lastEnemyGradient = updateLastGradient(lastEnemyGradient, gradient, enemyMilitaryCounted);
    }
    private double[] updateLastGradient(double[] currentAverage, double[] newEntry, double count) {

        // Averaging unnormalized vectors means that closer units count less. Which is actually what
        //  we want, because the closer they are, the smaller chance they have actually being hit.

        if (count == 0) return new double[2];

        double normexisting = (count-1.)/count;

        currentAverage[0] *= normexisting;
        currentAverage[1] *= normexisting;

        newEntry[0] /= count;
        newEntry[1] /= count;

        return SjxMath.elementwiseSum(currentAverage, newEntry, false);
    }
    private void updateShootGradients(double[] gradient, RobotInfo robot) {
        if (robot.team == myTeam)
            updateLastFriendlyGradient(gradient);
        else
            updateLastEnemyGradient(gradient);
    }


    public double[] friendlyDonutGradient(
            MapLocation myLocation, RobotInfo robot) {

        // Outside
        double[] outside = SjxMath.gaussianDerivative(
                myLocation, robot.location, range, friendlyMilitaryAttractionScale);
        // Inside
        double[] inside = SjxMath.gaussianDerivative(
                myLocation, robot.location, myType.bodyRadius*4., friendlyMilitaryRepulsionScale);

        updateShootGradients(outside, robot);

        // Doughnut
        return SjxMath.elementwiseSum(outside, inside, true);
    }

    public double[] avoidFriendlyRaiderGradient(MapLocation myLocation, RobotInfo robot) {

        double[] gradient = SjxMath.gaussianDerivative(
                        myLocation, robot.location, range, -friendlyRaiderRepulsionScale);

        updateShootGradients(gradient, robot);

        return gradient;
    }


    // double[][] is <outside, inside>
    private HashMap<RobotType, double[]> kitingDeviations = createKitingDeviations();
    private HashMap<RobotType, double[]> createKitingDeviations()
    {
        HashMap<RobotType, double[]> myMap = new HashMap<RobotType, double[]>();

        myMap.put(RobotType.SCOUT, new double[] {range/2., myType.bodyRadius});
        myMap.put(RobotType.LUMBERJACK, new double[] {range, RobotType.LUMBERJACK.strideRadius*2.});
        // If you are a lumber jack, reverse kiting is in place, so the inside circle should be a
        //  couple steps out of melee range. Override for scouts.
        if (myType == RobotType.LUMBERJACK) {
            myMap.put(RobotType.SOLDIER, new double[] {range, RobotType.LUMBERJACK.strideRadius*2.});
            myMap.put(RobotType.TANK, new double[] {range, RobotType.LUMBERJACK.strideRadius*2.});
            myMap.put(RobotType.SCOUT, new double[] {range/2., RobotType.LUMBERJACK.strideRadius*2.});
        }
        else {
            myMap.put(RobotType.SOLDIER, new double[] {range, RobotType.SOLDIER.bodyRadius*2.});
            myMap.put(RobotType.TANK, new double[] {range, RobotType.TANK.bodyRadius*2.});
        }
        myMap.put(RobotType.ARCHON, new double[] {range, RobotType.ARCHON.strideRadius*1.5});
        myMap.put(RobotType.GARDENER, new double[] {range, RobotType.GARDENER.strideRadius*1.5});
        return myMap;
    }
    public double[] enemyKitingDonutGradient(MapLocation myLocation, RobotInfo robot) {

        double outsideStandardDeviation = kitingDeviations.get(robot.type)[0];
        double insideStandardDeviation = kitingDeviations.get(robot.type)[1];

        double attractionScale;
        double repulsionScale = enemyMilitaryDangerScale;
        switch (robot.type) {
            case ARCHON:
            case GARDENER:
                attractionScale = enemyEconomicAttractionScale;
                break;
            default:
                attractionScale = enemyMilitaryDutyScale;
        }
        // Lumberjacks should exhibit reverse kiting behavior, where they flee unless very close.
        if (myType == RobotType.LUMBERJACK) {
            attractionScale *= -1.;
            repulsionScale *= -1.;
        }


        double[] outside = SjxMath.gaussianDerivative(
                myLocation, robot.location, outsideStandardDeviation, attractionScale);
        double[] inside = SjxMath.gaussianDerivative(
                myLocation, robot.location, insideStandardDeviation, repulsionScale);

        updateShootGradients(outside, robot);

        return SjxMath.elementwiseSum(outside, inside, true);

    }


    public double[] fleeEnemyGradient(MapLocation myLocation, RobotInfo robot) {

        double[] gradient = SjxMath.gaussianDerivative(
                            myLocation, robot.location, range, -enemyMilitaryDangerScale);

        updateShootGradients(gradient, robot);

        return gradient;

    }

    public double[] chargeEnemyGradient(MapLocation myLocation, RobotInfo robot) {

        double[] gradient = SjxMath.gaussianDerivative(
                            myLocation, robot.location, range, enemyMilitaryDutyScale);

        updateShootGradients(gradient, robot);

        return gradient;
    }

    public void addToShootingGradients(MapLocation myLocation, RobotInfo robot) {

        double[] gradient = SjxMath.gaussianDerivative(
                myLocation, robot.location, range/1.5, 6.);

        updateShootGradients(gradient, robot);
    }

    private double[] treeGradient(MapLocation myLocation, double scale) {

        // Sense all nearby trees.
        TreeInfo[] trees;
        if (myType != RobotType.LUMBERJACK)
            trees = me.senseNearbyTrees((float)(myType.strideRadius*1.5 + myType.bodyRadius*1.5));
        else
            trees = me.senseNearbyTrees();

        // Init the gradient vector over which we'll sum..
        double[] gradient = new double[] {0., 0.};

        // Iterate nearby trees
        for (TreeInfo tree : trees) {

            double closeStandardDeviation = tree.radius + myType.bodyRadius;

            double _scale = scale;

            if (myType == RobotType.LUMBERJACK)
                if (tree.getContainedRobot() != null)
                    _scale = scale*3.;
                else if (tree.getTeam() == myTeam)
                    _scale = scale*-0.5;
                else if (tree.getContainedBullets() > 0)
                    _scale = scale*1.3;

            // Each tree gets a gaussian with standard deviation a function of its radius.
            gradient = SjxMath.elementwiseSum(
                            gradient,
                            SjxMath.gaussianDerivative(myLocation, tree.location,
                                    closeStandardDeviation, _scale),
                            false
            );

            // Add a larger gradient around the tree for lumberjacks.
            if (myType == RobotType.LUMBERJACK)
                gradient = SjxMath.elementwiseSum(
                        gradient,
                        SjxMath.gaussianDerivative(myLocation, tree.location,
                                range, _scale),
                        false
                );

            updateClosestTree(myLocation, tree);

        }

        // Normalize gradient over all nearby trees.
        if (trees.length > 0) {
            gradient[0] /= trees.length;
            gradient[1] /= trees.length;
        }

        return gradient;
    }
    public double[] avoidTreesGradient(MapLocation myLocation) {
        return treeGradient(myLocation, -1.);
    }
    public double[] seekTreesGradient(MapLocation myLocation) {
        return treeGradient(myLocation, 1.);
    }

    // TODO bullet dodging gradients
    // TODO cast a gaussian out in front of the tracked bullet at speed.
    // TODO cast a "shadow" behind nearby trees with the same covariance as the bullet.
    // TODO the shadow should be multiplied by the bullet gaussians.
    // TODO don't normalize bullets the more the deader.
}
