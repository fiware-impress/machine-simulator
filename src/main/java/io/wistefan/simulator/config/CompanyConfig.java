package io.wistefan.simulator.config;

import lombok.Data;

import java.util.List;

@Data
public class CompanyConfig {

	public String name;
	public List<CraneConfig> cranes;
}
