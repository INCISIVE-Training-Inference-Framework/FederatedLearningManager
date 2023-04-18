package config;

import exceptions.BadConfigurationException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnvironmentVariable {

    private static Map<String, String> debugEnvironment;

    private final String name;
    private final EnvironmentVariableType type;
    private Object defaultValue;

    public EnvironmentVariable(String name, EnvironmentVariableType type) {
        this.name = name;
        this.type = type;
    }

    public EnvironmentVariable(String name, EnvironmentVariableType type, Object defaultValue) {
        this.name = name;
        this.type = type;
        this.defaultValue = defaultValue;
    }

    public Object load() throws BadConfigurationException {
        String value;
        if (debugEnvironment != null) value = debugEnvironment.get(this.name);
        else value = System.getenv(this.name);
        if (value == null || value.length() == 0) {
            if (this.defaultValue == null) {
                throw new BadConfigurationException("Missing environment variable " + this.name);
            } else {
                return this.defaultValue;
            }
        } else {
            switch (this.type) {
                case STRING:
                    return value;
                case INTEGER:
                    try {
                        return Integer.parseInt(value);
                    } catch (NumberFormatException e) {
                        throw new BadConfigurationException("Environment variable " + this.name + " should be an integer");
                    }
                case LONG:
                    try {
                        return Long.parseLong(value);
                    } catch (NumberFormatException e) {
                        throw new BadConfigurationException("Environment variable " + this.name + " should be a long");
                    }
                default:
                    // this code should never be executed
                    return null;
            }
        }
    }

    public static Map<String, Object> loadEnvironmentVariables(List<EnvironmentVariable> environmentVariables) throws BadConfigurationException {
        Map<String, Object> config = new HashMap<>();
        for (EnvironmentVariable environmentVariable: environmentVariables) {
            config.put(environmentVariable.name, environmentVariable.load());
        }
        return config;
    }

    public static void setDebugEnvironment(Map<String, String> debugEnvironment) {
        EnvironmentVariable.debugEnvironment = debugEnvironment;
    }
}
