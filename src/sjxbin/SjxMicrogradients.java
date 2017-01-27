package sjxbin;

import battlecode.common.*;
import lumber_jack_s.RobotPlayer;

import java.util.Arrays;
import java.util.HashMap;

/**
 * Created by azane on 1/18/17.
 */
public strictfp class SjxMicrogradients {

    //region inits
    // singleton
    public static SjxMicrogradients instance;
    public SjxMicrogradients() {
        instance = this;
        setScales();
    }

    private final RobotController me = RobotPlayer.rc;
    private final RobotType myType = me.getType();
    private final Team myTeam = me.getTeam();

    private MapLocation lastLocation = me.getLocation();

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
    //endregion

    //region parameters
    private final double senseRToStdev = 1.;
    private final double range = myType.sensorRadius*senseRToStdev;

    // These should all be positive! Negation is taken care of in the methods below,
    //  and differs by unit type sometimes.
    private double friendlyMilitaryAttractionScale = 4.;
    private double friendlyMilitaryRepulsionScale = 6.;
    private double friendlyRaiderRepulsionScale = 6.;
    // The scale at which you pursue normal duties related to the enemy military.
    private double enemyMilitaryDutyScale = 7.;
    // The scale at which you respond to danger related to enemy military.
    private double enemyMilitaryDangerScale = 6.;
    private double enemyEconomicAttractionScale = 6.;
    private double treeScale = 3.;
    private double treeToMacroRate = 0.2;

    private double macroEconomicTargetScale = 6.;
    private double macroMilitaryTargetScale = 6.;
    private double macroDefenseTargetScale = 6.;

    public void setScales() {
        // TODO set the scales given broadcasted information and robot type.

        switch (myType) {
            case TANK:
            case SOLDIER:
                treeScale = 0.0;
                break;
            case LUMBERJACK:
                treeScale = 1.5;
                friendlyRaiderRepulsionScale = 6.;
                enemyMilitaryDutyScale = 10.;
                // This scale is used to kite friendly economics. This should be less
                //  than our raider repulsion.
                friendlyMilitaryAttractionScale = 0.5;
                friendlyMilitaryRepulsionScale = 0.03;
                break;
            case SCOUT:
                break;

        }
    }
    //endregion

    //region targeting
    private RobotInfo target = null;
    public RobotInfo getShotLocation() {
        if (target == null)
            return null;
        else {
            // Release the target
            RobotInfo temp = target;
            target = null;
            return temp;
        }
    }
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
    public TreeInfo getTreeLocation() {
        if (closestTree == null)
            return null;
        else {
            // Release the target
            TreeInfo temp = closestTree;
            closestTree = null;
            return temp;
        }
    }
    private void updateClosestTree(MapLocation myLocation, TreeInfo tree) {
        if (
                    closestTree == null
                ||
                    ((closestTree.getContainedRobot() == null
                        && tree.getContainedRobot() != null)
                    &&
                        me.canChop(tree.getID()))
                ||
                    (tree.location.distanceSquaredTo(myLocation)
                        < closestTree.location.distanceSquaredTo(myLocation))
            )
                        //< closestTree.location.distanceSquaredTo(myLocation))
            closestTree = tree;
    }
    //endregion

    //region gradientprocessing
    private Matrix mavg = new Matrix(7, 2);
    private int mavgIndex = 0;
    private void insertGradient(double[] gradient) {
        Matrix m = new Matrix(gradient);
        mavg.assignRowInPlace(m, mavgIndex);

        mavgIndex++;
        if (mavgIndex >= mavg.numRows())
            mavgIndex = 0;
    }
    private double[] retrieveMavgGradient() {
        Matrix m = mavg.sumOver('M');

        return m.timesInPlace(1./mavg.numRows()).getData()[0];
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
                // Kite friendlies, but avoid each other more.
                if (rg == RobotGroup.FRIENDLYECONOMIC)
                    gradient = SjxMath.elementwiseSum(
                            friendlyDonutGradient(myLocation, robot),
                            gradient, false);

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
            case TANK:
                // Charge enemies.
                if (robot.team != myTeam) {
                    gradient = SjxMath.elementwiseSum(
                            chargeEnemyGradient(myLocation, robot),
                            gradient, false);
                    // Update firing target.
                    updateTarget(myLocation, robot);
                }
                else
                    // Only donut school other tanks.
                    if (robot.getType() == RobotType.TANK) {
                        gradient = SjxMath.elementwiseSum(
                                friendlyDonutGradient(myLocation, robot),
                                gradient, false);
                    }
                    else
                        // Manually add to shooting gradients.
                        addToShootingGradients(myLocation, robot);
            case SOLDIER:
                if (robot.team != myTeam) {
                    // Kite all enemies.
                    gradient = SjxMath.elementwiseSum(
                            enemyKitingDonutGradient(myLocation, robot),
                            gradient, false);

                    // Update firing target.
                    updateTarget(myLocation, robot);
                }
                else
                    // Donut school friendly raiders and military.
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

        // Increase tree sensitivity if we are stuck.
        updateTreeToMacroRate();

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
        gradient = SjxMath.elementwiseSum(gradient, macroGradient(myLocation), false);

        // Mavg it if haven't moved.
        if (!me.hasMoved())
            insertGradient(gradient);

//        if (myType == RobotType.SOLDIER || myType == RobotType.TANK) {
//            double[] mavggrad = retrieveMavgGradient();
//            if (mavggrad[0] < 0.07 || mavggrad[1] < 0.07)
//                if (treeScale > 0.05)
//                    treeScale -= 0.05;
//                else
//                    treeScale += 0.001;
//        }


        return gradient;

    }

    private void updateTreeToMacroRate() {
        // TODO disabled until we can move orthogonally toward the archon.
//        // If we haven't moved far, make the tree ratio larger.
//        if (lastLocation.isWithinDistance(me.getLocation(), (float)(myType.strideRadius/3.))
//                && treeToMacroRate < 5.) {
//            treeToMacroRate += 0.01;
//        }
//        // If we have moved, reduce it.
//        else
//        if (treeToMacroRate > 0.005)
//            treeToMacroRate -= 0.005;
//
//        System.out.println("My treeToMacroRate: " + treeToMacroRate);
//
//        lastLocation = me.getLocation();
    }

    public double[] macroGradient(MapLocation myLocation) {
        // TODO read target broadcast channels
        // TODO convert coords to location
        // TODO calculate the attraction/repulsion of each macro target given robot type.
        // TODO include treees?

        double[] gradient = new double[2];
        double[] treeAvoidGrad = new double[2];

        switch (myType) {
            case SOLDIER:
                treeAvoidGrad = avoidTreesGradient(myLocation);
                System.out.println("My tree gradient is: " + Arrays.toString(treeAvoidGrad));
            case TANK:
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

                        // Make sure the tree gradient is half of the archon gradient.
                        double treeMag = SjxMath.vectorMagnitude(treeAvoidGrad);
                        double archonMag = SjxMath.vectorMagnitude(archonGradient);
                        if (treeMag > 0.) {
                            treeAvoidGrad[0] = (treeAvoidGrad[0] / treeMag) * (archonMag / treeToMacroRate);
                            treeAvoidGrad[1] = (treeAvoidGrad[1] / treeMag) * (archonMag / treeToMacroRate);
                        }
                        else
                            System.out.println("My tree gradient is zero?");

                        gradient = SjxMath.elementwiseSum(treeAvoidGrad, gradient, false);
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
    //endregion

    //region friendlyfire
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
    //endregion

    //region gradientmethods
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
        myMap.put(RobotType.LUMBERJACK, new double[] {range, RobotType.LUMBERJACK.strideRadius*3.});
        // If you are a lumber jack, reverse kiting is in place, so the inside circle should be a
        //  couple steps out of melee range. Override for scouts.
        if (myType == RobotType.LUMBERJACK) {
            myMap.put(RobotType.SOLDIER, new double[] {range, RobotType.LUMBERJACK.strideRadius*2.});
            myMap.put(RobotType.TANK, new double[] {range, RobotType.LUMBERJACK.strideRadius*2.});
            myMap.put(RobotType.SCOUT, new double[] {range/2., RobotType.LUMBERJACK.strideRadius*2.});
        }
        else {
            myMap.put(RobotType.SOLDIER, new double[] {range, RobotType.SOLDIER.bodyRadius*3.});
            myMap.put(RobotType.TANK, new double[] {range, RobotType.TANK.bodyRadius*3.});
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
            trees = me.senseNearbyTrees((float)(myType.strideRadius*2. + myType.bodyRadius*2.));
        else
            trees = me.senseNearbyTrees();

        // Init the gradient vector over which we'll sum..
        double[] gradient = new double[] {0., 0.};

        // Iterate nearby trees
        for (TreeInfo tree : trees) {

            double closeStandardDeviation = (tree.radius + myType.bodyRadius);

            double _scale = scale;

            if (myType == RobotType.LUMBERJACK) {
                if (tree.getContainedRobot() != null)
                    _scale = scale * 4.;
                else if (tree.getTeam() == myTeam)
                    _scale = scale * -0.5;
                else if (tree.getContainedBullets() > 0)
                    _scale = scale * 1.3;

                // Be more attracted to low health trees.
                _scale *= tree.getMaxHealth()/(tree.getHealth() + (tree.getMaxHealth()/5.));
            }

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
        // Bouncing around trees isn't what we want. Instead, move orthogonally to the trees
        //  to move around them!
        // TODO disabled until we implement going orthogonal TOWARD other macro targets.
        double[] treeGrad = treeGradient(myLocation, -treeScale);

//        // Get the vector orthogonal to this one by swapping and negating one.
//        double[] orthoGrad = new double[treeGrad.length];
//        orthoGrad[0] = treeGrad[1];
//        orthoGrad[1] = treeGrad[0];
//
//        if (me.getID() % 2 == 0) {
//            orthoGrad[0] *= -1.;
//        }
//        else
//            orthoGrad[1] *= -1.;
//
//        return orthoGrad;
        return treeGrad;
    }
    public double[] seekTreesGradient(MapLocation myLocation) {
        return treeGradient(myLocation, treeScale);
    }

    // TODO bullet dodging gradients
    // TODO cast a gaussian out in front of the tracked bullet at speed.
    // TODO cast a "shadow" behind nearby trees with the same covariance as the bullet.
    // TODO the shadow should be multiplied by the bullet gaussians.
    // TODO don't normalize bullets the more the deader.
    //endregion
}
