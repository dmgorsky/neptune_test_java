package ai.neptune.test;

import ai.neptune.test.model.AddBatch;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.parsing.Parser;
import jakarta.inject.Inject;
import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

@QuarkusTest
class HttpEndpointTest {

    @Inject
    PriceService priceService;

    @Inject
    ServicesRegistry servicesRegistry;

    @Test
    void testHttpEndpoint() {
        given()
                .when().get("/stats/?symbol=w&k=10")
                .then()
                .statusCode(404);

        given()
                .when()
                .given()
                .contentType("application/json")
                .body(new AddBatch("q", new Float[]{1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f, 9.0f, 10.0f}))
                .post("/add_batch")
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .body(is("10"));

        given()
                .contentType("application/json")
                .pathParam("symbol", "q")
                .pathParam("k", "1")
                .when().get("/stats/?symbol={symbol}&k={k}")
                .then()
                .contentType("application/json")
                .defaultParser(Parser.JSON)
                .statusCode(200)
                .body("prices_min", is(1.0f))
                .body("prices_max", is(10.0f))
                .body("prices_sum", is(55.0f))
                .body("prices_variance", is(8.25f))
                .body("prices_last", is(10.0f))
        ;
    }


}

