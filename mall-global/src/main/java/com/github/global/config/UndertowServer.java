//package com.github.global.config;
//
//import io.undertow.server.DefaultByteBufferPool;
//import io.undertow.websockets.jsr.WebSocketDeploymentInfo;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
//import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
//import org.springframework.boot.web.server.WebServerFactoryCustomizer;
//import org.springframework.context.annotation.Configuration;
//
///**
// * In Spring Boot 2,
// *   the EmbeddedServletContainerCustomizer interface is replaced by WebServerFactoryCustomizer,
// *   while the ConfigurableEmbeddedServletContainer class is replaced with ConfigurableServletWebServerFactory.
// */
//@Configuration
//@ConditionalOnClass({ WebSocketDeploymentInfo.class })
//public class UndertowServer implements WebServerFactoryCustomizer<UndertowServletWebServerFactory> {
//
//    @Override
//    public void customize(UndertowServletWebServerFactory undertow) {
//        undertow.addDeploymentInfoCustomizers(deploymentInfo -> {
//            WebSocketDeploymentInfo webSocketDeploymentInfo = new WebSocketDeploymentInfo();
//            int processors = Runtime.getRuntime().availableProcessors();
//            webSocketDeploymentInfo.setBuffers(new DefaultByteBufferPool(false, (processors << 1) + 1));
//            deploymentInfo.addServletContextAttribute(WebSocketDeploymentInfo.class.getName(), webSocketDeploymentInfo);
//        });
//    }
//}
