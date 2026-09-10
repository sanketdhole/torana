package com.phaselume.torana.connector.jdbc.r2dbc;

import com.phaselume.torana.core.model.ConnectorConfig;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages ConnectionFactory instances cached per connector configuration.
 */
public class R2dbcConnectionFactory {

    private static final Logger log = LoggerFactory.getLogger(R2dbcConnectionFactory.class);

    private final Map<String, ConnectionFactory> factoryCache = new ConcurrentHashMap<>();

    public ConnectionFactory getFactory(ConnectorConfig config) {
        String id = (config != null && config.getId() != null) ? config.getId() : "default-r2dbc";
        return factoryCache.computeIfAbsent(id, k -> createFactory(config));
    }

    public ConnectionFactory createFactory(ConnectorConfig config) {
        Map<String, Object> props = (config != null && config.getProperties() != null)
                ? config.getProperties()
                : Map.of();

        String driver = props.getOrDefault("driver", "postgresql").toString();
        String host = props.getOrDefault("host", "localhost").toString();
        int port = parsePort(props.get("port"), driver);
        String database = props.getOrDefault("database", "postgres").toString();
        String user = props.getOrDefault("user", props.getOrDefault("username", "postgres")).toString();
        String password = props.getOrDefault("password", "").toString();

        ConnectionFactoryOptions options = ConnectionFactoryOptions.builder()
                .option(ConnectionFactoryOptions.DRIVER, driver)
                .option(ConnectionFactoryOptions.HOST, host)
                .option(ConnectionFactoryOptions.PORT, port)
                .option(ConnectionFactoryOptions.DATABASE, database)
                .option(ConnectionFactoryOptions.USER, user)
                .option(ConnectionFactoryOptions.PASSWORD, password)
                .build();

        ConnectionFactory baseFactory = ConnectionFactories.get(options);

        int poolSize = parseInt(props.get("poolSize"), 10);
        int minIdle = parseInt(props.get("minIdle"), 2);

        log.info("Initialized pooled R2DBC factory for driver={}, host={}, database={}", driver, host, database);
        return R2dbcConnectionPool.createPool(baseFactory, poolSize, minIdle, Duration.ofMinutes(5));
    }

    private int parsePort(Object portObj, String driver) {
        if (portObj != null) {
            try {
                return Integer.parseInt(portObj.toString());
            } catch (NumberFormatException ignored) {}
        }
        if ("mysql".equalsIgnoreCase(driver)) return 3306;
        if ("mssql".equalsIgnoreCase(driver)) return 1433;
        if ("oracle".equalsIgnoreCase(driver)) return 1521;
        return 5432; // Default postgres
    }

    private int parseInt(Object val, int fallback) {
        if (val != null) {
            try {
                return Integer.parseInt(val.toString());
            } catch (NumberFormatException ignored) {}
        }
        return fallback;
    }
}
