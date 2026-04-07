package com.integration.execution.contract.rest.response.kaseware;

import java.util.List;

public record KwDynamicDocType(
        String id,
        String title,
        List<String> tags
) {
}
