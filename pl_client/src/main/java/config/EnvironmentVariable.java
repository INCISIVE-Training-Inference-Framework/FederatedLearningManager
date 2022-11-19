package config;

import exceptions.BadConfigurationException;

public class EnvironmentVariable {

    public String name;
    public EnvironmentVariableType type;
    public Object defaultValue;

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
        String value = System.getenv(this.name);
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
}
