package com.jobapptracker.backend.company.repository;

import com.jobapptracker.backend.company.dto.CompanyDto;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Component
public class CompanyRowMapper implements RowMapper<CompanyDto> {

    @Override
    public CompanyDto mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        UUID companyId = resultSet.getObject("company_id", UUID.class);
        String companyName = resultSet.getString("company_name");
        String careersUrl = resultSet.getString("careers_url");

        LocalDate lastVisitedOn = resultSet.getObject("last_visited_on", LocalDate.class);
        int revisitAfterDays = resultSet.getInt("revisit_after_days");

        List<String> tags = toStringList(resultSet.getArray("tags"));

        LocalDate nextVisitOn = resultSet.getObject("next_visit_on", LocalDate.class);
        OffsetDateTime createdAt = resultSet.getObject("created_at", OffsetDateTime.class);
        OffsetDateTime updatedAt = resultSet.getObject("updated_at", OffsetDateTime.class);

        return new CompanyDto(
                companyId,
                companyName,
                careersUrl,
                lastVisitedOn,
                revisitAfterDays,
                tags,
                nextVisitOn,
                createdAt,
                updatedAt
        );
    }

    private static List<String> toStringList(Array sqlArray) throws SQLException {
        if (sqlArray == null) {
            return List.of();
        }

        Object arrObj = sqlArray.getArray();
        if (arrObj == null) {
            return List.of();
        }

        String[] array = (String[]) arrObj;
        return List.of(array);
    }
}
