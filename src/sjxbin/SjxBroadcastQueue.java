package sjxbin;

import battlecode.common.*;

import java.util.HashMap;
import java.util.Random;

/**
 * Created by azane on 1/27/17.
 */

public class SjxBroadcastQueue {

    // Because the gameconstants one is not correct.
    private final static int MAXBROADCASTCHANNEL = 9999;

    public final int size;
    private final int elementLength;
    public final int startingChannel;
    public final int positionIndicatorChannel;
    public final int numberElementsChannel;

    private final int lastWriteableChannel;
    private final int firstWriteableChannel;

    // The holder for the actual broadcast index position of the front
    //  of the queue.
    private int absoluteQueuePosition;

    // The number of data elements in the queue. This won't exceed size,
    //  but could be less than size.
    // Increment on enqueue, decrement on pop.
    private int numElements = 0;
    private void incNumElements() {
        // Don't make larger than the size.
        // This is okay, as enqueue overwrites the oldest data if necessary.
        if (numElements == size)
            return;
        else
            numElements++;
    }
    private void decNumElements() {
        // Having something call the decrementer when there is no data
        //  is actually an error.
        if (numElements == 0)
            throw new RuntimeException("Can't have a negative number of elements.");
        else
            numElements--;
    }
    public int getNumElements() {
        return numElements;
    }
    public boolean isEmpty() {
        return (numElements == 0);
    }

    private RobotController rc;

    // The labels of the data.
    private final HashMap<String, Integer> labels;

    // A holder for the currentIndex (abstracted). Used to call the next one.
    private int currentIndex = 0;
    public int getCurrentIndex() {
        return currentIndex;
    }

    public SjxBroadcastQueue(String[] labels, RobotController rc,
                             int size, int startingChannel, int positionIndicatorChannel,
                             int numberElementsChannel,
                             boolean initToGlobalQueuePosition) {
        this.size = size;
        elementLength = labels.length;
        if (elementLength <= 0)
            throw new RuntimeException("Invalid labels.");

        if (startingChannel <= (MAXBROADCASTCHANNEL - (this.size*elementLength))
                && startingChannel >= 0)
            this.startingChannel = startingChannel;
        else
            throw new RuntimeException("Invalid startingChannel or size.");

        if ((positionIndicatorChannel < this.startingChannel
                    || positionIndicatorChannel >= this.startingChannel + (this.size*elementLength))
                && positionIndicatorChannel >= 0
                && positionIndicatorChannel < MAXBROADCASTCHANNEL)
            this.positionIndicatorChannel = positionIndicatorChannel;
        else
            throw new RuntimeException("Invalid positionIndicatorChannel.");

        if ((numberElementsChannel < this.startingChannel
                || numberElementsChannel >= this.startingChannel + (this.size*elementLength))
                && numberElementsChannel >= 0
                && numberElementsChannel < MAXBROADCASTCHANNEL
                && numberElementsChannel != positionIndicatorChannel)
            this.numberElementsChannel = numberElementsChannel;
        else
            throw new RuntimeException("Invalid numberElementsChannel.");

        this.labels = new HashMap<>();
        for (int i = 0; i < elementLength; i++) {
            this.labels.put(labels[i], i);
        }

        // The last writeable channel is the starting channel, plus the number
        //  of channels occupied by a full set of data, minus 1 for index.
        lastWriteableChannel = startingChannel + (size*elementLength) - 1;
        // The globalFirst writeable channel is the starting channel, plus the number
        //  of channels occupied by one piece of data, minus 1 for index of length.
        firstWriteableChannel = startingChannel + elementLength -1;

        this.rc = rc;


        // Initialize the queue position to the last writeable channel, this way,
        //  the globalFirst enqueue increment will return it to the
        //  globalFirst writeable channel, instead of where the second data will go.
        // NOTE: we'll overwrite this if the global position has already been initialized.
        absoluteQueuePosition = lastWriteableChannel;

        if (initToGlobalQueuePosition) {

            // Check if the queue position has been initialized.
            // It starts at 0, but a queue position can never be 0, because the starting channel
            //  must be zero or greater, and at least 1 piece of data means that the last
            //  writeable channel must at least be 1.
            int queuePositionQuery;
            try {
                queuePositionQuery = rc.readBroadcast(positionIndicatorChannel);
            } catch (GameActionException e) {
                throw new RuntimeException("Could not retrieve initial queue position.");
            }

            // If it hasn't been initialized, initialize!
            if (queuePositionQuery == 0) {
                writeMetadata();
                yieldForBroadcast();
            } else
                // If it has, read it.
                readMetadata();
        }
    }

