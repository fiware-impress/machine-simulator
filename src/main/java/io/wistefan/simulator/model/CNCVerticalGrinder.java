package io.wistefan.simulator.model;

import lombok.Setter;
import lombok.ToString;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.ngsi.model.GeoPropertyVO;
import org.fiware.ngsi.model.PropertyVO;
import org.fiware.ngsi.model.RelationshipVO;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@ToString
public class CNCVerticalGrinder extends AbstractDevice {

	private Integer currentStepNr = 0;
	private Integer currentLength = 0;

	private String softwareVersion = "0.0.1";
	private Boolean active = false;
	private Integer maxWorkpieceHeight = 600;
	private Integer maxGrindingDiameter = 1200;
	private Integer maxGrindingLength = 500;
	@Setter
	private URI currentCompany;
	private Grinding grinding;
	private Spindle spindle = new Spindle(500, 18000);
	private Workpiece workpiece;


	public CNCVerticalGrinder(URI id, ScheduledExecutorService scheduledExecutorService, EntitiesApiClient entitiesApiClient, Clock clock) {
		super(id, scheduledExecutorService, entitiesApiClient, clock);
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
			// will be between 300 and 3000
			currentLength = getRandom(300, 3000);
			// reset step to 0
			currentStepNr = 0;
			startStep();
		} else {
			runCurrentStep();
		}
		currentStepNr++;
	}

	@Override
	protected EntityVO getNgsiEntity() {
		Instant observedAt = getClock().instant();
		GeoPropertyVO location = new GeoPropertyVO().observedAt(observedAt).type(GeoPropertyVO.Type.GEOPROPERTY).value(getLocation());
		EntityVO ngsiEntity = new EntityVO().atContext(CONTEXT_URL).id(getId()).location(location).type("grinder");
		Map<String, Object> additionalProperties = new HashMap<>();

		additionalProperties.put("softwareVersion", asProperty(softwareVersion, observedAt));
		additionalProperties.put("active", asProperty(active, observedAt));
		additionalProperties.put("maxWorkpieceHeight", asProperty(maxWorkpieceHeight, observedAt));
		additionalProperties.put("maxGrindingDiameter", asProperty(maxGrindingDiameter, observedAt));
		additionalProperties.put("maxGrindingLength", asProperty(maxGrindingLength, observedAt));

		if(currentCompany != null) {
			RelationshipVO companyRelationshipVO = new RelationshipVO().observedAt(observedAt).type(RelationshipVO.Type.RELATIONSHIP)._object(currentCompany);
			additionalProperties.put("currentCompany", companyRelationshipVO);
		}
		if (grinding != null) {
			PropertyVO grindingDiameterVO = asProperty(grinding.getDiameter(), observedAt);
			PropertyVO grindingLengthVO = asProperty(grinding.getLength(), observedAt);
			PropertyVO grindingVO = new PropertyVO().value("grinding").observedAt(observedAt).type(PropertyVO.Type.PROPERTY);
			grindingVO.setAdditionalProperties(Map.of("diameter", grindingDiameterVO, "length", grindingLengthVO));
			additionalProperties.put("grinding", grindingVO);
		}
		if (workpiece != null) {
			PropertyVO workpieceHeightVO = asProperty(workpiece.getHeight(), observedAt);
			PropertyVO wokpieceMaterialVO = asProperty(workpiece.getMaterial(), observedAt);
			PropertyVO workpieceVO = new PropertyVO().value("workpiece").observedAt(observedAt).type(PropertyVO.Type.PROPERTY);
			workpieceVO.setAdditionalProperties(Map.of("height", workpieceHeightVO, "material", wokpieceMaterialVO));
		}
		PropertyVO spindleActiveVO = asProperty(spindle.getActive(), observedAt);
		PropertyVO spindleGrindSpeedVO = asProperty(spindle.getGrindSpeed(), observedAt);
		PropertyVO spindleWorkSpeedVO = asProperty(spindle.getWorkSpeed(), observedAt);
		PropertyVO spindleMaxGrindSpeedVO = asProperty(spindle.getMaxGrindSpeed(), observedAt);
		PropertyVO spindleMaxWorkSpeedVO = asProperty(spindle.getMaxWorkSpeed(), observedAt);
		PropertyVO spindleVO = new PropertyVO().value("spindle").observedAt(observedAt).type(PropertyVO.Type.PROPERTY);
		spindleVO.setAdditionalProperties(
				Map.of(
						"active", spindleActiveVO,
						"grindSpeed", spindleGrindSpeedVO,
						"workSpeed", spindleWorkSpeedVO,
						"maxGrindSpeed", spindleMaxGrindSpeedVO,
						"maxWorkSpeed", spindleMaxWorkSpeedVO));
		additionalProperties.put("spindle", spindleVO);
		ngsiEntity.setAdditionalProperties(additionalProperties);
		return ngsiEntity;
	}

	private void runCurrentStep() {
		if (active) {
			int changedGrindSpeed = spindle.getGrindSpeed() + getRandom(-30, 30);
			int changedWorkSpeed = spindle.getWorkSpeed() + getRandom(-5, 5);
			// dont let it stop or get to fast
			if (!(changedGrindSpeed < 5) && !(changedGrindSpeed > spindle.getMaxGrindSpeed())) {
				spindle.setGrindSpeed(changedGrindSpeed);
			}
			if (!(changedWorkSpeed < 5) && !(changedWorkSpeed > spindle.getMaxWorkSpeed())) {
				spindle.setWorkSpeed(changedWorkSpeed);
			}
		}
	}

	private void startStep() {
		if (active) {
			spindle.setGrindSpeed(0);
			spindle.setWorkSpeed(0);
			spindle.setActive(false);
			workpiece = null;
			grinding = null;
			active = false;
		} else {
			spindle.setActive(true);
			spindle.setWorkSpeed(getRandom(10, spindle.getMaxWorkSpeed()));
			spindle.setGrindSpeed(getRandom(100, spindle.getMaxGrindSpeed()));
			workpiece = new Workpiece(getRandomMaterial(), getRandom(10, maxWorkpieceHeight));
			grinding = new Grinding(getRandom(5, maxGrindingDiameter), getRandom(5, maxGrindingLength));
		}
	}

	private Material getRandomMaterial() {
		int matNr = getRandom(0, 2);
		switch (matNr) {
			case 0:
				return Material.ALUMINUM;
			case 1:
				return Material.STEEL;
			default:
				return Material.WOOD;
		}
	}

}
