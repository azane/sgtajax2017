package sjxbin;

/**
 * Created by azane on 1/19/17.
 */
public class SjxBinary {

    // Turns the intset into a byte string, then builds a new list of longs from that byte string.
    // Bits left over that are smaller than "length" are not reported.
    public long[] parseInts(int[] intset, int length) throws Exception {

        if (length > 64 || length < 1) {
            throw new Exception("length must be greater than zero and less than or equal to 65.");
        }

        // TODO

        return new long[0];

    }

}
