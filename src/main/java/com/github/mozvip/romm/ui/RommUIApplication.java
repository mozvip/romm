package com.github.mozvip.romm.ui;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.github.mozvip.romm.core.RommProperties;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

@SpringBootApplication(scanBasePackages = {"com.github.mozvip.romm.core", "com.github.mozvip.romm.ui", "com.github.mozvip.romm.service", "com.github.mozvip.romm.model"})
@EnableJdbcRepositories(basePackages = {"com.github.mozvip.romm.model"})
@EnableConfigurationProperties(RommProperties.class)
public class RommUIApplication {
    public static void main(String[] args) {
        FlatDarculaLaf.install();
        SpringApplicationBuilder builder = new SpringApplicationBuilder(RommUIApplication.class);
        builder.headless(false);
        ConfigurableApplicationContext context = builder.run(args);
        final RommJFrame jFrame = context.getBean(RommJFrame.class);

        jFrame.pack();
        jFrame.setVisible(true);
        //System.exit(0);
    }
}
