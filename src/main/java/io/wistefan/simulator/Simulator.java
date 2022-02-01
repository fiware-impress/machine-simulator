package io.wistefan.simulator;

import io.micronaut.context.annotation.Context;
import io.wistefan.simulator.config.SimulatorConfiguration;
import io.wistefan.simulator.model.Crane;
import io.wistefan.simulator.model.LiftingCompany;
import lombok.RequiredArgsConstructor;
import org.fiware.ngsi.api.EntitiesApiClient;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.time.Clock;
import java.util.concurrent.ScheduledExecutorService;

@Context
@RequiredArgsConstructor
public class Simulator {

	private static final String ID_TEMPLATE = "urn:ngsi-ld:%s:%s";
	private final ScheduledExecutorService scheduledExecutorService;
	private final EntitiesApiClient entitiesApiClient;
	private final Clock clock;
	private final SimulatorConfiguration simulatorConfiguration;

	@PostConstruct
	public void simulate() {

		simulatorConfiguration.getCompanies().stream().forEach(company -> {
			URI companyID = URI.create(String.format(ID_TEMPLATE, "company", company.getName()));
			LiftingCompany companySimulator = new LiftingCompany(
					companyID,
					scheduledExecutorService,
					entitiesApiClient,
					clock, company.getName());
			company.cranes.stream().forEach(c -> {
				Crane crane = new Crane(
						URI.create(String.format(ID_TEMPLATE, "crane", c.getName())),
						scheduledExecutorService,
						entitiesApiClient,
						clock);
				crane.setCurrentCompany(companyID);
				crane.startSimulation();
			});
			companySimulator.startSimulation();
		});
	}
}
