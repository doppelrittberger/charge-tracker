package com.evcharge.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;

@RegisterRestClient(configKey = "ha-api")
@Path("/api")
public interface HaApiClient {

    @GET
    @Path("/states/{entityId}")
    @Produces(MediaType.APPLICATION_JSON)
    HaState getState(@PathParam("entityId") String entityId,
                     @HeaderParam("Authorization") String bearerToken);

    @GET
    @Path("/history/period/{timestamp}")
    @Produces(MediaType.APPLICATION_JSON)
    List<List<HaHistoryState>> getHistory(@PathParam("timestamp") String isoTimestamp,
                                          @QueryParam("filter_entity_id") String entityId,
                                          @QueryParam("end_time") String endIsoTimestamp,
                                          @QueryParam("minimal_response") boolean minimalResponse,
                                          @HeaderParam("Authorization") String bearerToken);

    @JsonIgnoreProperties(ignoreUnknown = true)
    class HaHistoryState {
        public String state;
        @JsonProperty("last_changed")
        public String lastChanged;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    class HaState {
        public String state;
        @JsonProperty("attributes")
        public HaAttributes attributes;

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class HaAttributes {
            @JsonProperty("unit_of_measurement")
            public String unitOfMeasurement;
            @JsonProperty("friendly_name")
            public String friendlyName;
        }
    }
}
