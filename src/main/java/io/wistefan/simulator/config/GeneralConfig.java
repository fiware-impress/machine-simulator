package io.wistefan.simulator.config;

import io.micronaut.context.annotation.ConfigurationProperties;
import lombok.Data;

@ConfigurationProperties("general")
@Data
public class GeneralConfig {

	/**
	 * Tenant to be used with orion
	 */
	private String tenant = null;
}
