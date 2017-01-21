package sjxbin;

import scala.Tuple2;

import java.util.ArrayList;

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

    private ArrayList<Matrix> weights;
    private int[] shape;
    private int numWeights = 0;
    private double ABS_WEIGHT_LIMIT = 5;

    // shape should have ints denoting the number of neurons on each layer.
    public SjxANN(int[] shape) {
        this.shape = shape;

        // Iterate to the second to last layer, as that has no weights stemming from it.
        for (int i = 0; i < (this.shape.length - 1); i++) {
            // Increment the number of weights.
            numWeights += this.shape[i]*this.shape[i+1];
            // Init to random between -1 and 1.
            weights.add(Matrix.random(this.shape[i], this.shape[i+1]).timesInPlace(6).plusInPlace(-3));
        }
    }

    public ArrayList<Matrix> runBatch(Matrix input) {
        // "input" should be [number of samples]x[input first layer]

        ArrayList<Matrix> layerPreActivations = new ArrayList<Matrix>();
        // Store the input as the first "activation".
        layerPreActivations.add(input);
        for (Matrix weight : weights) {
            // Sigmoid the last layer activations, multiply by the weight matrix, then transpose back
            //  to a [samples]x[layer size] matrix.
            layerPreActivations.add(layerPreActivations.get(layerPreActivations.size()).sigmoid()
                    .times(weight).transpose());
        }

        // This will be a layer by layer list of [number of samples]x[layer size]
        return layerPreActivations;
    }

    // Calculates the likelihood of the weights from the prior distribution.
    private double weightPrior(double[] flatWeightArray) {
        // Return the likelihood of the weight array given zero mean and stdev of 5. This corresponds to
        //  to the place where the sigmoid is practically zero.
        return SjxMath.gaussian(flatWeightArray, new double[flatWeightArray.length], ABS_WEIGHT_LIMIT);
    }
    public double weightPrior(ArrayList<Matrix> matrixArrayList) {
        double summedLayerLikelihood = 0;
        for ( Matrix matrix : matrixArrayList) {
            // Increment by the normalized/averaged weight likelihood.
            summedLayerLikelihood += weightPrior(matrix.flatten())/matrix.numElements();
        }
        // Normalize/average by layer.
        return summedLayerLikelihood/matrixArrayList.size();
    }

    public Matrix runBatchOutOnly(Matrix inputs) {
        // Just get the outputs, then sigmoid.
        return runBatch(inputs).get(shape[shape.length-1]).sigmoid();
    }
    public double meanSquaredError(Matrix inputs, Matrix outputs) {
        return runBatchOutOnly(inputs).minus(outputs).powInPlace(2)
                .timesInPlace(.5*(1/inputs.numRows())).sumOver('M').sumOver('N').getData()[0][0];
    }

    // Returns the average gradient of the weights. This will be 0 if assess is false.
    public double train(Matrix inputs, Matrix outputs, double scale, boolean assess) throws RuntimeException {
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
            weights.get(i).minus(weightGradient); // [Nin, Nout]

            if (assess) {
                averageGradient += weightGradient.sumOver('M').sumOver('N')
                                    .getData()[0][0]/numWeights;
            }

            // Transpose the weights, backprop the error gradient, elementwise multiply by the sigmoid
            //  derivative of the preactivation of the layer below.
            // Note that weights.get(i) retrieves the weight matrix going out of the layer at index i,
            //  and into the layer at i + 1 (which is the layer of the last activationErrorGradient).
            activationErrorGradient = weights.get(i) // [Nin, Nout]
                    .timesByRowVectors(activationErrorGradient) // * [S, Nout] = [S, Nin]
                    .hadamardProduct(layerPreActivations.get(i).sigmoidDerivative()); // [S, Nin] h [S, Nin]
        }

        return averageGradient;
    }

    public void TestSjxANN() {

        // This test has a network approximate a simple sin function.

        int[] shape = new int[] {2, 5, 3, 1};
        SjxANN ann = new SjxANN(shape);

        int sampleSize = 50;
        // Gen some data.
        ArrayList<double[][]> data = generateData(shape, sampleSize);
        Matrix minput = new Matrix(data.get(0));
        Matrix moutput = new Matrix(data.get(1));

        int trainingIterations = 1000;
        for (int i = 0; i < trainingIterations; i++) {
            System.out.println("SjxANN average gradient: " + ann.train(minput, moutput, 1, true));

            // Generate some assessment data.
            ArrayList<double[][]> tdata = generateData(shape, 25);
            Matrix tminput = new Matrix(tdata.get(0));
            Matrix tmoutput = new Matrix(tdata.get(1));
            System.out.println("SjxANN MSQ:" + ann.meanSquaredError(tminput, tmoutput));

            System.out.println();
        }
    }

    private ArrayList<double[][]> generateData(int[] shape, int sampleSize) {
        // Generate some data.
        double[][] input = new double[sampleSize][shape[0]];
        double[][] output = new double[sampleSize][shape[shape.length-1]];

        for (int i = 0; i < input.length; i++) {
            for (int j = 0; j < shape[0]; j++) {
                input[i][j] = Math.random()*10 - 5;
            }
            output[i] = testFunction(input[i]);
        }

        ArrayList<double[][]> data = new ArrayList<double[][]>();
        data.add(input);
        data.add(output);
        return data;
    }

    private double[] testFunction(double[] x) {
        return new double[] {Math.sin(x[0])*Math.sin(x[1])};
    }
}
