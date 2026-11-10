package com.haui.notification.resource;

import com.haui.notification.consumer.OrderNotificationConsumer;
import com.haui.notification.model.NotificationRecord;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Path("/api/notifications")
@Produces(MediaType.APPLICATION_JSON)
public class NotificationResource {

    @Inject
    OrderNotificationConsumer notificationConsumer;

    @GET
    public List<NotificationRecord> getNotifications() {
        return notificationConsumer.getRecentNotifications();
    }

    @GET
    @Path("/status")
    public Response getStatus() {
        return Response.ok(Map.of(
                "service", "notification-service",
                "framework", "Quarkus 3.15 LTS (Supersonic Subatomic Java)",
                "status", "UP",
                "timestamp", Instant.now(),
                "description", "Reactive Messaging AMQP Consumer listening on RabbitMQ queue 'order.created'"
        )).build();
    }
}
