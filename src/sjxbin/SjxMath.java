package sjxbin;
import battlecode.common.*;

import java.util.Arrays;
import java.util.Random;

/**
 * A set of mathematical functions for use in the SgtAjax battlecode bot.
 */
public strictfp class SjxMath {

    public static String csvFrom2dArray(double[][] array) {
        String csv = "";
        for (double[] row : array) {
            csv += SjxANN.trimEnds(Arrays.toString(row)) + '\n';
        }
        return csv;
    }

    // With max slope at origin, y bound in (-1,1)
    public static double sigmoid(double x) {
        return 2./(1. + Math.exp(-x)) -1.;
    }
    public static double sigmoidDerivative(double x) {
        return 2.*(Math.exp(x)/Math.pow(Math.exp(x) + 1., 2));
    }

    public static double dotProduct(double[] x1, double[] x2) {

        double sum = 0;

        for (int i = 0; i < x1.length; ++i) {
            sum += x1[i] * x2[i];
        }

        return sum;
    }

    public static double[] elementwiseSum(double[] x1, double[] x2, boolean subtract) {

        double[] sumVector = new double[2];

        if (x2.length != 2 || x1.length != 2)
            throw new RuntimeException("Must be of length 2.");

//        double sign = 1.;
//        if (negate) {
//            sign *= -1.;
//        }
//
//        for (int i = 0; i < x1.length; i++) {
//            sumVector[i] = x1[i] + x2[i]*sign;
//        }
        if (subtract) {
            sumVector[0] = x1[0] - x2[0];
            sumVector[1] = x1[1] - x2[1];
        }
        else {
            sumVector[0] = x1[0] + x2[0];
            sumVector[1] = x1[1] + x2[1];
        }

        return sumVector;
    }

    public static double[][] elementwiseSum(double[][] x1, double[][] x2, boolean negate) {
        double[][] sumVector = new double[x1.length][x1[0].length];

        double sign = 1.;
        if (negate) {
            sign *= -1.;
        }

        for (int i = 0; i < x1.length; i++)
            for (int j = 0; j < x1[0].length; j++)
                sumVector[i][j] = x1[i][j] + x2[i][j]*sign;

        return sumVector;
    }

    public static double euclideanSquaredDistance(double[] x1, double[] x2) {

        double totalDif = 0;

        for (int i = 0; i < x1.length; ++i) {
            totalDif += Math.pow(x1[i] - x2[i], 2.);
        }

        return totalDif;
    }

    public static double vectorMagnitude(double[] x1) {
        double mag = 0;

        for (int i = 0; i < x1.length; ++i) {
            mag += Math.pow(x1[i], 2.);
        }

        return Math.sqrt(mag);
    }

    /**
     * Returns the value at x for a given gaussian. The variance is defined isotropically, i.e. the same for all axes.
     */
    public static double gaussian(double[] x, double[] mean, double standardDeviation, double scale) {

        double esd = euclideanSquaredDistance(x, mean);

        double variance = Math.pow(standardDeviation, 2.);

        double exponent = -esd/(2.*variance);

        double unscaled = Math.exp(exponent);

        double gauss = scale * unscaled;

        return gauss;
    }

    public static double sampleGaussian(Random r, double mean, double standardDeviation) {
        double mySample = r.nextGaussian()*standardDeviation+mean;

        return mySample;
    }

    // Default to normalization if no scale is passed.
    public static double gaussian(double[] x, double[] mean, double standardDeviation) {
        return gaussian(x, mean, standardDeviation, gaussianNormConstant(standardDeviation, x.length));
    }

    // Returns the normalizing constant for a gaussian.
    public static double gaussianNormConstant(double standardDeviation, int dims) {
        return 1./(Math.pow(2.*Math.PI, dims/2)*standardDeviation);
    }

    public static double[] gaussianDerivative(double[] x, double[] mean, double standardDeviation, double scale) {

        double gauss = gaussian(x, mean, standardDeviation, scale);
        double variance = Math.pow(standardDeviation, 2.);

        double[] partials = new double[x.length];

        for (int i = 0; i < x.length; ++i) {
            partials[i] = (-(x[i] - mean[i])/variance)*gauss;
        }

        return partials;
    }

    public static double[] gaussianDerivative(MapLocation me, MapLocation other,
                                              double standardDeviation, double scale) {
        double[] xarray = new double[] {me.x, me.y};
        double[] meanarray = new double[] {other.x, other.y};

        return gaussianDerivative(xarray, meanarray, standardDeviation, scale);
    }

    // See https://www.desmos.com/calculator/crogrhzvyq
    public static double[] doughnutDerivative(MapLocation me, MapLocation other,
                                              double innerStandardDeviation, double innerScale,
                                              double outerStandardDeviation, double outerScale,
                                              boolean outerMinusInner) {

        double[] innerDerivative = gaussianDerivative(me, other, innerStandardDeviation, innerScale);
        double[] outerDerivative = gaussianDerivative(me, other, outerStandardDeviation, outerScale);

        // The collective derivative is just a difference, as the original function is simply a difference.
        if (outerMinusInner) {
            return elementwiseSum(outerDerivative, innerDerivative, true);
        }
        else {
            return elementwiseSum(innerDerivative, outerDerivative, true);
        }
    }
}