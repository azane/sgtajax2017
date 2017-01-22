package sjxbin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * Created by azane on 1/19/17.
 * This class is a basic neural network class designed to be stored as weights in low bit integers.
 * Implemented, this means that the weights go through a sigmoid before their layer is calculated.
 *  This makes the derivative small when weights get around +/- 5, keeping them in check so that,
 *   a set number of bits can consistently be converted to a decimal.
 *  This requires that the output be scaled appropriately. For example, bot movement prediction should
 *   not be in terms of the absolute coordinates, but the relative coordinates, as resolution is limited when
 *   +/- 5 needs to be scaled from 256.
 *  If using a bayesian sampler setting, the weight prior method of this class should be used to multiply
 *   with the likelihood.
 */
public strictfp class SjxANN {

    private ArrayList<Matrix> weights = new ArrayList<Matrix>();
    private int[] shape;
    private int numWeights = 0;
    private final static double ABS_WEIGHT_LIMIT = 1.;
    private boolean addBias = true;
    private double bias = 1;

    // shape should have ints denoting the number of neurons on each layer.
    public SjxANN(int[] shape, boolean addBias) {
        this.shape = shape.clone();
        this.addBias = addBias;

        if (this.addBias)
            // Add a bias input that will always = the same nonzero thing.
            this.shape[0] += 1;

        // Iterate to the second to last layer, as that has no weights stemming from it.
        for (int i = 0; i < (this.shape.length - 1); i++) {
            // Increment the number of weights.
            numWeights += this.shape[i]*this.shape[i+1];
            // Init to random between -1 and 1.
            weights.add(Matrix.random(this.shape[i], this.shape[i+1]).timesInPlace(2.*ABS_WEIGHT_LIMIT).plusInPlace(-ABS_WEIGHT_LIMIT/2.));
        }
    }

    public ArrayList<Matrix> runBatch(Matrix input) {
        // "input" should be [number of samples]x[input first layer]

        ArrayList<Matrix> layerPreActivations = new ArrayList<Matrix>();

        if (addBias) {
            input = input.appendColumn(bias);
        }

        // Store the input as the first "activation".
        layerPreActivations.add(input);
        for (Matrix weight : weights) {
            // Sigmoid the last layer activations, multiply by the weight matrix, then transpose back
            //  to a [samples]x[layer size] matrix.
            int i = layerPreActivations.size()-1;
            // Don't sig the inputs.
            if (i == 0)
                layerPreActivations.add(
                        layerPreActivations.get(i)
                        .times(weight)
                );
            else
                layerPreActivations.add(
                        layerPreActivations.get(i).sigmoid()
                                .times(weight)
                );
        }

        // This will be a layer by layer list of [number of samples]x[layer size]
        return layerPreActivations;
    }

    // Calculates the likelihood of the weights from the prior distribution.
    private static double weightPrior(double[] flatWeightArray) {
        // Return the likelihood of the weight array given zero mean and stdev of 5. This corresponds to
        //  to the place where the sigmoid is practically zero.
        double[] mean = new double[flatWeightArray.length];
        // Doesn't have to be normalized, as we're using this for sampling.
        double likelihood = SjxMath.gaussian(flatWeightArray, mean, ABS_WEIGHT_LIMIT, 10);
        return likelihood;
    }

    public static double weightPrior(ArrayList<Matrix> matrixArrayList) {

        int size = 0;
        for (Matrix matrix : matrixArrayList) {
            size += matrix.numElements();
        }

        double[] flatWeights = new double[size];
        int index = 0;
        for (Matrix matrix : matrixArrayList) {
            double[] flatWeight = matrix.flatten();
            for (int i = 0; i < flatWeight.length; i++) {
                flatWeights[index++] = flatWeight[i];
            }
        }

        return weightPrior(flatWeights);
    }

    public Matrix runBatchOutOnly(Matrix inputs) {
        // Just get the outputs, then sigmoid.
        return runBatch(inputs).get(shape.length-1).sigmoid();
    }
    public double meanSquaredError(Matrix inputs, Matrix outputs) {

        try {
            Matrix actualOutput = runBatchOutOnly(inputs);
            double normalizingConstant = .5*(1./inputs.numRows());
            Matrix difference = actualOutput.minus(outputs);
            double retval = difference.powInPlace(2)
                        .timesInPlace(normalizingConstant).sumOver('M').sumOver('N').getData()[0][0];
            return retval;
        }
        catch (Exception e) {
            return 0;
        }
    }

    // Returns the average gradient of the weights. This will be 0 if assess is false.
    public double trainBackprop(Matrix inputs, Matrix outputs, double scale, boolean assess, int iters)
            throws RuntimeException {

        // both inputs and outputs should be [number of samples]x[dimension of data point]
        if (inputs.numRows() != outputs.numRows()) {
            throw new RuntimeException("inputs must match outputs 1:1.");
        }
        // L: number of layers.
        // S: number of samples.
        // N: number of neurons.
        // Nin: number of input neurons.
        // Nout: number of output neurons.

        double averageGradient = 0;

        for (int k = 0; k < iters; k++) {

            ArrayList<Matrix> layerPreActivations = runBatch(inputs);  // [L,S,N]
            int numLayers = layerPreActivations.size();

            // Get output layer error gradient wrt output layer PreActivations.
            // (actual - desired) * (sigmoid derivative of preactivation).
            // NOTE (actual - desired) is the derivative of the squared error function.
            Matrix activationErrorGradient = layerPreActivations.get(numLayers - 1).sigmoid()
                    .minus(outputs) // [S, Nout] - [S, Nout]
                    .hadamardProduct(layerPreActivations.get(numLayers - 1).sigmoidDerivative()); // * [S, Nout]

            // Backpropogate the error, applying to the weight matrix after it's gradient is calculated.
            // numLayers -2: one for index, another because we already did the output above.
            for (int i = numLayers - 2; i >= 0; i--) {

                // The weight gradient is then the activation passing through the weight, times the error
                //  gradient of the preactivation of the output layer. In matrix terms, it's the input activation
                //  vector times the output gradient vector.
                // Batch expand and sum. This adds up all the gradients for each sample,
                //  then applies negatively to the weight (descending the error function).
                Matrix weightGradient = layerPreActivations.get(i).sigmoid() // [S, Nin]
                        // ~~~ [S, Nout] = [Nin, Nout]
                        .batchExpandWithVectorAndSumRowByRow(activationErrorGradient)
                        .timesInPlace(scale);
                weights.set(i, weights.get(i).minus(weightGradient)); // [Nin, Nout]

                if (assess) {
                    averageGradient += weightGradient.sumOver('M').sumOver('N')
                            .getData()[0][0] / numWeights;
                }

                // Transpose the weights, backprop the error gradient, elementwise multiply by the sigmoid
                //  derivative of the preactivation of the layer below.
                // Note that weights.get(i) retrieves the weight matrix going out of the layer at index i,
                //  and into the layer at i + 1 (which is the layer of the last activationErrorGradient).
                activationErrorGradient = weights.get(i) // [Nin, Nout]
                        .timesByRowVectors(activationErrorGradient) // * [S, Nout] = [S, Nin]
                        .hadamardProduct(layerPreActivations.get(i).sigmoidDerivative()); // [S, Nin] h [S, Nin]
            }
        }
        return averageGradient;
    }

    private double likelihood(Matrix inputs, Matrix outputs) {
        // This is effectively a gaussian with set variance, which is fine as the variance
        //  would only need to change if the outputs are not normalized to sigmoid output.
        // We assume that they are normalized to sigmoid output.
        // The final likelihood is the likelihood given the data, and the likelihood from the prior.
        return Math.exp(-1. * meanSquaredError(inputs, outputs)) * weightPrior(weights);
        //return weightPrior(weights);
        //return Math.exp(-1. * meanSquaredError(inputs, outputs));
    }

    public double trainMetropolisHastings(Matrix inputs, Matrix outputs, double samplerDeviation, int iters) {

        try {

            // Return the percentage of accepted moves.
            double accepted = 0;
            double rejected = 0;

            // Prep random object outside of loops.
            Random r = new Random();
            // Get the likelihood for the starting state.
            double startingLikelihood = likelihood(inputs, outputs);


            for (int k = 0; k < iters; k++) {

                // Declarations for loop.
                ArrayList<Matrix> proposedWeights = new ArrayList<Matrix>();
                double[][] proposedArrayWeight;

                for (Matrix weight : weights) {

                    // Get a new sample of the weights.
                    proposedArrayWeight = new double[weight.numRows()][weight.numColumns()];
                    for (int i = 0; i < proposedArrayWeight.length; i++)
                        for (int j = 0; j < proposedArrayWeight[0].length; j++) {
                            // Assume the same deviation for all axes of the sampling distribution.
//                            double weightMean = weight.getData(i, j);
//                            double w = SjxMath.sampleGaussian(r, weightMean, samplerDeviation);
//                            proposedArrayWeight[i][j] = w;
                            proposedArrayWeight[i][j] = weight.getData(i, j) + (Math.random() - .5)*samplerDeviation;

                        }

                    proposedWeights.add(new Matrix(proposedArrayWeight));
                }

                ArrayList<Matrix> heldWeights = weights;
                weights = proposedWeights;

                // Calculate the likelihood of these potential weights.
                double proposedLikelihood = likelihood(inputs, outputs);

                // Accept on a metropolis hastings basis.
                double moveLikelihood = proposedLikelihood / startingLikelihood;
                if (moveLikelihood > 1.) {
                    // We've already put the proposed weights into the weights, so just update likelihood.
                    startingLikelihood = proposedLikelihood;
                    accepted++;
                } else {
                    double rand = r.nextDouble();
                    if (rand < moveLikelihood) {
                        startingLikelihood = proposedLikelihood;
                        accepted++;
                    } else {
                        // Reset weights, leave startingLikelihood.
                        weights = heldWeights;
                        rejected++;
                    }
                }
            }
            return (accepted) / (accepted + rejected);
        }
        catch (Exception e) {
            return 0;
        }
    }

    private static String trimEnds(String s) {
        return s.substring(1, s.length()-1);
    }

    public static void TestSjxANN() {

        testSanity();

        String networkOutput = "";
        String acceptanceRate = "";
        String trace = "";
        String testMSQ = "";
        String trainMSQ = "";
        String averageGradient = "";
        String testData = "";
        String priorCurve = "";
        try {
            // This test has the network approximate a simple sin function.

            int[] shape = new int[]{1, 2, 2, 2, 1};
            SjxANN ann = new SjxANN(shape, true);

            int sampleSize = 10;
            // Gen some data.
            ArrayList<double[][]> data = generateData(shape, sampleSize, 0);
            Matrix minput = new Matrix(data.get(0));
            Matrix moutput = new Matrix(data.get(1));

            // Generate the prior curve.
            ArrayList<double[][]> pcurve = generateData(new int[] {1,1}, 100, 1);
            Matrix pcurvex = new Matrix(pcurve.get(0));
            Matrix pcurvey = new Matrix(pcurve.get(1));
            priorCurve += trimEnds(Arrays.toString(pcurvex.flatten()));
            priorCurve += '\n';
            priorCurve += trimEnds(Arrays.toString(pcurvey.flatten()));

            int trainingIterations = 50;
            double samplerDeviation = ABS_WEIGHT_LIMIT/50.;
            for (int i = 0; i < trainingIterations; i++) {
                averageGradient += ann.trainBackprop(minput, moutput, 1.5, true, 10) + ",";
//                double acceptanceRateD = ann.trainMetropolisHastings(minput, moutput, samplerDeviation, 1);
//                if (acceptanceRateD > 0.5) samplerDeviation += ABS_WEIGHT_LIMIT/300.;
//                else if (acceptanceRateD < 0.5) samplerDeviation -= ABS_WEIGHT_LIMIT/300.;
//                acceptanceRate += acceptanceRateD + ",";

                String thisTrace = Arrays.toString(ann.weights.get(1).flatten());
                trace += trimEnds(thisTrace) + '\n';

                // Generate some assessment data.
//                ArrayList<double[][]> tdata = generateData(shape, 15, 0);
//                Matrix tminput = new Matrix(tdata.get(0));
//                Matrix tmoutput = new Matrix(tdata.get(1));
//
//                testMSQ += ann.meanSquaredError(tminput, tmoutput) + ",";
                trainMSQ += ann.meanSquaredError(minput, moutput) + ",";

                // Swap in data.
//                data = generateData(shape, sampleSize);
//                minput = new Matrix(data.get(0));
//                moutput = new Matrix(data.get(1));

                System.out.println();

                if (i == trainingIterations - 1) {
                    networkOutput += trimEnds(Arrays.toString(minput.flatten()));
                    networkOutput += '\n';
                    networkOutput += trimEnds(Arrays.toString(ann.runBatchOutOnly(minput).flatten()));
//                    testData += trimEnds(Arrays.toString(tminput.flatten()));
//                    testData += '\n';
//                    testData += trimEnds(Arrays.toString(tmoutput.flatten()));
                    System.out.println();
                }
            }
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }

        System.out.println(networkOutput);
    }

    private static ArrayList<double[][]> generateData(int[] shape, int sampleSize, int functionIndex) {
        double[][] input = new double[sampleSize][shape[0]];
        double[][] output = new double[sampleSize][shape[shape.length-1]];

        for (int i = 0; i < input.length; i++) {
            for (int j = 0; j < shape[0]; j++) {
                input[i][j] = Math.random()*3. - 1.5;
            }
            output[i] = testFunctions(input[i], functionIndex);
        }

        ArrayList<double[][]> data = new ArrayList<double[][]>();
        data.add(input);
        data.add(output);
        return data;
    }

    private static double[] testFunctions(double[] x, int functionIndex) {
        switch (functionIndex) {
            case 0:
                return new double[] {SjxMath.sigmoid(Math.sin(1.5*x[0]))};
            case 1:
                return new double[] {weightPrior(x)};
            default:
                throw new IndexOutOfBoundsException("No function under index " + functionIndex);
        }
    }

    public static boolean testSanity() {

        // This tests the function of the network

        double[][] layer1 = new double[][] {
                new double[] {-.4, .3}
        };
        double[][] layer2 = new double[][] {
                new double[] {2.},
                new double[] {0.3}
        };

        SjxANN ann = new SjxANN(new int[] {1, 2, 1}, false);
        ann.weights = new ArrayList<Matrix>();
        ann.weights.add(new Matrix(layer1));
        ann.weights.add(new Matrix(layer2));

        double[][] inputs = new double[][] {
                new double[] {1.},
                new double[] {-2.},
                new double[] {-5.},
                new double[] {10.},
                new double[] {Math.E},
                new double[] {Math.PI}
        };

        Matrix actualOut = ann.runBatchOutOnly(new Matrix(inputs));

        boolean insane = false;
        for (int i = 0; i < inputs.length; i++) {
            double out = SjxMath.sigmoid(
                SjxMath.sigmoid(inputs[i][0] * layer1[0][0]) * layer2[0][0] +
                SjxMath.sigmoid(inputs[i][0] * layer1[0][1]) * layer2[1][0]
            );
            if (out != actualOut.getData()[i][0]) {
                insane = true;
            }
        }
        return insane;
    }
}
