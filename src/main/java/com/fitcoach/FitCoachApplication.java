package com.fitcoach;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SpringBootApplication
public class FitCoachApplication {

    /**
     * Run with {@code --migrate-db-and-exit} to apply JPA schema updates and compatibility hooks,
     * then exit. Uses an ephemeral HTTP port so it does not clash with a dev server on 8080.
     */
    public static void main(String[] args) {
        boolean migrateOnly = Arrays.asList(args).contains("--migrate-db-and-exit");
        SpringApplication app = new SpringApplication(FitCoachApplication.class);
        if (migrateOnly) {
            List<String> merged = new ArrayList<>(Arrays.asList(args));
            if (merged.stream().noneMatch(a -> a.equals("--server.port=0") || a.startsWith("--server.port="))) {
                merged.add("--server.port=0");
            }
            args = merged.toArray(new String[0]);
        }
        ConfigurableApplicationContext ctx = app.run(args);
        if (migrateOnly) {
            System.exit(SpringApplication.exit(ctx, () -> 0));
        }
    }
}
