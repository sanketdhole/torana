package com.phaselume.torana.connector.jdbc;

import com.phaselume.torana.connector.jdbc.query.ResultSetSerializer;
import com.phaselume.torana.connector.jdbc.query.SqlOperationGuard;
import com.phaselume.torana.connector.jdbc.query.SqlQueryExtractor;
import com.phaselume.torana.connector.jdbc.r2dbc.R2dbcConnectionFactory;
import com.phaselume.torana.connector.jdbc.security.SchemaAllowList;
import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.AgentResponse;
import com.phaselume.torana.core.model.ConnectorConfig;
import com.phaselume.torana.core.spi.BackendConnector;
import com.phaselume.torana.core.spi.ResilienceDecorator;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Reactive SQL backend connector powered by R2DBC.
 */
public class R2dbcBackendConnector implements BackendConnector {

    private static final Logger log = LoggerFactory.getLogger(R2dbcBackendConnector.class);

    private final R2dbcConnectionFactory connectionFactoryProvider;
    private final SqlQueryExtractor queryExtractor;
    private final SqlOperationGuard operationGuard;
    private final SchemaAllowList schemaAllowList;
    private final ResultSetSerializer resultSerializer;
    private final ResilienceDecorator resilienceDecorator;

    public R2dbcBackendConnector(R2dbcConnectionFactory connectionFactoryProvider,
                                 SqlQueryExtractor queryExtractor,
                                 SqlOperationGuard operationGuard,
                                 SchemaAllowList schemaAllowList,
                                 ResultSetSerializer resultSerializer,
                                 ResilienceDecorator resilienceDecorator) {
        this.connectionFactoryProvider = connectionFactoryProvider != null ? connectionFactoryProvider : new R2dbcConnectionFactory();
        this.queryExtractor = queryExtractor != null ? queryExtractor : new SqlQueryExtractor();
        this.operationGuard = operationGuard != null ? operationGuard : new SqlOperationGuard();
        this.schemaAllowList = schemaAllowList != null ? schemaAllowList : new SchemaAllowList();
        this.resultSerializer = resultSerializer != null ? resultSerializer : new ResultSetSerializer();
        this.resilienceDecorator = resilienceDecorator;
    }

    public R2dbcBackendConnector() {
        this(new R2dbcConnectionFactory(), new SqlQueryExtractor(), new SqlOperationGuard(), new SchemaAllowList(), new ResultSetSerializer(), null);
    }

    @Override
    public String type() {
        return "r2dbc";
    }

    @Override
    public boolean supports(ConnectorConfig config) {
        if (config == null || config.getType() == null) return false;
        String t = config.getType().toLowerCase();
        return "r2dbc".equals(t) || "jdbc".equals(t) || "sql".equals(t);
    }

    @Override
    public Flux<AgentResponse.Chunk> execute(AgentContext context, ConnectorConfig config) {
        if (config == null) {
            return Flux.error(new IllegalArgumentException("ConnectorConfig must not be null"));
        }

        SqlQueryExtractor.ExtractedQuery extracted = queryExtractor.extract(context);
        String sql = extracted.getSql();
        Map<String, Object> params = extracted.getParameters();

        List<String> allowedOps = extractList(config.getProperties(), "allowedOperations", List.of("SELECT"));
        List<String> allowedTables = extractList(config.getProperties(), "allowedTables", List.of());

        operationGuard.validate(sql, allowedOps);
        schemaAllowList.validate(sql, allowedTables);

        String profileName = getResilienceProfile(config);

        if (resilienceDecorator != null) {
            return resilienceDecorator.decorate(profileName, null, () -> executeQuery(config, sql, params));
        }

        return executeQuery(config, sql, params);
    }

    private Flux<AgentResponse.Chunk> executeQuery(ConnectorConfig config, String sql, Map<String, Object> params) {
        ConnectionFactory factory = connectionFactoryProvider.getFactory(config);

        return Mono.from(factory.create())
                .flatMapMany(connection -> {
                    Statement statement = connection.createStatement(sql);
                    if (params != null && !params.isEmpty()) {
                        for (Map.Entry<String, Object> entry : params.entrySet()) {
                            if (entry.getValue() != null) {
                                try {
                                    statement.bind(entry.getKey(), entry.getValue());
                                } catch (Exception e) {
                                    log.debug("Could not bind named parameter {}: {}", entry.getKey(), e.getMessage());
                                }
                            }
                        }
                    }

                    return Flux.from(statement.execute())
                            .flatMap(resultSerializer::serialize)
                            .doFinally(signal -> Mono.from(connection.close()).subscribe());
                });
    }

    @SuppressWarnings("unchecked")
    private List<String> extractList(Map<String, Object> props, String key, List<String> fallback) {
        if (props == null || !props.containsKey(key)) return fallback;
        Object val = props.get(key);
        if (val instanceof List) {
            return (List<String>) val;
        }
        return fallback;
    }

    private String getResilienceProfile(ConnectorConfig config) {
        if (config != null && config.getProperties() != null && config.getProperties().containsKey("resilienceProfile")) {
            return config.getProperties().get("resilienceProfile").toString();
        }
        return "default";
    }
}
