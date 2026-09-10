package com.phaselume.torana.connector.nfs.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phaselume.torana.connector.nfs.NfsBackendConnector;
import com.phaselume.torana.connector.nfs.buffer.ChunkedFileReader;
import com.phaselume.torana.connector.nfs.buffer.ContentTypeDetector;
import com.phaselume.torana.connector.nfs.buffer.VirtualThreadExecutor;
import com.phaselume.torana.connector.nfs.handler.FileListHandler;
import com.phaselume.torana.connector.nfs.handler.FileReadHandler;
import com.phaselume.torana.connector.nfs.handler.NfsOperationGuard;
import com.phaselume.torana.connector.nfs.handler.NfsPathValidator;
import com.phaselume.torana.core.spi.BackendConnector;
import com.phaselume.torana.core.spi.ResilienceDecorator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for NFS / File Storage Backend Connector module.
 */
@AutoConfiguration
public class ToranaConnectorNfsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public VirtualThreadExecutor virtualThreadExecutor() {
        return new VirtualThreadExecutor();
    }

    @Bean
    @ConditionalOnMissingBean
    public ContentTypeDetector contentTypeDetector() {
        return new ContentTypeDetector();
    }

    @Bean
    @ConditionalOnMissingBean
    public ChunkedFileReader chunkedFileReader(VirtualThreadExecutor executor, ContentTypeDetector contentTypeDetector) {
        return new ChunkedFileReader(executor, contentTypeDetector);
    }

    @Bean
    @ConditionalOnMissingBean
    public NfsPathValidator nfsPathValidator() {
        return new NfsPathValidator();
    }

    @Bean
    @ConditionalOnMissingBean
    public NfsOperationGuard nfsOperationGuard() {
        return new NfsOperationGuard();
    }

    @Bean
    @ConditionalOnMissingBean
    public FileReadHandler fileReadHandler(ChunkedFileReader chunkedFileReader) {
        return new FileReadHandler(chunkedFileReader);
    }

    @Bean
    @ConditionalOnMissingBean
    public FileListHandler fileListHandler(ContentTypeDetector contentTypeDetector, ObjectProvider<ObjectMapper> objectMapperProvider) {
        return new FileListHandler(contentTypeDetector, objectMapperProvider.getIfAvailable(ObjectMapper::new));
    }

    @Bean
    @ConditionalOnMissingBean(name = "nfsBackendConnector")
    public BackendConnector nfsBackendConnector(NfsPathValidator pathValidator,
                                                NfsOperationGuard operationGuard,
                                                FileReadHandler fileReadHandler,
                                                FileListHandler fileListHandler,
                                                ObjectProvider<ResilienceDecorator> resilienceDecoratorProvider) {
        return new NfsBackendConnector(
                pathValidator,
                operationGuard,
                fileReadHandler,
                fileListHandler,
                resilienceDecoratorProvider.getIfAvailable()
        );
    }
}
