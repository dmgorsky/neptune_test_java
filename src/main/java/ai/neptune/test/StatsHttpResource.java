package ai.neptune.test;

import ai.neptune.test.model.AddBatch;
import ai.neptune.test.model.StatsResponse;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestResponse;

@Path("/")
public class StatsHttpResource {

    @Inject
    ServicesRegistry servicesRegistry;


    @GET
    @Path("stats")

    @Produces(MediaType.APPLICATION_JSON)
    public RestResponse<StatsResponse> getStats(@QueryParam("symbol") String symbol,
                                                @QueryParam("k") int k) {
        var result = servicesRegistry.getStats(symbol, k);
        if (result.isLeft()) {
            return RestResponse.notFound();
        } else {
            return RestResponse.ok(result.get());
        }
    }


    @POST
    @Path("add_batch")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public RestResponse<String> addBatch(AddBatch command) {
        var result = servicesRegistry.addBatch(command);
        if (result.isLeft()) {
            return RestResponse.notModified("Too many symbols");
        } else {
            return RestResponse.ok(result.get());
        }
    }
}
