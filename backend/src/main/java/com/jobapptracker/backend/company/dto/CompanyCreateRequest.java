package com.jobapptracker.backend.company.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jobapptracker.backend.tag.dto.TagDto;

import java.time.LocalDate;
import java.util.List;

public record CompanyCreateRequest(
   String companyName,
   String careersUrl,

   @JsonFormat(pattern = "yyyy-MM-dd")
   LocalDate lastVisitedOn,

   Integer revisitAfterDays,
   List<TagDto> tags
) {}
