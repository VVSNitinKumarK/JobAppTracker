package com.jobapptracker.backend.company.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jobapptracker.backend.tag.dto.TagDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record CompanyCreateRequest(
   @NotBlank(message = "companyName is required and must not be blank")
   @Size(max = 255, message = "companyName must not exceed 255 characters")
   String companyName,

   @NotBlank(message = "careersUrl is required and must not be blank")
   @Size(max = 2048, message = "careersUrl must not exceed 2048 characters")
   @Pattern(
           regexp = "^https?://[^\\s/$.?#].[^\\s]*$",
           message = "careersUrl must be a valid URL starting with http:// or https://"
   )
   String careersUrl,

   @JsonFormat(pattern = "yyyy-MM-dd")
   LocalDate lastVisitedOn,

   @Positive(message = "revisitAfterDays must be > 0")
   Integer revisitAfterDays,

   List<TagDto> tags
) {}
