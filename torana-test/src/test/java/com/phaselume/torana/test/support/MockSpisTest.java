package com.phaselume.torana.test.support;

import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.AgentResponse;
import com.phaselume.torana.core.model.ConnectorConfig;
import com.phaselume.torana.core.model.ToranaAuthentication;
import com.phaselume.torana.test.fixtures.AgentContextFixtures;
import com.phaselume.torana.test.fixtures.ToranaAuthenticationFixtures;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MockSpisTest {

    @Test
    void testMockAuthenticationProvider() {
        MockAuthenticationProvider authProvider = new MockAuthenticationProvider("jwt");
        ToranaAuthentication alice = ToranaAuthenticationFixtures.aliceUser();
        authProvider.returns(alice);

        StepVerifier.create(authProvider.authenticate(null))
                .expectNext(alice)
                .verifyComplete();

        assertEquals(1, authProvider.getInvocationCount());

        authProvider.fails();
        StepVerifier.create(authProvider.authenticate(null))
                .verifyComplete();
    }

    @Test
    void testMockBackendConnector() {
        MockBackendConnector connector = new MockBackendConnector("mock-llm");
        connector.returnsText("Response text");

        assertTrue(connector.supports(ConnectorConfig.builder().type("mock-llm").build()));
        assertFalse(connector.supports(ConnectorConfig.builder().type("other").build()));

        AgentContext ctx = AgentContextFixtures.authenticatedContext();
        List<AgentResponse.Chunk> chunks = connector.execute(ctx, null).collectList().block();

        assertNotNull(chunks);
        assertEquals(2, chunks.size());
        assertEquals("Response text", chunks.get(0).getTextDelta());
        assertTrue(chunks.get(1).isLast());
        assertEquals(1, connector.getExecutionCount());
    }

    @Test
    void testMockPipelineStep() {
        MockPipelineStep step = new MockPipelineStep("test-step");
        step.setsAttribute("tested", true);

        AgentContext ctx = AgentContextFixtures.authenticatedContext();
        AgentContext result = step.execute(ctx).block();

        assertNotNull(result);
        assertEquals(true, result.getAttribute("tested"));
        assertEquals(1, step.getExecutionCount());
        assertEquals(1, step.getRecordedContexts().size());
    }
}
