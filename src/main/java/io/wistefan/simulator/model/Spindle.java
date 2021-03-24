package io.wistefan.simulator.model;

import lombok.Data;

@Data
public class Spindle {

	private Boolean active = false;
	private final Integer maxWorkSpeed;
	private final Integer maxGrindSpeed;
	private Integer workSpeed = 0;
	private Integer grindSpeed = 0;
}
