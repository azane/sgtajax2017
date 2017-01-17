package sjxmath;

/**
 * A set of mathematical functions for use in the SgtAjax battlecode bot.
 */
public strictfp class SjxMath {

    public static double euclideanSquaredDistance(double[] x1, double[] x2) {

        double totalDif = 0;

        for (int i = 0; i < x1.length; ++i) {
            totalDif += Math.pow(x1[i] - x2[i], 2.);
        }

        return totalDif;
    }

    /**
     * Returns the value at x for a given gaussian. The variance is defined isotropically, i.e. the same for all axes.
     */
    public static double gaussian(double[] x, double[] mean, double standardDeviation, double scale) {

        double esd = euclideanSquaredDistance(x, mean);

        return scale * Math.exp(-esd/2*Math.pow(standardDeviation, 2.));
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
}