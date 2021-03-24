package io.wistefan.simulator;

import io.micronaut.context.annotation.Context;
import io.wistefan.simulator.config.SimulatorConfiguration;
import io.wistefan.simulator.model.CNCVerticalGrinder;
import io.wistefan.simulator.model.GrindingCompany;
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
			GrindingCompany companySimulator = new GrindingCompany(
					companyID,
					scheduledExecutorService,
					entitiesApiClient,
					clock, company.getName());
			company.grinders.stream().forEach(grinder -> {
				CNCVerticalGrinder cncVerticalGrinder = new CNCVerticalGrinder(
						URI.create(String.format(ID_TEMPLATE, "grinder", grinder.getName())),
						scheduledExecutorService,
						entitiesApiClient,
						clock);
				cncVerticalGrinder.setCurrentCompany(companyID);
				cncVerticalGrinder.startSimulation();
			});
			companySimulator.startSimulation();
		});
	}
}
