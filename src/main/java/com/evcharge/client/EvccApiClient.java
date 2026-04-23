package com.evcharge.client;

import com.evcharge.dto.EvccSession;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;

@RegisterRestClient(configKey = "evcc-api")
@Path("/api")
public interface EvccApiClient {

    @GET
    @Path("/sessions")
    @Produces(MediaType.APPLICATION_JSON)
    List<EvccSession> getSessions();
}
