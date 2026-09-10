package com.phaselume.torana.connector.jdbc.r2dbc;

import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.spi.ConnectionFactory;

import java.time.Duration;

/**
 * Creates pooled ConnectionFactory instances using r2dbc-pool.
 */
public class R2dbcConnectionPool {

    public static ConnectionPool createPool(ConnectionFactory baseFactory, int maxSize, int minIdle, Duration maxIdleTime) {
        ConnectionPoolConfiguration configuration = ConnectionPoolConfiguration.builder(baseFactory)
                .maxSize(maxSize > 0 ? maxSize : 10)
                .initialSize(minIdle >= 0 ? minIdle : 2)
                .maxIdleTime(maxIdleTime != null ? maxIdleTime : Duration.ofMinutes(5))
                .build();

        return new ConnectionPool(configuration);
    }
}
