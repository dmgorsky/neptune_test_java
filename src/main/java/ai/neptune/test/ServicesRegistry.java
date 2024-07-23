package ai.neptune.test;

import ai.neptune.test.model.AddBatch;
import ai.neptune.test.model.StatsResponse;
import io.vavr.control.Either;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.HashMap;

@ApplicationScoped
public class ServicesRegistry {


    private HashMap<String, PriceService> priceServices = new HashMap<>();

    public Either<String, StatsResponse> getStats(String symbol, int k) {
        if (!priceServices.containsKey(symbol)) {
            return Either.left("Symbol not found");
        }
        var priceService = priceServices.get(symbol);
        return priceService.getStats(k);
    }

    public Either<String, String> addBatch(AddBatch command) {
        if (priceServices.size() > 10) {
            return Either.left("Too many symbols");
        }
        priceServices.putIfAbsent(command.getSymbol(), new PriceService());
        var priceService = priceServices.get(command.getSymbol());
        return priceService.addBatch(command.getValues());
    }
}
