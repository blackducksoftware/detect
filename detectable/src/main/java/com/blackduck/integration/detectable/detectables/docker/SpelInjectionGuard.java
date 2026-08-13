package com.blackduck.integration.detectable.detectables.docker;

/**
 * Rejects property values that could trigger value-borne Spring Expression Language (SpEL)
 * evaluation in Docker Inspector's {@code @Value("${...}")}-bound fields.
 * <p>
 * Docker Inspector is a Spring Boot application on Spring Framework 5.3.x, which is affected by
 * SpEL-injection issues including CVE-2026-41849. Spring's {@code @Value} injection pipeline
 * resolves {@code ${...}} placeholders and then passes the resolved value through
 * {@code StandardBeanExpressionResolver}, which parses and evaluates any {@code #{...}} template
 * in the string. Values that contain {@code #{...}} therefore constitute a code-execution sink
 * during Spring context refresh, before any application-level checks run.
 * <p>
 * Detect's role in the mitigation is to refuse to forward any configuration value containing
 * a SpEL template to Docker Inspector, whether via the fork command line
 * ({@code --docker.image=...}, {@code --docker.tar=...}, {@code --docker.image.id=...}) or via
 * the generated {@code application.properties} file (passthrough properties,
 * {@code docker.platform.top.layer.id}).
 * <p>
 * Rejection is intentionally aggressive: any string containing the literal {@code "#{"}
 * substring is refused, whether balanced or not. Unbalanced templates still enter Spring's
 * SpEL parser and can raise {@code SpelParseException} at bean init, which is itself a
 * denial-of-service primitive.
 */
public final class SpelInjectionGuard {

    private static final String SPEL_TEMPLATE_PREFIX = "#{";

    private SpelInjectionGuard() {
        // utility class
    }

    /**
     * @return {@code true} if the value contains a SpEL template start sequence ({@code "#{"}).
     * Null and empty values are treated as safe.
     */
    public static boolean containsSpelTemplate(String value) {
        return value != null && value.contains(SPEL_TEMPLATE_PREFIX);
    }

    /**
     * Throws {@link IllegalArgumentException} if the value contains a SpEL template.
     *
     * @param propertyName the name of the property being forwarded (used in the error message)
     * @param value        the value to check
     */
    public static void rejectIfContainsSpelTemplate(String propertyName, String value) {
        if (containsSpelTemplate(value)) {
            throw new IllegalArgumentException(buildRejectionMessage(propertyName));
        }
    }

    public static String buildRejectionMessage(String propertyName) {
        return String.format(
            "Detect refused to forward property '%s' to Docker Inspector because its value contains a Spring "
                + "Expression Language template ('#{...}'). This is refused as a defense against value-borne SpEL "
                + "injection in Docker Inspector (CVE-2026-41849). Remove the '#{...}' expression from the property "
                + "value, or set the property to a plain string.",
            propertyName
        );
    }
}
