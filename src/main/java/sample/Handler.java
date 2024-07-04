package sample;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class Handler implements ApplicationRunner {
	private final WebClient.Builder webClientBuilder;

	@Override
	public void run(ApplicationArguments args) throws Exception {
		Hooks.enableAutomaticContextPropagation();
		emulatePubsubReceiver();
	}

	private void emulatePubsubReceiver() {
//		Flux.just("A", "B", "C")
		Flux.just("A")
				.map(s -> {
					MDC.put("traceId", s);
					return s;
				})
				.doOnNext(next -> log.info("(main) in doOnNext before doSomething: {}", next))
				.flatMap(this::doSomething)
				.doOnNext(next -> log.info("(main) in doOnNext before doRequest: {}", next))
				.flatMap(this::doRequest)
				.doOnNext(next -> log.info("(main) in doOnNext after doRequest: {}", next))
				.blockLast();
	}

	private Mono<String> doSomething(final String string) {
		return Mono.just(string)
				.delayElement(Duration.ofSeconds(1))
				.map(s -> {
					log.info("(doSomething) in map: {}", s);
					return s;
				})
				.doOnNext(next -> log.info("(doSomething) in doOnNext: {}", next));
	}

	private Mono<String> doRequest(final String string) {
		log.info("(doRequest) before WebClient exchange: {}", string);
		return webClientBuilder
				.build()
				.get()
				.uri(URI.create("https://google.be"))
				.exchangeToMono(clientResponse -> {
					log.info("(doRequest) in exchangeToMono: {}", clientResponse.statusCode());
					return Mono.just(string)
							.doOnNext(next -> log.info("(doRequest) in doOnNext: {}", next));
				});
	}
}
