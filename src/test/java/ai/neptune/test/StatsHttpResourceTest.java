package ai.neptune.test;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class StatsHttpResourceTest {
    @Test
    void testHelloEndpoint() {
        given()
          .when().get("/stats/?symbol=w&k=10")
          .then()
             .statusCode(200)
//             .body(endsWith("Hello from PricesService!"))
        ;
    }

}
