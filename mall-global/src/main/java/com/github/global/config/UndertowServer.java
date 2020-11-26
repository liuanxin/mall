package com.github.global.config;

import com.github.common.util.U;
import io.undertow.server.DefaultByteBufferPool;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass({ WebSocketDeploymentInfo.class })
public class UndertowServer implements WebServerFactoryCustomizer<UndertowServletWebServerFactory> {

    @Override
    public void customize(UndertowServletWebServerFactory factory) {
        factory.addDeploymentInfoCustomizers(deploymentInfo -> {
            WebSocketDeploymentInfo webSocketDeploymentInfo = new WebSocketDeploymentInfo();
            webSocketDeploymentInfo.setBuffers(new DefaultByteBufferPool(false, U.PROCESSORS << 1 + 1));
            deploymentInfo.addServletContextAttribute(WebSocketDeploymentInfo.class.getName(), webSocketDeploymentInfo);
        });
    }
}
