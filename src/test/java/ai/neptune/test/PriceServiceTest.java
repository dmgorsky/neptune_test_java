package ai.neptune.test;

import ai.neptune.test.model.AddBatch;
import io.quarkus.test.junit.QuarkusTest;
import io.vavr.control.Either;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

@QuarkusTest
class PriceServiceTest {

    @Inject
    PriceService priceService;

    @Inject
    ServicesRegistry servicesRegistry;

    @Test
    void testPriceService() {


        //adding values, moving the pointer
        Either<String, String> ab = priceService.addBatch(new Float[]{1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f, 9.0f, 10.0f});
        Assertions.assertTrue(ab.contains("10"));
        ab = priceService.addBatch(new Float[]{1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f, 9.0f, 10.0f});
        Assertions.assertTrue(ab.contains("20"));

        //forcing circular buffer to rewind
        var newarr = new Float[100_000_000];
        Arrays.fill(newarr, 0f);
        ab = priceService.addBatch(newarr);
        Assertions.assertTrue(ab.contains("20"));
        ab = priceService.addBatch(new Float[]{1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f, 9.0f, 10.0f});
        Assertions.assertTrue(ab.contains("30"));

        //checking guard for null
        ab = priceService.addBatch(new Float[100]);
        Assertions.assertEquals("Null value present", ab.getLeft());
        System.err.println("#3");

        //checking stats calculation (simple case, last 1e1 entries)
        var st = priceService.getStats(1);

        System.err.println(st.toString());
        Assertions.assertTrue(st.isRight());
        var stats = st.get();
        Assertions.assertEquals(1f, stats.prices_min());
        Assertions.assertEquals(10f, stats.prices_max());
        Assertions.assertEquals(55f, stats.prices_sum());
        Assertions.assertEquals(8.25f, stats.prices_variance());
        Assertions.assertEquals(10f, stats.prices_last());

    }

    @Test
    void testServicesRegistry() {
        servicesRegistry = new ServicesRegistry(); // re-init
        Either<String, String> ab = Either.left("Too many symbols");

        for (int i = 0; i <= 10; i++) {
            ab = servicesRegistry.addBatch(new AddBatch(i + "", new Float[]{1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f, 9.0f, 10.0f}));
        }
        Assertions.assertTrue(ab.isRight());
        ab = servicesRegistry.addBatch(new AddBatch("q", new Float[]{1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f, 9.0f, 10.0f}));
        Assertions.assertEquals(ab.getLeft(), "Too many symbols");
    }


}

