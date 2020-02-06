package com.redhat.emergency.response.disaster;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.redhat.emergency.response.disaster.model.DisasterCenter;
import com.redhat.emergency.response.disaster.model.InclusionZone;
import com.redhat.emergency.response.disaster.model.Shelter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.Completable;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.healthchecks.Status;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.healthchecks.HealthCheckHandler;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.handler.BodyHandler;
import io.vertx.reactivex.micrometer.PrometheusScrapingHandler;

public class RestApiVerticle extends AbstractVerticle {

    private static final Logger log = LoggerFactory.getLogger(RestApiVerticle.class);
    private static List<Shelter> shelters = new ArrayList<Shelter>(Arrays.asList(
        new Shelter("123", "Port City Marina", new BigDecimal(-77.9519), new BigDecimal(34.2461), 0)
    ));
    private static DisasterCenter disasterCenter = new DisasterCenter("Wilmington, North Carolina, United States", 
        new BigDecimal(-77.886765), new BigDecimal(34.158808));

    private static List<InclusionZone> inclusionZones = new ArrayList<InclusionZone>(Arrays.asList(
        new InclusionZone("1", Arrays.asList(
            new double[]{-77.95, 34.26},
			new double[]{-77.82, 34.26},
			new double[]{-77.77, 34.24},
			new double[]{-77.812, 34.185},
			new double[]{-77.830, 34.195},
			new double[]{-77.868, 34.134},
			new double[]{-77.885, 34.081},
			new double[]{-77.89, 34.04},
			new double[]{-77.93, 33.96},
			new double[]{-77.919, 34.00},
			new double[]{-77.920, 34.05},
			new double[]{-77.927, 34.12},
			new double[]{-77.914, 34.126},
			new double[]{-77.937, 34.151},
			new double[]{-77.954, 34.190},
			new double[]{-77.95, 34.26}
        ))
    ));

    @Override
    public Completable rxStart() {
        return initializeHttpServer(config());
    }

    private Completable initializeHttpServer(JsonObject config) {

        Router router = Router.router(vertx);

        router.route("/metrics").handler(PrometheusScrapingHandler.create());
        router.route().handler(BodyHandler.create());

        HealthCheckHandler healthCheckHandler = HealthCheckHandler.create(vertx)
                .register("health", f -> f.complete(Status.OK()));
        router.get("/health").handler(healthCheckHandler);

        router.post("/disaster").handler(this::updateDisaster);
        router.get("/shelters").handler(this::getShelters);
        router.get("/inclusion-zones").handler(this::getInclusionZones);
        router.get("/center").handler(this::getDisasterCenter);

        return vertx.createHttpServer()
                .requestHandler(router)
                .rxListen(config.getJsonObject("http").getInteger("port", 8080))
                .ignoreElement();
    }

    private void updateDisaster(RoutingContext rc) {
        log.info("Received message: {}", rc.getBodyAsJson().encodePrettily());

        shelters.clear();
        inclusionZones.clear();

        try {
            rc.getBodyAsJson().getJsonArray("shelters").forEach(shelter -> {
                log.info(((JsonObject)shelter).encodePrettily());
                shelters.add(Json.decodeValue(((JsonObject) shelter).encode(), Shelter.class));
            });

            rc.getBodyAsJson().getJsonArray("inclusionZones").forEach(inclusionZone -> {
                inclusionZones.add(Json.decodeValue(((JsonObject) inclusionZone).encode(), InclusionZone.class));
            });

            disasterCenter = Json.decodeValue(rc.getBodyAsJson().getJsonObject("center").encode(), DisasterCenter.class);
        } catch (DecodeException e) {
            log.error("decoding problem", e);
        }

        rc.response().setStatusCode(200).end();
    }

    private void getShelters(RoutingContext rc) {
        rc.response().setStatusCode(200).end(Json.encode(shelters));
    }

    private void getInclusionZones(RoutingContext rc) {
        rc.response().setStatusCode(200).end(Json.encode(inclusionZones));
    }

    private void getDisasterCenter(RoutingContext rc) {
        rc.response().setStatusCode(200).end(Json.encode(disasterCenter));
    }
}