package sjxbin;




import battlecode.common.GameActionException;
import lumber_jack_s.RobotPlayer;
import java.util.Random;

/******************************************************************************
 *  Compilation:  javac Matrix.java
 *  Execution:    java Matrix
 *
 *  A bare-bones immutable data type for M-by-N matrices.
 *
 ******************************************************************************/

final public class Matrix {
    private final int M;             // number of rows
    private final int N;             // number of columns
    private final double[][] data;   // M-by-N array

    // create M-by-N matrix of 0's
    public Matrix(int M, int N) {
        this.M = M;
        this.N = N;
        data = new double[M][N];
    }

    // create matrix based on 2d array
    public Matrix(double[][] data) {
        M = data.length;
        N = data[0].length;
        this.data = new double[M][N];
        for (int i = 0; i < M; i++)
            for (int j = 0; j < N; j++)
                this.data[i][j] = data[i][j];
    }

    // copy constructor
    private Matrix(Matrix A) {
        this(A.data);
    }
    public Matrix copy() {
        return new Matrix(this);
    }

    // create and return a random M-by-N matrix with values between 0 and 1
    public static Matrix random(int M, int N) {
        Matrix A = new Matrix(M, N);
        for (int i = 0; i < M; i++)
            for (int j = 0; j < N; j++)
                A.data[i][j] = Math.random();
        return A;
    }

    // create and return the N-by-N identity matrix
    public static Matrix identity(int N) {
        Matrix I = new Matrix(N, N);
        for (int i = 0; i < N; i++)
            I.data[i][i] = 1;
        return I;
    }

    //region andystuff
    /**
     * START stuff added by Andy.
     */

    public int numElements() {
        return M*N;
    }
    public int numRows() {
        return M;
    }
    public int numColumns() {
        return N;
    }

    public double[][] getData() {
        return data.clone();
    }

    public Matrix sumOver(char axis) throws RuntimeException{
        Matrix A = null;
        switch (axis) {
            case 'M':
                A = new Matrix(1,N);
                for (int i = 0; i < M; i++)
                    for (int j = 0; j < N; j++)
                        A.data[0][j] += data[i][j];
                break;
            case 'N':
                A = new Matrix(M, 1);
                for (int i = 0; i < M; i++)
                    for (int j = 0; j < N; j++)
                        A.data[i][0] += data[i][j];
                break;
            default:
                throw new RuntimeException("Axis must be either 'M' or 'N'.");
        }
        return A;
    }

    public Matrix elementwiseTimes(Matrix B) {

        if (B.M != M || B.N != N)
            throw new RuntimeException("Illegal matrix dimensions.");

        Matrix A = new Matrix(M, N);
        for (int i = 0; i < M; i++)
            for (int j = 0; j < N; j++)
                A.data[i][j] = data[i][j]*B.data[i][j];
        return A;
    }
    public Matrix hadamardProduct(Matrix B) {
        return elementwiseTimes(B);
    }

    public Matrix plusInPlace(double num) {
        for (int i = 0; i < M; i++)
            for (int j = 0; j < N; j++)
                data[i][j] += num;
        return this;
    }

    public Matrix timesInPlace(double num) {
        for (int i = 0; i < M; i++)
            for (int j = 0; j < N; j++)
                data[i][j] *= num;
        return this;
    }

    public Matrix powInPlace(double num) {
        for (int i = 0; i < M; i++)
            for (int j = 0; j < N; j++)
                data[i][j] = Math.pow(data[i][j], num);
        return this;
    }

    public Matrix expInPlace() {
        for (int i = 0; i < M; i++)
            for (int j = 0; j < N; j++)
                data[i][j] = Math.exp(data[i][j]);
        return this;
    }

    public Matrix sigmoidInPlace() {
        for (int i = 0; i < M; i++)
            for (int j = 0; j < N; j++)
                data[i][j] = SjxMath.sigmoid(data[i][j]);
        return this;
    }

