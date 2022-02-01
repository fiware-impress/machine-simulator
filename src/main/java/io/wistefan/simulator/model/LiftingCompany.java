package io.wistefan.simulator.model;

import lombok.ToString;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.ngsi.model.GeoPropertyVO;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.time.ZoneOffset.UTC;

@ToString
public class LiftingCompany extends AbstractDevice {

	private final String name;
	private List<URI> grinders = new ArrayList<>();

	public LiftingCompany(URI id, ScheduledExecutorService scheduledExecutorService, EntitiesApiClient entitiesApiClient, Clock clock, String name) {
		super(id, scheduledExecutorService, entitiesApiClient, clock);
		this.name = name;
	}

	public void addGrinder(URI grinder) {
		grinders.add(grinder);
	}

	public void removeGrinder(URI grinder) {
		grinders.remove(grinder);
	}

	@Override
	protected EntityVO getNgsiEntity() {
		Date observedAt = Date.from(getClock().instant());
		GeoPropertyVO location = new GeoPropertyVO().observedAt(observedAt).type(GeoPropertyVO.Type.GEOPROPERTY).value(getLocation());
		EntityVO ngsiEntity = new EntityVO().atContext(CONTEXT_URL).id(getId()).location(location).type("company").operationSpace(null).observationSpace(null);
		ngsiEntity.setAdditionalProperties(Map.of("name", asProperty(name, observedAt)));
		return ngsiEntity;
	}

	@Override
	protected void runStep() {
		//nothing to do here
	}

	@Override
	protected int getStartOffset() {
		return 0;
	}

	@Override
	protected int getUpdateFrequency() {
		return 1;
	}

	@Override
	protected TimeUnit getUpdateUnit() {
		return TimeUnit.HOURS;
	}
}