    private int getChannel(int index, String label) {

        if (index < 0 || index >= numElements)
            throw new IndexOutOfBoundsException("Index " + index + " is either less" +
                    " than zero or exceeds the last element index of " + (numElements-1) +
                    "\nIf the last element index is -1, this queue is empty. ; )");
        if (!labels.containsKey(label))
            throw new IndexOutOfBoundsException("The label " + label + " does not exist.");

        // The actual broadcast channel will be the queue position
        //  minus the number of elements back, times the element length.
        // Then, subtract the label index to get to the correct data.
        int nonCyclicIndex =
                            absoluteQueuePosition
                            - (index * elementLength)
                            - labels.get(label);

        // But, we must make sure we are looping back around.
        if (nonCyclicIndex >= startingChannel)
            return nonCyclicIndex;
        else
            // Wrap around to the top, but add one for index.
            return lastWriteableChannel - (startingChannel - nonCyclicIndex) + 1;
    }

    private int get(int index, String label) {
        try {
            return rc.readBroadcast(getChannel(index, label));
        }
        catch (GameActionException e) {
            throw new RuntimeException("Broadcast read failed.");
        }
    }

    private HashMap<String, Integer> get(int index) {
        HashMap<String, Integer> data = new HashMap<>();
        for (String label : labels.keySet()) {
            data.put(label, get(index, label));
        }
        return data;
    }

    private void write(int index, String label, int value) {
        try {
            rc.broadcast(getChannel(index, label), value);
        }
        catch (GameActionException e) {
            throw new RuntimeException("Broadcast write failed.");
        }
    }

    private void write(int index, HashMap<String, Integer> data) {
        for (String label : labels.keySet()) {
            write(index, label, data.get(label));
        }
    }

    public void writeCurrent(String label, int value) {
        write(currentIndex, label, value);
    }
    public void writeCurrent(HashMap<String, Integer> data) {
        write(currentIndex, data);
    }

    // Return the specified property of the data at the readCurrent index.
    public int readCurrent(String label) {
        return get(currentIndex, label);
    }

    // Return the data at the readCurrent index.
    public HashMap<String, Integer> readCurrent() {
        return get(currentIndex);
    }

    // Move to the next set of data. Return false if there is no next data.
    public boolean next() {

        // We're going to add one to the index, make sure that doesn't
        //  exceed our indexing (length - 1).
        if ((currentIndex + 1) >= numElements)
            return false;

        currentIndex++;
        if (currentIndex == size)
            currentIndex = 0;

        return true;
    }

    public boolean nextExists() {
        if (currentIndex == numElements - 1)
            return false;
        else
            return true;
    }

    // Reset the iterator to the globalFirst entry (top of queue), and return the data.
    public void first() {
        currentIndex = 0;
    }
    public void prepIter() {
        currentIndex = -1;
    }

    public boolean enqueue(HashMap<String, Integer> data) {
        // Advance the front of the queue. If old memory exists here,
        //  it will be overwritten. Hence, FIFO.

        advanceQueuePosition();
        try {
            // This knows about overwriting old data.
            // Increment here so the indexer knows there's a place for the new data.
            incNumElements();

            // Write each piece of data to the new front of queue.
            write(0, data);
            return true;
        }
        catch (RuntimeException e) {
            // Return the queue position to where it was originally.
            // This will also erase partial writes if any element of the data
            //  failed to broadcast.
            retreatQueuePosition();
            return false;
        }
    }

