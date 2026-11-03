package com.example.demo.dto;

import com.example.demo.model.StudentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentStatusUpdateRequest {

    @NotNull(message = "Student status is mandatory")
    private StudentStatus status;
}

