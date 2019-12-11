package com.example.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.PrincipalNameKeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@SpringBootApplication
public class GatewayApplication {

  @Bean
  RedisRateLimiter redisRateLimiter() {
    return new RedisRateLimiter(5, 7);
  }

  @Bean
  MapReactiveUserDetailsService authentication() {
    return new MapReactiveUserDetailsService(
        User.withDefaultPasswordEncoder().username("jlong").password("pw").roles("USER").build());
  }

  @Bean
  SecurityWebFilterChain authorization(ServerHttpSecurity http) {
    return http
        .csrf(ServerHttpSecurity.CsrfSpec::disable)
        .httpBasic(Customizer.withDefaults())
        .authorizeExchange(x -> x
            .pathMatchers("/proxy").authenticated()
            .anyExchange().permitAll())
        .build();
  }

  @Bean
  RouteLocator gateway(RouteLocatorBuilder rlb) {
    return rlb
        .routes()
        .route(rSpec -> rSpec
            .path("/proxy").and().host("*.spring.io")
            .filters(fSpec -> fSpec
                .setPath("/reservations")
                .requestRateLimiter(rlSpec -> rlSpec
                    .setRateLimiter(this.redisRateLimiter())
                    .setKeyResolver(new PrincipalNameKeyResolver()))
            )
            .uri("http://localhost:8080"))
        .build();
  }

  public static void main(String[] args) {
    SpringApplication.run(GatewayApplication.class, args);
  }

}
