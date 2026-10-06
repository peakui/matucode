package com.peakui.auth.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateAvatarRequest {

    @NotBlank
    @Size(max = 255)
    private String avatarUrl;
}
