package com.jobapptracker.backend.checklist.repository;

import com.jobapptracker.backend.checklist.dto.ChecklistCompanyDto;
import com.jobapptracker.backend.company.dto.CompanyDto;
import com.jobapptracker.backend.company.repository.CompanyRowMapper;
import com.jobapptracker.backend.tag.dto.TagDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Array;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Repository
public class ChecklistRepository {

    private final JdbcTemplate jdbcTemplate;

    public ChecklistRepository(JdbcTemplate template) {
        this.jdbcTemplate = template;
    }

    public List<ChecklistCompanyDto> getCheckList(LocalDate date) {
        String sqlQuery = """
                SELECT
                    company.company_id,
                    company.company_name,
                    company.careers_url,
                    company.last_visited_on,
                    company.revisit_after_days,
                    COALESCE(
                        array_agg(t.tag_key ORDER BY t.tag_name)
                            FILTER (WHERE t.tag_id IS NOT NULL),
                        '{}'::text[]
                    ) AS tag_keys,
                    COALESCE(
                        array_agg(t.tag_name ORDER BY t.tag_name)
                            FILTER (WHERE t.tag_id IS NOT NULL),
                        '{}'::text[]
                    ) AS tag_names,
                    company.next_visit_on,
                    company.created_at,
                    company.updated_at,
                    COALESCE(checklist.completed, FALSE) AS completed
                FROM jobapps.company_tracking company
                LEFT JOIN jobapps.daily_checklist checklist
                    ON checklist.company_id = company.company_id
                    AND checklist.check_date = ?
                LEFT JOIN jobapps.company_tag ct
                    ON ct.company_id = company.company_id
                LEFT JOIN jobapps.tag t
                    ON t.tag_id = ct.tag_id
                WHERE company.next_visit_on IS NOT NULL
                AND company.next_visit_on <= ?
                GROUP BY
                    company.company_id,
                    company.company_name,
                    company.careers_url,
                    company.last_visited_on,
                    company.revisit_after_days,
                    company.next_visit_on,
                    company.created_at,
                    company.updated_at,
                    checklist.completed
                ORDER BY company.company_name ASC
                """;

        Date d = Date.valueOf(date);
        return jdbcTemplate.query(sqlQuery, ROW_MAPPER, d, d);
    }

    public void upsertCompletion(LocalDate date, UUID companyId, boolean completed) {
        String sqlQuery = """
                INSERT INTO jobapps.daily_checklist (check_date, company_id, completed, completed_at, updated_at)
                VALUES (?, ?, ?, CASE WHEN ? THEN now() ELSE NULL END, now())
                ON CONFLICT (check_date, company_id)
                DO UPDATE SET
                    completed = EXCLUDED.completed,
                    completed_at = EXCLUDED.completed_at,
                    updated_at = now()
                """;

        jdbcTemplate.update(sqlQuery, Date.valueOf(date), companyId, completed, completed);
    }

    public List<CompanyDto> submitDay(LocalDate date) {
        String sqlQuery = """
                WITH completed_companies As (
                    SELECT checklist.company_id
                    FROM jobapps.daily_checklist checklist
                    WHERE checklist.check_date = ?
                        AND checklist.completed = TRUE
                ),
                updated AS (
                    UPDATE jobapps.company_tracking company
                    SET
                        last_visited_on = ?,
                        updated_at = now()
                    FROM completed_companies completed
                    WHERE company.company_id = completed.company_id
                        AND (company.last_visited_on IS NULL OR company.last_visited_on < ?)
                    RETURNING
                        company.company_id,
                        company.company_name,
                        company.careers_url,
                        company.last_visited_on,
                        company.revisit_after_days,
                        company.next_visit_on,
                        company.created_at,
                        company.updated_at
                )
                SELECT
                    u.company_id,
                    u.company_name,
                    u.careers_url,
                    u.last_visited_on,
                    u.revisit_after_days,
                    COALESCE(
                        array_agg(t.tag_key ORDER BY t.tag_name)
                            FILTER (WHERE t.tag_id IS NOT NULL),
                        '{}'::text[]
                    ) AS tag_keys,
                    COALESCE(
                        array_agg(t.tag_name ORDER BY t.tag_name)
                            FILTER (WHERE t.tag_id IS NOT NULL),
                        '{}'::text[]
                    ) AS tag_names,
                    u.next_visit_on,
                    u.created_at,
                    u.updated_at
                FROM updated u
                LEFT JOIN jobapps.company_tag ct ON ct.company_id = u.company_id
                LEFT JOIN jobapps.tag t ON t.tag_id = ct.tag_id
                GROUP BY
                    u.company_id,
                    u.company_name,
                    u.careers_url,
                    u.last_visited_on,
                    u.revisit_after_days,
                    u.next_visit_on,
                    u.created_at,
                    u.updated_at
                ORDER BY u.company_name ASC
                """;

        Date d = Date.valueOf(date);

        CompanyRowMapper mapper = new CompanyRowMapper();

        return jdbcTemplate.query(sqlQuery, mapper, d, d, d);
    }

    public boolean companyExists(UUID companyId) {
        List<Integer> rows = jdbcTemplate.query(
                "SELECT 1 FROM jobapps.company_tracking WHERE company_id = ?",
                (resultSet, rowNum) -> resultSet.getInt(1),
                companyId
        );

        return !rows.isEmpty();
    }

    private static final RowMapper<ChecklistCompanyDto> ROW_MAPPER = new RowMapper<>() {
        @Override
        public ChecklistCompanyDto mapRow(ResultSet resultSet, int rowNum) throws SQLException {
            UUID companyId = resultSet.getObject("company_id", UUID.class);

            List<String> tagKeys = toStringList(resultSet.getArray("tag_keys"));
            List<String> tagNames = toStringList(resultSet.getArray("tag_names"));
            List<TagDto> tags = zipTags(tagKeys, tagNames);

            return new ChecklistCompanyDto(
                    companyId,
                    resultSet.getString("company_name"),
                    resultSet.getString("careers_url"),
                    resultSet.getObject("last_visited_on", LocalDate.class),
                    resultSet.getInt("revisit_after_days"),
                    tags,
                    resultSet.getObject("next_visit_on", LocalDate.class),
                    resultSet.getObject("created_at", OffsetDateTime.class),
                    resultSet.getObject("updated_at", OffsetDateTime.class),
                    resultSet.getBoolean("completed")
            );
        }

        private static List<String> toStringList(Array sqlArray) throws SQLException {
            if (sqlArray == null) return List.of();
            Object arrayObject = sqlArray.getArray();
            if (arrayObject == null) return List.of();
            return Arrays.asList((String[]) arrayObject);
        }

        private static List<TagDto> zipTags(List<String> keys, List<String> names) {
            int n = Math.min(keys.size(), names.size());
            List<TagDto> out = new ArrayList<>(n);

            for (int i = 0; i < n; i++) {
                String k = keys.get(i);
                String nm = names.get(i);
                if (k != null && !k.isBlank() && nm != null && !nm.isBlank()) {
                    out.add(new TagDto(k, nm));
                }
            }
            return out;
        }
    };
}
