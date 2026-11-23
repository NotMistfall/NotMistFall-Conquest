package notmistfalls.conquest.config;

/** Thrown when the config file is unreadable or has an invalid value. */
public final class ConfigException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ConfigException(String message) {
        super(message);
    }

    public ConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
