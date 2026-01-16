package com.jobapptracker.backend.checklist.repository;

import com.jobapptracker.backend.checklist.dto.ChecklistCompanyDto;
import com.jobapptracker.backend.config.SqlArrayUtils;
import com.jobapptracker.backend.tag.dto.TagDto;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class ChecklistRowMapper implements RowMapper<ChecklistCompanyDto> {

    @Override
    public ChecklistCompanyDto mapRow(ResultSet rs, int rowNum) throws SQLException {
        UUID companyId = rs.getObject("company_id", UUID.class);

        List<String> tagKeys = SqlArrayUtils.toStringList(rs.getArray("tag_keys"));
        List<String> tagNames = SqlArrayUtils.toStringList(rs.getArray("tag_names"));
        List<TagDto> tags = SqlArrayUtils.zipTags(tagKeys, tagNames);

        return new ChecklistCompanyDto(
                companyId,
                rs.getString("company_name"),
                rs.getString("careers_url"),
                rs.getObject("last_visited_on", LocalDate.class),
                rs.getInt("revisit_after_days"),
                tags,
                rs.getObject("next_visit_on", LocalDate.class),
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("updated_at", OffsetDateTime.class),
                rs.getBoolean("completed"),
                rs.getBoolean("in_checklist")
        );
    }
}