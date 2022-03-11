package io.wistefan.simulator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.annotation.Context;
import io.wistefan.simulator.config.SimulatorConfiguration;
import io.wistefan.simulator.model.Crane;
import io.wistefan.simulator.model.LiftingCompany;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fiware.ngsi.api.EntitiesApiClient;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.time.Clock;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;

@Slf4j
@Context
@RequiredArgsConstructor
public class Simulator {

	private static final String ID_TEMPLATE = "urn:ngsi-ld:%s:%s";
	private final ScheduledExecutorService scheduledExecutorService;
	private final EntitiesApiClient entitiesApiClient;
	private final Clock clock;
	private final SimulatorConfiguration simulatorConfiguration;

	private final ObjectMapper objectMapper;

	@PostConstruct
	public void simulate() {

		try {
			log.info(objectMapper.writeValueAsString(clock.instant()));
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
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
						clock,
						Optional.ofNullable(c.getLatitude()),
						Optional.ofNullable(c.getLongitude()),
						c.getMaxHookHeight(),c.getMaxLiftingWeight(), c.getPayloadAtTip(), c.getModel());
				crane.setCurrentCustomer(companyID);
				crane.startSimulation();
			});
			companySimulator.startSimulation();
		});
	}
}
