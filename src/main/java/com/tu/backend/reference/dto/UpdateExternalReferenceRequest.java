package com.tu.backend.reference.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateExternalReferenceRequest(
    @Size(max = 64) String resourceItemId,
    @Pattern(regexp = "auto|manual_bound|manual_unbound") String bindingMode,
    @Size(max = 255) String displayText,
    @Size(max = 255) String citationLocator,
    @Size(max = 1024) String citationNote
) {
}
