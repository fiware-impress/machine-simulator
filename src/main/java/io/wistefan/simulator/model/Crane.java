package io.wistefan.simulator.model;

import io.wistefan.simulator.config.GeneralConfig;
import lombok.Setter;
import lombok.ToString;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.ngsi.model.GeoPropertyVO;
import org.fiware.ngsi.model.PointVO;
import org.fiware.ngsi.model.PropertyVO;
import org.fiware.ngsi.model.RelationshipVO;

import java.net.URI;
import java.time.Clock;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@ToString
public class Crane extends AbstractDevice {

	/*
		This is a crane.
		When the crane becomes active lifting a weight is in process.
		Based on the weight the maximum speed of the hook can be calculated (max 2 m/s at no load).
		Based on the hookspeed the amps, volts and rpm of the engine can be calculated (max 1500 rpm, 52 Amps and min 225V).
		In a step a delta_height is calculated based on a random value with the maximum speed (step is 1 second).
		If the cranes hook is at it's top position no further lifting is done, but the crane stays active until the step is done.
		There is a chance the crane has a problem with the engine, causing vibration. Thus the v_rms can be higher in a lifting process.
	*/

	private Integer currentStepNr = 0;
	private Integer currentLength = 0;

	private final String softwareVersion = "0.0.1";
	private boolean active = false;
	private double currentHookHeight = 0.0;
	private boolean malfunction = false;

	private final double maxHookHeight;
	private final double maxLiftingWeight;
	// compatibility
	private final double maxPayload;
	private final double payloadAtTip;
	private final String model;

	private double lat = 51.24752;
	private double longi = 13.87789;

	@Setter
	private URI currentCustomer;
	private Lifting lifting;
	private ElectricMotor elMotor = new ElectricMotor(1500.0, 240.0, 52.0, 1.0);


	public Crane(URI id, ScheduledExecutorService scheduledExecutorService, EntitiesApiClient entitiesApiClient, Clock clock, GeneralConfig generalConfig, Optional<Double> optionalLat, Optional<Double> optionalLongi, double maxHookHeight, double maxLiftingWeight, double payloadAtTip, String model) {
		super(id, scheduledExecutorService, entitiesApiClient, clock, generalConfig);
		this.maxHookHeight = maxHookHeight;
		this.maxLiftingWeight = maxLiftingWeight;
		this.maxPayload = maxLiftingWeight;
		this.payloadAtTip = payloadAtTip;
		this.model = model;
		optionalLat.ifPresent(olv -> lat = olv);
		optionalLongi.ifPresent(olv -> longi = olv);
	}

	@Override
	protected int getStartOffset() {
		return getRandom(0, 30);
	}

	@Override
	protected int getUpdateFrequency() {
		return 1;
	}

	@Override
	protected TimeUnit getUpdateUnit() {
		return TimeUnit.SECONDS;
	}

	@Override
	protected void runStep() {
		if (currentLength == 0 || currentLength.equals(currentStepNr)) {
			// generate length of next on/off period.
			currentLength = getRandom(10, 20);
			// reset step to 0
			currentStepNr = 0;
			active = !active;
			startStep();
		} else {
			runCurrentStep();
		}
		currentStepNr++;
	}

	@Override
	protected PointVO getLocation() {
		PointVO pointVO = new PointVO();
		pointVO.type(PointVO.Type.POINT);
		pointVO.coordinates().add(lat);
		pointVO.coordinates().add(longi);
		return pointVO;
	}