    public Matrix sigmoid() {
        Matrix A = new Matrix(M, N);
        double[][] adata = A.data;
        double[] ai;
        double[] thisi;

        for (int i = 0; i < M; i++) {
            ai = adata[i];
            thisi = data[i];
            for (int j = 0; j < N; j++) {
                ai[j] = SjxMath.sigmoid(thisi[j]);
            }
        }
        return A;
    }

    public Matrix sigmoidDerivative() {
        Matrix A = new Matrix(M, N);
        for (int i = 0; i < M; i++)
            for (int j = 0; j < N; j++)
                A.data[i][j] = SjxMath.sigmoidDerivative(data[i][j]);
        return A;
    }

    public double[] flatten() {
        double[] flattened = new double[M*N];
        int index = 0;
        for (int i = 0; i < M; i++)
            for (int j = 0; j < N; j++)
                flattened[index++] = data[i][j];
        return flattened;
    }

    public Matrix expandWithVector(Matrix B) throws RuntimeException {
        if (this.M !=1 || B.N !=1) {
            throw new RuntimeException("Both matrices must be vectors transposed correctly.");
        }

        Matrix C = new Matrix(B.M, N);
        for (int i = 0; i < B.M; i++)
            for (int j = 0; j < N; j++)
                C.data[i][j] = data[0][j] * B.data[i][0];


        return C;
    }

    public Matrix getRowasRow(int index) {
       return new Matrix(data[index]);
    }
    public Matrix getColumnAsRow(int index) {
        // TODO don't transpose globalFirst.
        return new Matrix(this.transpose().data[index]);
    }

    // Row vector constructor.
    public Matrix(double[] vector) {
        this(new double[][] {vector});
    }

    // This batch version expands each row/column of "this" and "B", and adds them to the expanded matrix.
    // Note that while the inputs are rowxrow, the output will be [B.N]x[this.N], or columnxcolumn.
    public Matrix batchExpandWithVectorAndSumRowByRow(Matrix B) throws RuntimeException {
        // NOTE B needs to be transposed globalFirst if you want normal row/column expansion. Otherwise it's
        //  rowxrow.

        if (this.M != B.M) {
            throw new RuntimeException("This expansion is row by row. There must be equal numbers of rows.");
        }

        Matrix C = new Matrix(this.N, B.N);
        for (int i = 0; i < M; i++)  {
            Matrix Avec = this.getRowasRow(i);
            Matrix Bvec = B.getRowasRow(i).transpose();
            C = C.plus(Avec.expandWithVector(Bvec).transpose());
        }

        return C;
    }

    public Matrix assignRowInPlace(Matrix B, int index) throws RuntimeException {
        if (B.M != 1 || B.N != N) {
            throw new RuntimeException("assignRow requires a single row vector matrix of 1xN.");
        }

        for (int j = 0; j < N; j++)
            data[index][j] = B.data[0][j];

        return this;
    }

    public Matrix assignRowInPlace(double[] B, int index) throws RuntimeException {

        if (N != B.length)
            throw new RuntimeException("assignRowInPlace requires a single row array" +
                    " with length equal to N.");

        for (int j = 0; j < B.length; j++)
            data[index][j] = B[j];

        return this;
    }


    // This multiplies "this" by each row of B as a vector.
    // The output is a matrix of B.M rows and this.M columns.
    public Matrix timesByRowVectors(Matrix B) {

        // The inners are this.N and B.N, because we are treating
        //  each B row as a vector of M=1. Thus, the new matrix is
        //  a matrix that's the number of B row vectors (B.M) by the
        //  remaining side of multiplying "this" times a vector (this.M).
        Matrix C = new Matrix(B.M, this.M);

        // Iterate rows of B.
        for (int i = 0; i < B.M; i++)
            // Pull out a row vector from B, multiply this matrix by it,
            //  and assign it to C. Note we need to transpose the row to a column vector, and then to
            //  a row vector after the multiplication.
            C.assignRowInPlace(this.times(B.getRowasRow(i).transpose()).transpose(), i);

        return C;
    }

