package com.peakui.auth.model.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateCurrentUserRequest {

    @Size(max = 50)
    private String nickname;

    @Size(max = 20)
    private String phone;

    private Integer gender;

    private LocalDate birthday;

    @Size(max = 255)
    private String signature;
}
