package com.jobapptracker.backend.company.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.util.List;

public record CompanyCreateRequest(
   String companyName,
   String careersUrl,

   @JsonFormat(pattern = "yyyy-MM-dd")
   LocalDate lastVisitedOn,

   Integer revisitAfterDays,
   List<String> tags
) {}
