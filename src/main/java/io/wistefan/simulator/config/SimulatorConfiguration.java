package io.wistefan.simulator.config;

import io.micronaut.context.annotation.ConfigurationProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties("simulator")
@Data
public class SimulatorConfiguration {

	private List<CompanyConfig> companies = new ArrayList<>();
}
