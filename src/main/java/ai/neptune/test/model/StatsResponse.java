package ai.neptune.test.model;

public record StatsResponse(
        String calculation_range,
        Float prices_sum,
        Float prices_min,
        Float prices_max,
        Float prices_last,
        Float prices_variance
) {
}
