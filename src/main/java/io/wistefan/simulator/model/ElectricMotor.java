package io.wistefan.simulator.model;

import lombok.Data;

@Data
public class ElectricMotor {

	private Boolean active = false;
	private final Double maxRpm;
	private final Double maxVoltage;
	private final Double maxCurrent;
	private final Double maxVrms;
	private Double rpm = 0.0;
	private Double u1 = 230.0;
	private Double u2 = 230.0;
	private Double u3 = 230.0;
	private Double i1 = 0.0;
	private Double i2 = 0.0;
	private Double i3 = 0.0;
	private Double vrms = 0.0;

}
