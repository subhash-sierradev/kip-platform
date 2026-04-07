package com.integration.execution.util;

import com.integration.execution.contract.model.enums.ServiceType;

import java.text.Normalizer;
import java.util.UUID;
import java.util.regex.Pattern;

public final class SecretKeyUtil {

    private static final int MAX_SECRET_NAME_LENGTH = 100;
    private static final String SEPARATOR = "-";
    private static final Pattern SAFE_PATTERN = Pattern.compile("[^a-z0-9-]");

    private SecretKeyUtil() {
    }

    public static String generate(ServiceType serviceType, String tenantId, UUID connectionId) {
        String service = normalize(serviceType.name());
        String tenant = normalize(tenantId);
        String uuid = connectionId.toString();
        String secretName = String.join(
                SEPARATOR, service, tenant, uuid
        );
        return enforceLength(secretName);
    }

    private static String normalize(String value) {
        return SAFE_PATTERN.matcher(Normalizer.normalize(value, Normalizer.Form.NFD).toLowerCase()
                .replace(" ", SEPARATOR)).replaceAll("");
    }

    private static String enforceLength(String secretName) {
        if (secretName.length() <= MAX_SECRET_NAME_LENGTH) {
            return secretName;
        }
        return secretName.substring(
                secretName.length() - MAX_SECRET_NAME_LENGTH
        );
    }
}
