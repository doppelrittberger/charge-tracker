package com.evcharge.web;

import com.evcharge.service.ChargeSessionService;
import com.evcharge.service.OdometerService;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
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

    @Inject
    OdometerService odometerService;

    @ConfigProperty(name = "vehicle.battery.capacity-kwh", defaultValue = "77.0")
    java.math.BigDecimal batteryCapacityKwh;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance index() {
        return dashboard
            .data("sessions", service.getAllSessions())
            .data("statistics", service.getStatistics())
            .data("odometer", odometerService.getOdometer().orElse(null))
            .data("soc", odometerService.getSoc().orElse(null))
            .data("batteryCapacityKwh", batteryCapacityKwh);
    }
}
