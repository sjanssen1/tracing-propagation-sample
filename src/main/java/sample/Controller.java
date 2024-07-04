package sample;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@Slf4j
public class Controller {

	private final WebClient.Builder webClientBuilder;

	@GetMapping(path = "/hello")
	public Mono<String> hello() {
		return Mono.just("Hello")
				.doOnNext(next -> log.info("(main) doOnNext before doRequest: {}", MDC.getCopyOfContextMap()))
				.flatMap(this::doRequest)
				.doOnNext(next -> log.info("(main) doOnNext after doRequest: {}", MDC.getCopyOfContextMap()));
	}

	public Mono<String> doRequest(final String hello) {
		return webClientBuilder
				.build()
				.get()
				.uri(URI.create("https://google.be"))
				.exchangeToMono(clientResponse -> {
					log.info("(doRequest) in exchangeToMono: {}", clientResponse.statusCode());
					return Mono.just(hello);
				})
				.doOnNext(next -> log.info("(doRequest) doOnNext: {}", next));
	}
}
