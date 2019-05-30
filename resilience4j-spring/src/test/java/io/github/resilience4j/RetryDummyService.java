package io.github.resilience4j;

import java.util.concurrent.CompletionStage;

import org.springframework.stereotype.Component;

import io.github.resilience4j.retry.annotation.Retry;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class RetryDummyService implements TestDummyService {
	@Override
	@Retry(name = BACKEND, fallbackMethod = "recovery")
	public String sync() {
		return syncError();
	}

	@Override
	public CompletionStage<String> asyncThreadPool() {
		// no-op
		return null;
	}

	@Override
	public CompletionStage<String> asyncThreadPoolSuccess() {
		// no-op
		return null;
	}

	@Override
	@Retry(name = BACKEND, fallbackMethod = "completionStageRecovery")
	public CompletionStage<String> async() {
		return asyncError();
	}

	@Override
	@Retry(name = BACKEND, fallbackMethod = "fluxRecovery")
	public Flux<String> flux() {
		return fluxError();
	}

	@Override
	@Retry(name = BACKEND, fallbackMethod = "monoRecovery")
	public Mono<String> mono(String parameter) {
		return monoError(parameter);
	}

	@Override
	@Retry(name = BACKEND, fallbackMethod = "observableRecovery")
	public Observable<String> observable() {
		return observableError();
	}

	@Override
	@Retry(name = BACKEND, fallbackMethod = "singleRecovery")
	public Single<String> single() {
		return singleError();
	}

	@Override
	@Retry(name = BACKEND, fallbackMethod = "completableRecovery")
	public Completable completable() {
		return completableError();
	}

	@Override
	@Retry(name = BACKEND, fallbackMethod = "maybeRecovery")
	public Maybe<String> maybe() {
		return maybeError();
	}

	@Override
	@Retry(name = BACKEND, fallbackMethod = "flowableRecovery")
	public Flowable<String> flowable() {
		return flowableError();
	}
}
