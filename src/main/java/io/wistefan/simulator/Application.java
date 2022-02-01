package io.wistefan.simulator;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.runtime.Micronaut;

import java.time.Clock;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Factory
public class Application {

	public static void main(String[] args) {
		Micronaut.run(Application.class, args);
	}

	@Bean
	public ScheduledExecutorService simulationExecutor() {
		return Executors.newScheduledThreadPool(1);
	}

	@Bean
	public Clock clock() {
		return Clock.systemUTC();
	}

}
