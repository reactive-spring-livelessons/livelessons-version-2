package com.example.rsocketclient;

import greetings.GreetingRequest;
import greetings.GreetingResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.rsocket.messaging.RSocketStrategiesCustomizer;
import org.springframework.cloud.gateway.rsocket.client.BrokerClient;
import org.springframework.context.ApplicationListener;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.security.rsocket.metadata.BasicAuthenticationEncoder;
import org.springframework.security.rsocket.metadata.UsernamePasswordMetadata;

import static org.springframework.security.rsocket.metadata.UsernamePasswordMetadata.BASIC_AUTHENTICATION_MIME_TYPE;

@Log4j2
@SpringBootApplication
public class RsocketClientApplication {

  public static void main(String[] args) {
    SpringApplication.run(RsocketClientApplication.class, args);
  }

  @Bean
  RSocketStrategiesCustomizer rSocketStrategiesCustomizer() {
    return strategies -> strategies.encoder(new BasicAuthenticationEncoder());
  }

  @Bean
  RSocketRequester rSocketRequester(RSocketRequester.Builder builder ) {
    return builder
        .connectTcp("localhost", 8888)
        .block();
  }

  @Bean
  ApplicationListener<ApplicationReadyEvent> secureClient(RSocketRequester localhost) {
    return event -> {
      var credentials = new UsernamePasswordMetadata("user", "password");
      localhost
          .route("greeting")
          .metadata(credentials, BASIC_AUTHENTICATION_MIME_TYPE)
          .retrieveMono(String.class)
          .subscribe(gr -> log.info("secure response: " + gr));
    };
  }

  @Bean
  ApplicationListener<ApplicationReadyEvent> client(RSocketRequester.Builder builder) {
    return event ->
        builder
            .connectTcp("localhost", 8888)
            .block()
            .route("greetings")
            .data(new GreetingRequest("Livelessons"))
            .retrieveFlux(GreetingResponse.class)
            .subscribe(gr -> log.info("rsocket client: " + gr.getMessage()));
  }

  @Bean
  ApplicationListener<PayloadApplicationEvent<RSocketRequester>> gatewayClient(BrokerClient client) {
    return event ->
        event
            .getPayload()
            .route("greetings")
            .metadata(client.forwarding("greetings-service"))
            .data(new GreetingRequest("World"))
            .retrieveFlux(GreetingResponse.class)
            .subscribe(gr -> log.info("gateway rsocket client: " + gr.getMessage()));
  }
}

