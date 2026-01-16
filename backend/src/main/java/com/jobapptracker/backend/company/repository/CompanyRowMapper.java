package com.jobapptracker.backend.company.repository;

import com.jobapptracker.backend.company.dto.CompanyDto;
import com.jobapptracker.backend.config.SqlArrayUtils;
import com.jobapptracker.backend.tag.dto.TagDto;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class CompanyRowMapper implements RowMapper<CompanyDto> {

    @Override
    public CompanyDto mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        UUID companyId = resultSet.getObject("company_id", UUID.class);

        List<String> tagKeys = SqlArrayUtils.toStringList(resultSet.getArray("tag_keys"));
        List<String> tagNames = SqlArrayUtils.toStringList(resultSet.getArray("tag_names"));
        List<TagDto> tags = SqlArrayUtils.zipTags(tagKeys, tagNames);

        return new CompanyDto(
                companyId,
                resultSet.getString("company_name"),
                resultSet.getString("careers_url"),
                resultSet.getObject("last_visited_on", LocalDate.class),
                resultSet.getInt("revisit_after_days"),
                tags,
                resultSet.getObject("next_visit_on", LocalDate.class),
                resultSet.getObject("created_at", OffsetDateTime.class),
                resultSet.getObject("updated_at", OffsetDateTime.class)
        );
    }
}
