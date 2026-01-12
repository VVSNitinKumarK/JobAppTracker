package com.jobapptracker.backend.company.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jobapptracker.backend.tag.dto.TagDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.List;

public record CompanyCreateRequest(
   @NotBlank(message = "companyName is required and must not be blank")
   String companyName,

   @NotBlank(message = "careersUrl is required and must not be blank")
   String careersUrl,

   @JsonFormat(pattern = "yyyy-MM-dd")
   LocalDate lastVisitedOn,

   @Positive(message = "revisitAfterDays must be > 0")
   Integer revisitAfterDays,

   List<TagDto> tags
) {}
