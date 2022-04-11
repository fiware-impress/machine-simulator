package io.wistefan.simulator.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.http.client.exceptions.ReadTimeoutException;
import io.wistefan.simulator.config.GeneralConfig;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.ngsi.model.EntityFragmentVO;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.ngsi.model.PointVO;
import org.fiware.ngsi.model.PropertyVO;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
@Getter
@ToString
public abstract class AbstractDevice {

	protected static final URL CONTEXT_URL;

	static {
		try {
			CONTEXT_URL = new URL("https://uri.etsi.org/ngsi-ld/v1/ngsi-ld-core-context.jsonld");
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	private final URI id;
	private final ScheduledExecutorService scheduledExecutorService;
	private final EntitiesApiClient entitiesApiClient;
	private final Clock clock;
	private final GeneralConfig generalConfig;
	private boolean isCreated = false;

	protected abstract EntityVO getNgsiEntity();

	protected abstract void runStep();

	protected abstract int getStartOffset();

	protected abstract int getUpdateFrequency();

	protected abstract TimeUnit getUpdateUnit();

	public void startSimulation() {


		scheduledExecutorService.scheduleAtFixedRate(() -> runSimulationStep(), getStartOffset(), getUpdateFrequency(), getUpdateUnit());
	}

	protected PointVO getLocation() {
		double lati = 0.01 * getRandom(0, 100);
		double longi = 0.01 * getRandom(0, 100);
		PointVO location = new PointVO();
		location.type(PointVO.Type.POINT);
		location.coordinates().add(52 + lati);
		location.coordinates().add(13 + longi);
		return location;
	}

	private void runSimulationStep() {
		runStep();
		reportEntityState();
	}

	private void reportEntityState() {
		EntityVO entityVO = getNgsiEntity();
		if (!isCreated) {
			isCreated = true;
			executeRequest(() -> entitiesApiClient.createEntity(entityVO, generalConfig.getTenant()), "Was not able to create entity. Might already exist.");
			return;
		}
		executeRequest(
				() -> entitiesApiClient.updateEntityAttrs(id, entityToEntityFragment(entityVO), generalConfig.getTenant()), "Was not able to update the entity.");
	}

	private void executeRequest(Runnable r, String msg) {
		try {
			r.run();
			log.info("Updated {}", this.id);
		} catch (HttpClientResponseException e) {
			log.warn("{} {}: {}", msg, e.getStatus(), e.getMessage());
		} catch (ReadTimeoutException timeoutException) {
			log.warn("Timeout on update. For device {}", this);
		}
	}

	private EntityFragmentVO entityToEntityFragment(EntityVO entityVO) {
		EntityFragmentVO entityFragmentVO = new EntityFragmentVO();
		entityFragmentVO.setAdditionalProperties(entityVO.getAdditionalProperties());
		return entityFragmentVO
				.atContext(entityVO.atContext())
				.location(entityVO.getLocation())
				.operationSpace(null)
				.observationSpace(null);
	}

	protected Integer getRandom(int min, int max) {
		Random random = new Random();
		return random.ints(min, max)
				.findFirst()
				.getAsInt();
	}

	protected Double getRandomD(Double min, Double max) {
		Random random = new Random();
		return random.doubles(min, max).findFirst().getAsDouble();
	}

	protected PropertyVO asProperty(Object value, Date observedAt) {
		return new PropertyVO().observedAt(observedAt).value(value).type(PropertyVO.Type.PROPERTY);
	}
}
