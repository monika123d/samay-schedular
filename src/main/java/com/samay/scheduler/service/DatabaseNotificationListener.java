package com.samay.scheduler.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.samay.scheduler.event.DataChangeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A service that runs in the background to listen for PostgreSQL notifications on a specific channel.
 * When a notification is received (sent by a database trigger), it parses the payload
 * and publishes a corresponding Spring ApplicationEvent. This allows the application to react
 * to data changes made by any process, including those made directly in the database.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseNotificationListener implements InitializingBean, DisposableBean {

    private static final String CHANNEL_NAME = "samay_events_channel";

    private final DataSource dataSource;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile boolean running = true;

    /**
     * This method is called by Spring once the bean has been constructed and all properties are set.
     * It's part of the InitializingBean interface and is used here to start the background listener thread.
     */
    @Override
    public void afterPropertiesSet() {
        executor.submit(this::listenForNotifications);
    }

    /**
     * The main loop that runs in the background thread. It maintains a connection to the database,
     * listens for notifications, and handles connection errors by retrying.
     */
    private void listenForNotifications() {
        //log.info("Starting PostgreSQL notification listener for channel '{}'...", CHANNEL_NAME);
        while (running) {
            try (Connection connection = dataSource.getConnection()) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("LISTEN " + CHANNEL_NAME);
                }

                PGConnection pgConnection = connection.unwrap(PGConnection.class);

                while (running) {
                    PGNotification[] notifications = pgConnection.getNotifications(10000); // 10-second timeout

                    if (notifications != null) {
                        for (PGNotification notification : notifications) {
                            handleNotification(notification);
                        }
                    }
                }
            } catch (SQLException e) {
                log.error("Database connection error in notification listener. Retrying in 10 seconds...", e);
                sleepFor(10000); // Wait before attempting to reconnect
            } catch (Exception e) {
                log.error("An unexpected error occurred in the notification listener. Retrying in 10 seconds...", e);
                sleepFor(10000);
            }
        }
        log.info("PostgreSQL notification listener stopped.");
    }

    /**
     * Handles a single notification received from the database by parsing its payload
     * and publishing a native Spring DataChangeEvent.
     *
     * @param notification The notification object from the PostgreSQL JDBC driver.
     */
    private void handleNotification(PGNotification notification) {
        String payload = notification.getParameter();
        log.info("[DB Notification] Received payload on channel '{}': {}", notification.getName(), payload);

        try {
            JsonNode root = objectMapper.readTree(payload);

            String tableName = root.path("tableName").asText();
            String changedField = root.path("changedField").asText();
            JsonNode newValueNode = root.path("newValue"); // Use path() to get the JsonNode

            String newValue = newValueNode.asText();

            if (tableName.isEmpty() || changedField.isEmpty()) {
                log.warn("Received notification with missing tableName or changedField. Payload: {}", payload);
                return;
            }

            DataChangeEvent appEvent = new DataChangeEvent(
                    this,
                    tableName,
                    changedField,
                    newValue
            );
            eventPublisher.publishEvent(appEvent);
            log.info("Successfully published Spring DataChangeEvent for '{}.{}' update to '{}'",
                    tableName, changedField, newValue);

        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON payload from notification: {}", payload, e);
        }
    }

    /**
     * A utility method to pause the current thread.
     * @param millis The number of milliseconds to sleep.
     */
    private void sleepFor(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * This method is called by Spring when the application context is being closed.
     * It's part of the DisposableBean interface and ensures our background thread is stopped gracefully.
     */
    @Override
    public void destroy() {
        log.info("Shutting down PostgreSQL notification listener...");
        running = false;
        executor.shutdownNow();
    }
}