package com.example.greetingservice;

/*
 * This demo supports server-sent events, websockets and RSocket
 */

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Stream;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@SpringBootApplication
public class GreetingServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(GreetingServiceApplication.class, args);
  }

  @Bean
  RouterFunction<ServerResponse> routes(GreetingService greetingService) {
    return route()
        .GET("/greetings/{name}", request -> {
          var name = request.pathVariable("name");
          var greetingRequest = new GreetingRequest(name);
          var greet = greetingService.greet(greetingRequest);
          return ServerResponse
              .ok()
              .contentType(MediaType.TEXT_EVENT_STREAM)
              .body(greet, GreetingResponse.class);
        })
        .build();
  }

}


@Configuration
class WebSocketConfiguration {

  @Bean
  SimpleUrlHandlerMapping simpleUrlHandlerMapping(WebSocketHandler wsh) {
    return new SimpleUrlHandlerMapping(Map.of("/ws/greetings", wsh), 10);
  }

  @Bean
  WebSocketHandler webSocketHandler(GreetingService gs) {
    return session -> {
      var responses = session
          .receive()
          .map(WebSocketMessage::getPayloadAsText)
          .map(GreetingRequest::new)
          .flatMap(gs::greet)
          .map(GreetingResponse::getMessage)
          .map(session::textMessage);
      return session.send(responses);
    };
  }

  @Bean
  WebSocketHandlerAdapter webSocketHandlerAdapter() {
    return new WebSocketHandlerAdapter();
  }
}


@Data
@AllArgsConstructor
@NoArgsConstructor
class GreetingResponse {
  private String message;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class GreetingRequest {
  private String name;
}

@Log4j2
@Controller
class GreetingService {

  @MessageMapping("greetings")
  Flux<GreetingResponse> greet(GreetingRequest request) {
    return Flux
        .fromStream(Stream.generate(() -> new GreetingResponse("Hello " + request.getName() + " @ " + Instant.now())))
        .delayElements(Duration.ofSeconds(1));
  }


  @MessageMapping("error-signal")
  Mono<String> handleAndReturnError(String payload) {
    return Mono.error(new IllegalArgumentException("Invalid input error"));
  }

  @MessageExceptionHandler(IllegalArgumentException.class)
  public Mono<String> onIllegalArgumentException(
      IllegalArgumentException iae) {
    log.error(iae);
    return Mono.just("OoOps!");
  }

}