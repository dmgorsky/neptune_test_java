package ai.neptune.test;

import ai.neptune.test.model.StatsResponse;
import io.vavr.control.Either;
import io.vavr.control.Try;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

@ApplicationScoped
public class PriceService {

    private static final int maxSize = 100_000_000;
    private final Object pricesLock = new Object();

    private Float[] prices = new Float[maxSize];

    private AtomicInteger nextStartSequence = new AtomicInteger(0);

    private Semaphore semaphore = new Semaphore(1);

    public PriceService() {
        Arrays.fill(prices, 0.0f);
    }

    public Either<String, StatsResponse> getStats(int k) {
        var realK = (int) Math.min(Math.pow(10, k), maxSize);
        var sSum = 0.0f;
        var sMin = Float.MAX_VALUE;
        var sMax = Float.MIN_VALUE;
        var sCount = 0;
        var sLast = prices[nextStartSequence.get() - 1];
        var sVariance = 0.0f;
        var startFrom = nextStartSequence.get() - 1;
        Float mean;
        Float varianceSq = 0f;

        //1 pass
        var it = new StatsRevDataStream(k);
        mean = it.peekNext(); // starting value from the stream


        while (it.hasNext()) {
            var currValue = it.next();
            sSum += currValue;
            sMin = Math.min(sMin, currValue);
            sMax = Math.max(sMax, currValue);
            if (sCount > 0) { // skipping the first element
                var delta = currValue - mean;
                mean += delta / (sCount + 1);
                varianceSq += delta * (currValue - mean);
            }

            sCount++;

        }

        sVariance = (float) Math.sqrt(varianceSq / sCount);

        var endTo = (startFrom - realK) % maxSize;
        if (endTo < 0) {
            endTo += maxSize;
        }
        return Either.right(new
                StatsResponse(
                startFrom + ".." + endTo,
                sSum,
                sMin,
                sMax,
                sLast,
                sVariance
        ));
    }

    public Either<String, String> addBatch(Float[] values) {

        //guards
        if (values.length > maxSize) {
            return Either.left("Too many values");
        }
        if (Arrays.asList(values).contains(null)) {
            return Either.left("Null value present");
        }
        //let's limit writes
        var lock = Try.of(() -> {
            semaphore.acquire();
            return "ok";
        }).toEither();
        synchronized (pricesLock) { // write blocks lock on `pricesLock`
            //should we split?
            int endIndex = nextStartSequence.get() + values.length;
            if (endIndex >= maxSize) {
                // split
                int split = endIndex - maxSize + 1; //check
                System.arraycopy(values, 0, prices, nextStartSequence.get(), split);
                System.arraycopy(values, split, prices, 0, values.length - split);
            } else {
                // no split
                System.arraycopy(values, 0, prices, nextStartSequence.get(), values.length);
            }
            //and update new ending
            nextStartSequence.addAndGet(values.length);
            if (nextStartSequence.get() >= maxSize) {
                nextStartSequence.addAndGet(-1 * maxSize);
            }
            semaphore.release();

        }

        return Either.right(nextStartSequence.toString());
    }

    /**
     * This iterator allows to get `prices` elements in reverse order.
     * However, `k` order provided is checked only for maxSize.
     * If `k` order is greater than actual prices added,
     * the iterator will take prices initially in the array.
     **/
    public class StatsRevDataStream implements Iterator<Float> {
        private int endIndex; // start index for reverse iteration

        private int neededCount; // how many values we want to take
        private int index;

        public StatsRevDataStream(int k) {
            this.neededCount = (int) Math.min(maxSize, Math.pow(10, k));
            this.endIndex = nextStartSequence.get() - 1;
            this.index = endIndex;
        }

        @Override
        public boolean hasNext() {
            return this.neededCount > 0;
        }

        @Override
        public Float next() {
            if (this.index < 0) {
                this.index = endIndex;
            }
            this.neededCount--;
            return prices[index--];
        }

        //peeking next element from the stream w/o moving a pointer
        public Float peekNext() {
            if (this.index < 0) {
                this.index = endIndex;
            }
            return prices[index];
        }

    }
}