    public double getData(int i, int j) {
        return data[i][j];
    }

    public Matrix appendColumn(double value) {

        double[][] B = new double[M][N+1];

        for (int i = 0; i < M; i++) {
            for (int j = 0; j < N; j++)
                B[i][j] = data[i][j];
            B[i][B[i].length-1] = value;
        }

        return new Matrix(B);
    }


    public Matrix dropoutMinus(Matrix B, Random random, double rate) {

        if (rate < 0 || rate > 1)
            throw new RuntimeException("Rate must be between 0 and 1");

        // For this to be consistent, random should be seeded the same every time it's passed.

        //  This can be used for "dropout" where only a select set of parameters have their gradient
        //  applied.

        Matrix A = this;
        if (B.M != A.M || B.N != A.N)
            throw new RuntimeException("Illegal matrix dimensions.");
        Matrix C = new Matrix(M, N);
        for (int i = 0; i < M; i++)
            for (int j = 0; j < N; j++) {
                double r = random.nextDouble();
                if (r < rate)
                    C.data[i][j] = A.data[i][j] - B.data[i][j];
                else
                    C.data[i][j] = A.data[i][j];
            }
        return C;
    }

    // These both return the channel after the last broadcast channel read.
    // This is used so iterating functions can get the next free channel.
    static double decimalMoveForIntBroadcast = 100000;
    public int writeBroadcast(int startingChannel, double lowerBound, double higherBound, int bits) {
        // TODO use lowerBound, higherBound and bits to compress data.

        // Store to return the last channel read.
        int i = 0;
        try {
            double[] flat = this.flatten();
            for (i = 0; i < flat.length; i++) {
                RobotPlayer.rc.broadcast(i+startingChannel,(int)(flat[i]*decimalMoveForIntBroadcast));
            }
        }
        catch (GameActionException e){
            System.out.println("Failed to write matrix to broadcast.");
        }
        return i+startingChannel+1;
    }
    public int readBroadcast(int startingChannel, double lowerBound, double higherBound, int bits) {
        // TODO use lowerBound, higherBound and bits to decompress data.

        // Store to return the channel after the last read.
        int i = 0;
        int j = 0;
        try {
            for (i = 0; i < M; i++) {
                for (j = 0; j < N; j++) {
                    data[i][j] = RobotPlayer.rc.readBroadcast(startingChannel+(i*N)+j)
                            /decimalMoveForIntBroadcast;
                }
            }
        }
        catch (GameActionException e) {
            System.out.println("Failed to read in matrix from broadcast.");
        }
        // Don't need to add j because i incremented for the last row.
        return startingChannel+(i*N)+1;
    }
    public int writeBroadcastBytecodeCost(int bits) {
        // Bytecode totalCost for one broadcast write * num elements.
        return 10*numElements()*(bits/32);
    }
    public int readBroadcastBytecodeCost(int bits) {
        // Bytecode totalCost for one broadcast read * num elements.
        return 5*numElements()*(bits/32);
    }
    public int readWriteBroadcastBytecodeCost(int bits) {
        // Bytecost for a read and write * num elements.
        return 15*numElements()*(bits/32);
    }

    // Is A close to B?
    public boolean eq(Matrix B, double maximumDifference) {

        if (maximumDifference < 0) throw new RuntimeException("maximumDifference must be positive.");

        Matrix A = this;
        if (B.M != A.M || B.N != A.N) throw new RuntimeException("Illegal matrix dimensions.");

        for (int i = 0; i < M; i++)
            for (int j = 0; j < N; j++)
                if (Math.abs(A.data[i][j]-B.data[i][j]) > maximumDifference) return false;
        return true;
    }

    /**
     * END stuff added by Andy.
     */
    //endregion andystuff



    // swap rows i and j
    private void swap(int i, int j) {
        double[] temp = data[i];
        data[i] = data[j];
        data[j] = temp;
    }

