package com.evcharge.web;

import com.evcharge.service.ChargeSessionService;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/")
public class DashboardController {

    @Inject
    Template dashboard;

    @Inject
    ChargeSessionService service;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance index() {
        return dashboard
            .data("sessions", service.getAllSessions())
            .data("statistics", service.getStatistics());
    }
}