    private void advanceQueuePosition() {
        // Wrap around to the beginning if needed.
        if (absoluteQueuePosition < lastWriteableChannel)
            absoluteQueuePosition += elementLength;
        else
            absoluteQueuePosition = firstWriteableChannel;
    }
    private void retreatQueuePosition() {
        if (absoluteQueuePosition == firstWriteableChannel)
            absoluteQueuePosition = lastWriteableChannel;
        else
            absoluteQueuePosition -= elementLength;
    }

    // This method 'pops', stack style, the most recent entry of the queue.
    // I know it's not queue behavior, git over iiiit.
    public void pop() {
        // This will throw an error if there aren't any elements.
        decNumElements();
        retreatQueuePosition();
    }

    // The caller is responsible for calling this after enqueuing a bunch of stuff.
    public boolean writeMetadata() {
        try {
            rc.broadcast(positionIndicatorChannel, absoluteQueuePosition);
            rc.broadcast(numberElementsChannel, numElements);
            return true;
        }
        catch (GameActionException e) {
            return false;
        }
    }

    // The caller is responsible for calling this before processing.
    public boolean readMetadata() {
        try {
            absoluteQueuePosition = rc.readBroadcast(positionIndicatorChannel);
            numElements = rc.readBroadcast(numberElementsChannel);
            return true;
        }
        catch (GameActionException e) {
            return false;
        }
    }

    // This can be used to guarantee a broadcast.
    public void yieldForBroadcast() {
        Clock.yield();
    }

    public static boolean test(RobotController rc) {
        return TestSjxBroadcastQueue.test(rc);
    }
}

class TestSjxBroadcastQueue {
    public static boolean test(RobotController rc) {

        int lockChannel = 999;
        // Only allow one bot to run this test at a time.
        try {
            Clock.yield();
            if (rc.readBroadcast(lockChannel) == 1)
                return true;
            else {
                rc.broadcast(lockChannel, 1);
                Clock.yield(); //Guarantee broadcast to other bots.
            }
        }
        catch (GameActionException e) {
            return false;
        }


        String[] labels = new String[] {"1", "two", "twa", "net", "cinco"};

        SjxBroadcastQueue queue = new SjxBroadcastQueue(
                labels, rc, 10, 500, 499,
                498,
                false);

        long rseed = 1986058301;
        Random r = new Random();
        r.setSeed(rseed);

        int max = queue.size + 15;
        HashMap<String, Integer>[] testerData = new HashMap[max];
        for (int i = 0; i < max; i++) {
            testerData[i] = new HashMap<>();
            for(String s : labels)
                testerData[i].put(s, r.nextInt());
            queue.enqueue(testerData[i]);

            queue.writeMetadata();

            queue.yieldForBroadcast();

            queue.readMetadata();

            // Ensure that the recent queue-age processed.
            queue.first();
            for (String s : labels) {
                if (queue.readCurrent(s) != testerData[i].get(s))
                    return false;
            }
        }

        // Iterate through the queue to make sure it's content is accurate to the last
        //  data enqueued.
        queue.readMetadata();
        queue.first();
        for (int i = max-1; i > max - queue.size - 1; i--) {
            for(String s : labels)
                if (queue.readCurrent(s) != testerData[i].get(s))
                    return false;
            queue.next();
        }

        // Verify that the queue stuck to its memory limitations.
        try {
            if (rc.readBroadcast(queue.startingChannel - 2) != 0)
                return false;
            if (rc.readBroadcast(queue.startingChannel + queue.size*labels.length) != 0)
                return false;
        }
        catch (GameActionException e) {
            return false;
        }

        // Pop the stack till the next element doesn't exist. That will be the last one.
        while(queue.nextExists()) {
            queue.pop();
        }
        if (queue.getNumElements() != 1) {
            return false;
        }
        // pop the last one
        queue.pop();
        if (queue.getNumElements() != 0)
            return false;

        try {
            // This should throw an error.
            queue.pop();
            return false;
        }
        catch (RuntimeException e) {
            // Good job.
        }



        // Release the test to others.
        try {

            rc.broadcast(lockChannel, 0);
            Clock.yield(); //Guarantee broadcast to other bots.

        }
        catch (GameActionException e) {
            return false;
        }

        return true;
    }
}
