package org.springaicommunity.bench.core.io;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springaicommunity.bench.core.spec.BenchCase;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility for loading a BenchCase from a YAML file.
 * (Day-0 / Day-1: parsing only – schema validation can be added later.)
 */
public final class BenchCaseLoader {

    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    private BenchCaseLoader() {
        /* static utility – do not instantiate */
    }

    /** Read the YAML file and convert it into a BenchCase POJO. */
    public static BenchCase load(Path yamlPath) throws IOException {
        try (var reader = Files.newBufferedReader(yamlPath)) {
            return YAML_MAPPER.readValue(reader, BenchCase.class);
        }
    }
}
