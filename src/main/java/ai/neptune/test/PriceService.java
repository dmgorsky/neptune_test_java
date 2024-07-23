package ai.neptune.test;

import ai.neptune.test.model.StatsResponse;
import io.vavr.control.Either;
import io.vavr.control.Try;

import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class PriceService {
    private static final int maxSize = 100_000_000;
    private final Object pricesLock = new Object();

    private Float[] prices = new Float[maxSize];

    private AtomicInteger endSequence = new AtomicInteger(0);

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
        var sLast = prices[endSequence.get()];
        var sVariance = 0.0f;
        var startFrom = endSequence.get();
//        var startFrom = endSequence.get() - realK;
//        if (startFrom < 0) {
//            startFrom += maxSize;
//        }

        var it = new StatsReverseIterator(k);

        while (it.hasNext()) {
            var next = it.next();
            sSum += next;
            sMin = Math.min(sMin, next);
            sMax = Math.max(sMax, next);
            sCount++;
        }
        var mean = sSum / sCount;

        it = new StatsReverseIterator(k);
        while (it.hasNext()) {
            var next = it.next();
            sVariance += (next - mean) * (next - mean);
        }

        sVariance /= sCount;

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
        //guard
        if (values.length > maxSize) {
            return Either.left("Too many values");
        }
        //let's limit writes
        var lock = Try.of(() -> {
            semaphore.acquire();
            return "ok";
        }).toEither();
        synchronized (pricesLock) { // write blocks lock on `pricesLock`
            //should we split?
            int endIndex = endSequence.get() + values.length;
            if (endIndex >= maxSize) {
                // split
                int split = endIndex - maxSize + 1; //check
                System.arraycopy(values, 0, prices, endSequence.get(), split);
                System.arraycopy(values, split, prices, 0, values.length - split);
            } else {
                // no split
                System.arraycopy(values, 0, prices, endSequence.get(), values.length);
            }
            //and update new ending
            endSequence.addAndGet(values.length);
            semaphore.release();
        }

        return Either.right("" + values.length);
    }

    /**
     * This iterator allows to get `prices` elements in reverse order.
     * However, `k` order provided is checked only for maxSize.
     * If `k` order is greater than actual prices added,
     * the iterator will take prices initially in the array.
     **/
    public class StatsReverseIterator implements Iterator<Float> {
        int endIndex; // start index for reverse iteration

        int neededCount; // how many values we want to take

        public StatsReverseIterator(int k) {
            this.neededCount = (int) Math.min(maxSize, Math.pow(10, k));
            this.endIndex = endSequence.get();
        }

        private int index = endIndex;

        @Override
        public boolean hasNext() {
            return this.neededCount > 0;
        }

        @Override
        public Float next() {
            if (index < 0) {
                index = endIndex;
                neededCount--;
            }
            return prices[index--];
        }
    }
}