    // create and return the transpose of the invoking matrix
    public Matrix transpose() {
        Matrix A = new Matrix(N, M);
        for (int i = 0; i < M; i++)
            for (int j = 0; j < N; j++)
                A.data[j][i] = this.data[i][j];
        return A;
    }

    // return C = A + B
    public Matrix plus(Matrix B) {
        Matrix A = this;
        if (B.M != A.M || B.N != A.N)
            throw new RuntimeException("Illegal matrix dimensions.");
        Matrix C = new Matrix(M, N);
        for (int i = 0; i < M; i++)
            for (int j = 0; j < N; j++)
                C.data[i][j] = A.data[i][j] + B.data[i][j];
        return C;
    }


    // return C = A - B
    public Matrix minus(Matrix B) {
        Matrix A = this;
        if (B.M != A.M || B.N != A.N)
            throw new RuntimeException("Illegal matrix dimensions.");
        Matrix C = new Matrix(M, N);
        for (int i = 0; i < M; i++)
            for (int j = 0; j < N; j++)
                C.data[i][j] = A.data[i][j] - B.data[i][j];
        return C;
    }

    // does A = B exactly?
    public boolean eq(Matrix B) {
        Matrix A = this;
        if (B.M != A.M || B.N != A.N) throw new RuntimeException("Illegal matrix dimensions.");
        for (int i = 0; i < M; i++)
            for (int j = 0; j < N; j++)
                if (A.data[i][j] != B.data[i][j]) return false;
        return true;
    }

    // return C = A * B
    public Matrix times(Matrix B) {

        //SjxBytecodeTracker bct = new SjxBytecodeTracker();
        //bct.start();

        Matrix A = this;
        if (A.N != B.M)
            throw new RuntimeException("Illegal matrix dimensions.");
        Matrix C = new Matrix(A.M, B.N);

        // Pre loop declarations.
        double[][] cdata = C.data;
        double[][] adata = A.data;
        double[][] bdata = B.data;
        double[] cai;
        double[] aai;

        int cm = C.M;
        int cn = C.N;
        int an = A.N;

        for (int i = 0; i < cm; i++) {
            cai = cdata[i];
            aai = adata[i];
            for (int j = 0; j < cn; j++) {
                for (int k = 0; k < an; k++) {
                    cai[j] += (aai[k] * bdata[k][j]);
                }
            }
        }
        //bct.end();
        return C;
    }


    // return x = A^-1 b, assuming A is square and has full rank
    public Matrix solve(Matrix rhs) {
        if (M != N || rhs.M != N || rhs.N != 1)
            throw new RuntimeException("Illegal matrix dimensions.");

        // create copies of the data
        Matrix A = new Matrix(this);
        Matrix b = new Matrix(rhs);

        // Gaussian elimination with partial pivoting
        for (int i = 0; i < N; i++) {

            // find pivot row and swap
            int max = i;
            for (int j = i + 1; j < N; j++)
                if (Math.abs(A.data[j][i]) > Math.abs(A.data[max][i]))
                    max = j;
            A.swap(i, max);
            b.swap(i, max);

            // singular
            if (A.data[i][i] == 0.0) throw new RuntimeException("Matrix is singular.");

            // pivot within b
            for (int j = i + 1; j < N; j++)
                b.data[j][0] -= b.data[i][0] * A.data[j][i] / A.data[i][i];

            // pivot within A
            for (int j = i + 1; j < N; j++) {
                double m = A.data[j][i] / A.data[i][i];
                for (int k = i + 1; k < N; k++) {
                    A.data[j][k] -= A.data[i][k] * m;
                }
                A.data[j][i] = 0.0;
            }
        }

        // back substitution
        Matrix x = new Matrix(N, 1);
        for (int j = N - 1; j >= 0; j--) {
            double t = 0.0;
            for (int k = j + 1; k < N; k++)
                t += A.data[j][k] * x.data[k][0];
            x.data[j][0] = (b.data[j][0] - t) / A.data[j][j];
        }
        return x;
    }
}