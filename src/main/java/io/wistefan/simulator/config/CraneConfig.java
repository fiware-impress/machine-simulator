package io.wistefan.simulator.config;

import io.wistefan.simulator.model.HealthStatus;
import lombok.Data;

@Data
public class CraneConfig {

	private String name;
	private String model;
	private Double latitude = null;
	private Double longitude = null;
	private Double maxRadius = 60.0;
	private Double maxHookHeight = 130.0;
	private Double maxLiftingWeight = 8000.0;
	private Double payloadAtTip = 1650.0;
	private HealthStatus healthStatus = HealthStatus.HEALTHY;
	private Double currentCost= 40.20;
}
