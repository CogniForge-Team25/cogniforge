package com.cogniforge.controls;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class ControlDtos {
    private ControlDtos() {}

    public record CreateControlRequest(
            @NotBlank @Pattern(regexp = "^[A-Z]{2,5}-[A-Z0-9]{3,8}$") String code,
            @NotBlank @Size(max = 150) String name,
            @NotBlank @Pattern(regexp = "ORGANIZATIONAL|PEOPLE|PHYSICAL|TECHNOLOGICAL") String category,
            @Pattern(regexp = "PLANNED|EFFECTIVE|NEEDS_REVIEW|INEFFECTIVE") String status,
            Long ownerId) {}

    public record UpdateControlRequest(
            @NotBlank @Size(max = 150) String name,
            @NotBlank @Pattern(regexp = "ORGANIZATIONAL|PEOPLE|PHYSICAL|TECHNOLOGICAL") String category,
            @NotBlank @Pattern(regexp = "PLANNED|EFFECTIVE|NEEDS_REVIEW|INEFFECTIVE") String status,
            Long ownerId) {}

    public record ControlView(long id, String code, String name, String category, String status, Long ownerId) {}
}
