package sjxbin;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.RobotController;
import scala.Int;

import java.util.HashMap;
import java.util.Random;

/**
 * Created by azane on 1/27/17.
 */
public class SjxBroadcastQueue {

    public final int size;
    private final int elementLength;
    public final int startingChannel;
    public final int positionIndicatorChannel;

    private final int lastWriteableChannel;
    private final int firstWriteableChannel;

    // The holder for the actual broadcast index position of the front
    //  of the queue.
    private int absoluteQueuePosition;

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
                             boolean initToGlobalQueuePosition) {
        this.size = size;
        elementLength = labels.length;
        if (elementLength <= 0)
            throw new RuntimeException("Invalid labels.");

        if (startingChannel <= (GameConstants.BROADCAST_MAX_CHANNELS - (this.size*elementLength))
                && startingChannel >= 0)
            this.startingChannel = startingChannel;
        else
            throw new RuntimeException("Invalid startingChannel or size.");

        if ((positionIndicatorChannel < this.startingChannel
                    || positionIndicatorChannel >= this.startingChannel + (this.size*elementLength))
                && positionIndicatorChannel >= 0
                && positionIndicatorChannel < GameConstants.BROADCAST_MAX_CHANNELS)
            this.positionIndicatorChannel = positionIndicatorChannel;
        else
            throw new RuntimeException("Invalid positionIndicatorChannel.");

        this.labels = new HashMap<>();
        for (int i = 0; i < elementLength; i++) {
            this.labels.put(labels[i], i);
        }

        // The last writeable channel is the starting channel, plus the number
        //  of channels occupied by a full set of data, minus 1 for index.
        lastWriteableChannel = startingChannel + (size*elementLength) - 1;
        // The first writeable channel is the starting channel, plus the number
        //  of channels occupied by one piece of data, minus 1 for index of length.
        firstWriteableChannel = startingChannel + elementLength -1;

        this.rc = rc;


        // Initialize the queue position to the last writeable channel, this way,
        //  the first enqueue increment will return it to the
        //  first writeable channel, instead of where the second data will go.
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
                writeQueuePosition();
                yieldToGuaranteeBroadcast();
            } else
                // If it has, read it.
                readQueuePosition();
        }
    }

    private int getChannel(int index, String label) {
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

    public int get(int index, String label) {
        try {
            return rc.readBroadcast(getChannel(index, label));
        }
        catch (GameActionException e) {
            throw new RuntimeException("Broadcast read failed.");
        }
    }

    public HashMap<String, Integer> get(int index) {
        HashMap<String, Integer> data = new HashMap<>();
        for (String label : labels.keySet()) {
            data.put(label, get(index, label));
        }
        return data;
    }

    // Return the specified property of the data at the current index.
    public int current(String label) {
        return get(currentIndex, label);
    }

    // Return the data at the current index.
    public HashMap<String, Integer> current() {
        return get(currentIndex);
    }

    // Move to the next set of data.
    public void next() {
        currentIndex++;
        if (currentIndex == size)
            currentIndex = 0;
    }

    // Reset the iterator to the first entry (top of queue), and return the value
    //  under the given property.
    public int first(String label) {
        currentIndex = 0;
        return get(currentIndex, label);
    }

    // Reset the iterator to the first entry (top of queue), and return the data.
    public HashMap<String, Integer> first() {
        currentIndex = 0;
        return get(currentIndex);
    }

    public boolean enqueue(HashMap<String, Integer> data) {
        // Advance the front of the queue. If old memory exists here,
        //  it will be overwritten. Hence, FIFO.
        // Wrap around to the beginning if needed.
        if (absoluteQueuePosition < lastWriteableChannel)
            absoluteQueuePosition += elementLength;
        else
            absoluteQueuePosition = firstWriteableChannel;

        try {
            // Write each piece of data to the correct index.
            for (String s : labels.keySet()) {
                rc.broadcast(getChannel(0, s), data.get(s));
            }
            return true;
        }
        catch (GameActionException e) {
            // Return the queue position to where it was originally.
            // This will also erase partial writes if any element of the data
            //  failed to broadcast.
            if (absoluteQueuePosition == firstWriteableChannel)
                absoluteQueuePosition = lastWriteableChannel;
            else
                absoluteQueuePosition -= elementLength;
            return false;
        }
    }

    public boolean writeQueuePosition() {
        try {
            rc.broadcast(positionIndicatorChannel, absoluteQueuePosition);
            return true;
        }
        catch (GameActionException e) {
            return false;
        }
    }

    public boolean readQueuePosition() {
        try {
            absoluteQueuePosition = rc.readBroadcast(positionIndicatorChannel);
            return true;
        }
        catch (GameActionException e) {
            return false;
        }
    }

    public void yieldToGuaranteeBroadcast() {
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

            queue.yieldToGuaranteeBroadcast();

            // Ensure that the recent queue-age processed.
            for (String s : labels) {
                if (queue.first(s) != testerData[i].get(s))
                    return false;
            }
        }

        // Iterate through the queue to make sure it's content is accurate to the last
        //  data enqueued.
        queue.first();
        for (int i = max-1; i > max - queue.size - 1; i--) {
            for(String s : labels)
                if (queue.current(s) != testerData[i].get(s))
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
