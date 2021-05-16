package com.github.mozvip.romm.cli;

import com.github.mozvip.romm.core.RommProperties;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

@SpringBootApplication(scanBasePackages = {"com.github.mozvip.romm.cli", "com.github.mozvip.romm.core", "com.github.mozvip.romm.service", "com.github.mozvip.romm.model"})
@EnableJdbcRepositories(basePackages = {"com.github.mozvip.romm.model"})
@EnableJdbcAuditing
@EnableBatchProcessing
@EnableConfigurationProperties(RommProperties.class)
public class RommApplication {

	public static void main(String[] args) {
		SpringApplication.run(RommApplication.class, args);
	}

}
