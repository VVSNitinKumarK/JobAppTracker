package com.jobapptracker.backend.company.repository;

import com.jobapptracker.backend.company.dto.CompanyDto;
import com.jobapptracker.backend.tag.dto.TagDto;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Component
public class CompanyRowMapper implements RowMapper<CompanyDto> {

    @Override
    public CompanyDto mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        UUID companyId = resultSet.getObject("company_id", UUID.class);

        List<String> keys = toStringList(resultSet.getArray("tag_keys"));
        List<String> names = toStringList(resultSet.getArray("tag_names"));
        List<TagDto> tags = zipTags(keys, names);

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

    private static List<String> toStringList(Array sqlArray) throws SQLException {
        if (sqlArray == null) {
            return List.of();
        }
        Object arr = sqlArray.getArray();
        if (arr == null) {
            return List.of();
        }
        return Arrays.asList((String[]) arr);
    }

    private static List<TagDto> zipTags(List<String> keys, List<String> names) {
        int n = Math.min(keys.size(), names.size());
        List<TagDto> out = new ArrayList<>(n);

        for (int i = 0; i < n; i++) {
            String k = keys.get(i);
            String nm = names.get(i);

            if (k == null || k.isBlank() || nm == null || nm.isBlank()) {
                continue;
            }

            out.add(new TagDto(k.trim(), nm.trim()));
        }

        return out;
    }
}