	@Override
	protected EntityVO getNgsiEntity() {
		Date observedAt = Date.from(getClock().instant());
		GeoPropertyVO location = new GeoPropertyVO().observedAt(observedAt).type(GeoPropertyVO.Type.GEOPROPERTY).value(getLocation());
		EntityVO ngsiEntity = new EntityVO().atContext(CONTEXT_URL).id(getId()).location(location).type("crane").observationSpace(null).operationSpace(null);
		Map<String, Object> additionalProperties = new HashMap<>();

		additionalProperties.put("softwareVersion", asProperty(softwareVersion, observedAt));
		additionalProperties.put("active", asProperty(active, observedAt));
		additionalProperties.put("maxHookHeight", asProperty(maxHookHeight, observedAt));
		additionalProperties.put("maxLiftingWeight", asProperty(maxLiftingWeight, observedAt));
		additionalProperties.put("maxPayLoad", asProperty(maxPayload, observedAt));
		additionalProperties.put("currentHookHeight", asProperty(currentHookHeight, observedAt));
		additionalProperties.put("model", asProperty(model, observedAt));


		if (currentCustomer != null) {
			RelationshipVO companyRelationshipVO = new RelationshipVO().observedAt(observedAt).type(RelationshipVO.Type.RELATIONSHIP)._object(currentCustomer);
			additionalProperties.put("currentCustomer", companyRelationshipVO);
		}
		if (lifting != null) {
			PropertyVO liftingWeightVO = asProperty(lifting.getWeight(), observedAt);
			additionalProperties.put("currentWeight", liftingWeightVO);
		}
		//======== Electric Motor:
		PropertyVO elMotorActiveVO = asProperty(elMotor.getActive(), observedAt);
		PropertyVO elMotorRpmVO = asProperty(elMotor.getRpm(), observedAt);
		PropertyVO elMotorU1VO = asProperty(elMotor.getU1(), observedAt);
		PropertyVO elMotorU2VO = asProperty(elMotor.getU2(), observedAt);
		PropertyVO elMotorU3VO = asProperty(elMotor.getU3(), observedAt);
		PropertyVO elMotorI1VO = asProperty(elMotor.getI1(), observedAt);
		PropertyVO elMotorI2VO = asProperty(elMotor.getI2(), observedAt);
		PropertyVO elMotorI3VO = asProperty(elMotor.getI3(), observedAt);
		PropertyVO elMotorVRmsVO = asProperty(elMotor.getVrms(), observedAt);

		PropertyVO elMotorMaxVoltageVO = asProperty(elMotor.getMaxVoltage(), observedAt);
		PropertyVO elMotorMaxCurrentVO = asProperty(elMotor.getMaxCurrent(), observedAt);
		PropertyVO elMotorMaxRPMVO = asProperty(elMotor.getMaxRpm(), observedAt);
		PropertyVO elMotorMaxVrmsVO = asProperty(elMotor.getMaxVrms(), observedAt);

		PropertyVO elMotorVO = new PropertyVO().value("electricmotor").observedAt(observedAt).type(PropertyVO.Type.PROPERTY);
		elMotorVO.setAdditionalProperties(
				Map.ofEntries(
						Map.entry("active", elMotorActiveVO),
						Map.entry("rpm", elMotorRpmVO),
						Map.entry("u1", elMotorU1VO),
						Map.entry("u2", elMotorU2VO),
						Map.entry("u3", elMotorU3VO),
						Map.entry("i1", elMotorI1VO),
						Map.entry("i2", elMotorI2VO),
						Map.entry("i3", elMotorI3VO),
						Map.entry("vrms", elMotorVRmsVO),
						Map.entry("rpmmax", elMotorMaxRPMVO),
						Map.entry("umax", elMotorMaxVoltageVO),
						Map.entry("imax", elMotorMaxCurrentVO),
						Map.entry("vrmsmax", elMotorMaxVrmsVO)
				)
		);
		additionalProperties.put("electricmotor", elMotorVO);

		// compat with frontend
		PropertyVO generalInformation = new PropertyVO()
				.type(PropertyVO.Type.PROPERTY)
				.value(Map.of("softwareVersion", asProperty(softwareVersion, observedAt),
						"maxHookHeight", asProperty(maxHookHeight, observedAt),
						"maxLiftingWeight", asProperty(maxLiftingWeight, observedAt),
						"maxPayLoad", asProperty(maxPayload, observedAt),
						"model", asProperty(model, observedAt),
						"currentHookHeight", asProperty(currentHookHeight, observedAt),
						"currentWeight", lifting != null ? asProperty(lifting.getWeight(), observedAt) : asProperty(0, observedAt)));

		additionalProperties.put("inUse", asProperty(lifting != null, observedAt);
		additionalProperties.put("generalInformation", generalInformation);
		ngsiEntity.setAdditionalProperties(additionalProperties);

		return ngsiEntity;
	}

	private void runCurrentStep() {
		if (active) {
			// Calculate delta_h:
			Double tmpHookspeed = getRandomD(0.1, getMaxHookspeedByWeight(lifting.getWeight()));
			currentHookHeight += tmpHookspeed;
			if (currentHookHeight >= 60.0) {
				// Hook in max. altitude
				tmpHookspeed = 0.0;
				currentHookHeight = 60.0;
				elMotor.setU1(0.0);
				elMotor.setU2(0.0);
				elMotor.setU3(0.0);
				elMotor.setI1(0.0);
				elMotor.setI2(0.0);
				elMotor.setI3(0.0);
				elMotor.setRpm(0.0);
				elMotor.setVrms(0.0);
			} else {
				//Hook moving:
				Double tmpCurrent = getCurrentByHookspeed(tmpHookspeed, lifting.getWeight(), elMotor.getMaxCurrent());
				Double tmpVoltage = getVoltageByCurrent(tmpCurrent);
				Double tmpRpm = getRPMByHookspeed(tmpHookspeed);
				Double tmpVrms = getVrmsByHookspeed(tmpHookspeed, malfunction);

				// To make the data be more natural there will be some small distortions in the readings:
				Double distortionMin = -0.1;
				Double distortionMax = 0.1;

				elMotor.setU1(tmpVoltage + getRandomD(distortionMin, distortionMax));
				elMotor.setU2(tmpVoltage + getRandomD(distortionMin, distortionMax));
				elMotor.setU3(tmpVoltage + getRandomD(distortionMin, distortionMax));
				elMotor.setI1(tmpCurrent + getRandomD(distortionMin, distortionMax));
				elMotor.setI2(tmpCurrent + getRandomD(distortionMin, distortionMax));
				elMotor.setI3(tmpCurrent + getRandomD(distortionMin, distortionMax));
				elMotor.setRpm(tmpRpm);
				elMotor.setVrms(tmpVrms);
			}
		}
	}

	private Double getRPMByHookspeed(Double s) {
		//Hook speed m/s
		return (s * 750.0);
	}

	private Double getVrmsByHookspeed(Double s, Boolean malfunction) {
		//Between 0 and 1 thus s is maximal 2
		Double tmpVrms = s / 2.5;
		if (malfunction) {
			if (tmpVrms > 0.89) {
				tmpVrms = 0.99;
			} else {
				Double res = 2.0;
				while (res > 1.0) {
					Double tmp = getRandomD(0.1, 0.2);
					res = tmpVrms + tmp;
				}
				tmpVrms = res;
			}
		}
		return (tmpVrms);
	}

	private Double getCurrentByHookspeed(Double s, Double w, Double m) {
		// s = hookspeed, w = weight, m = maxCurrent
		Double maxHookS = getMaxHookspeedByWeight(w);
		Double percent = s / maxHookS;
		return (m * percent);
	}

	private Double getVoltageByCurrent(Double c) {
		// 0 Amps -> 230V
		// 52 Amps -> 225V
		return (-0.09615 * c + 230.0);
	}

	private Double getMaxHookspeedByWeight(Double w) {
		// 2000kg -> 2m/s
		// 8000kg -> 0.75m/s
		// In between linear.
		// Linear Regression yields:
		return (-0.0002083 * w + 2.417);
	}

	private void startStep() {
		if (!active) {
			elMotor.setU1(0.0);
			elMotor.setU2(0.0);
			elMotor.setU3(0.0);
			elMotor.setI1(0.0);
			elMotor.setI2(0.0);
			elMotor.setI3(0.0);
			elMotor.setRpm(0.0);
			elMotor.setVrms(0.0);
			//active = false;
			malfunction = false;
			elMotor.setActive(false);
		} else {
			//Chance of a malfunction -> higher v_rms. Can be adjusted here:
			Double chance = getRandomD(0.0, 1.0);
			if (chance > 0.8) {
				malfunction = true;
			}
			currentHookHeight = 0.0;
			lifting = new Lifting(getRandomD(1000.0, maxLiftingWeight));
			elMotor.setActive(true);
			elMotor.setI1(40.0);
			elMotor.setI2(40.0);
			elMotor.setI3(40.0);
			elMotor.setU1(getVoltageByCurrent(40.0));
			elMotor.setU2(getVoltageByCurrent(40.0));
			elMotor.setU3(getVoltageByCurrent(40.0));
			Double tmpHookspeed = getRandomD(0.1, getMaxHookspeedByWeight(lifting.getWeight()));
			elMotor.setRpm(getRPMByHookspeed(tmpHookspeed));
			elMotor.setVrms(getVrmsByHookspeed(tmpHookspeed, false));
		}
	}


}
