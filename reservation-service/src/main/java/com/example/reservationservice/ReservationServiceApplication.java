package com.example.reservationservice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

/*
 * This demonstrates reactive NoSQL, reative SQL, traditional HTTP endpoints, and more.
 */
@SpringBootApplication
public class ReservationServiceApplication {

  @Bean
  RouterFunction<ServerResponse> routes(ReservationRepository rr) {
    return route()
        .GET("/reservations", serverRequest -> ok().body(rr.findAll(), Reservation.class))
        .build();
  }

  public static void main(String[] args) {
    SpringApplication.run(ReservationServiceApplication.class, args);
  }

}

@Log4j2
@Component
@RequiredArgsConstructor
class Initializer {

  private final ReservationRepository reservationRepository;

  @EventListener(ApplicationReadyEvent.class)
  public void ready() {

    var names = Flux
        .just("A", "B", "C", "D")
        .map(name -> new Reservation(null, name))
        .flatMap(this.reservationRepository::save);

    this.reservationRepository
        .deleteAll()
        .thenMany(names)
        .thenMany(this.reservationRepository.findAll())
        .subscribe(log::info);
  }
}

interface ReservationRepository extends ReactiveCrudRepository<Reservation, Integer> {
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class Reservation {

  @Id
  private Integer id;
  private String name;
}