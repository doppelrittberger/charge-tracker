package com.evcharge.resource;

import com.evcharge.dto.ChargeSessionRequest;
import com.evcharge.dto.ChargeSessionResponse;
import com.evcharge.dto.StatisticsResponse;
import com.evcharge.service.ChargeSessionService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.LocalDateTime;
import java.util.List;

@Path("/api/sessions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ChargeSessionResource {

    @Inject
    ChargeSessionService service;

    @POST
    public Response createSession(ChargeSessionRequest request) {
        ChargeSessionResponse response = service.createSession(request);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @GET
    public List<ChargeSessionResponse> getAllSessions(
            @QueryParam("start") String start,
            @QueryParam("end") String end) {
        
        if (start != null && end != null) {
            LocalDateTime startDate = LocalDateTime.parse(start);
            LocalDateTime endDate = LocalDateTime.parse(end);
            return service.getSessionsByDateRange(startDate, endDate);
        }
        
        return service.getAllSessions();
    }

    @GET
    @Path("/{id}")
    public Response getSession(@PathParam("id") Long id) {
        ChargeSessionResponse response = service.getSessionById(id);
        if (response == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(response).build();
    }

    @PUT
    @Path("/{id}")
    public Response updateSession(@PathParam("id") Long id, ChargeSessionRequest request) {
        ChargeSessionResponse response = service.updateSession(id, request);
        if (response == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(response).build();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteSession(@PathParam("id") Long id) {
        boolean deleted = service.deleteSession(id);
        if (!deleted) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }

    @GET
    @Path("/statistics")
    public StatisticsResponse getStatistics() {
        return service.getStatistics();
    }
}
