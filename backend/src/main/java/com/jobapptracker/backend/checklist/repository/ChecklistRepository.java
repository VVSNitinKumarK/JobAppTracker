package com.jobapptracker.backend.checklist.repository;

import com.jobapptracker.backend.checklist.dto.ChecklistCompanyDto;
import com.jobapptracker.backend.company.dto.CompanyDto;
import com.jobapptracker.backend.company.repository.CompanyRowMapper;
import com.jobapptracker.backend.config.DatabaseConstants;
import com.jobapptracker.backend.tag.dto.TagDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(ChecklistRepository.class);

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
                    COALESCE(checklist.completed, FALSE) AS completed,
                    (checklist.company_id IS NOT NULL) AS in_checklist
                FROM %s company
                LEFT JOIN %s checklist
                    ON checklist.company_id = company.company_id
                    AND checklist.check_date = ?
                LEFT JOIN %s ct
                    ON ct.company_id = company.company_id
                LEFT JOIN %s t
                    ON t.tag_id = ct.tag_id
                WHERE (
                    company.next_visit_on IS NOT NULL
                    AND company.next_visit_on <= ?
                ) OR (
                    checklist.company_id IS NOT NULL
                    AND checklist.completed = FALSE
                )
                GROUP BY
                    company.company_id,
                    company.company_name,
                    company.careers_url,
                    company.last_visited_on,
                    company.revisit_after_days,
                    company.next_visit_on,
                    company.created_at,
                    company.updated_at,
                    checklist.completed,
                    checklist.company_id
                ORDER BY company.company_name ASC
                """.formatted(
                DatabaseConstants.TABLE_COMPANY_TRACKING,
                DatabaseConstants.TABLE_DAILY_CHECKLIST,
                DatabaseConstants.TABLE_COMPANY_TAG,
                DatabaseConstants.TABLE_TAG
        );

        Date d = Date.valueOf(date);
        return jdbcTemplate.query(sqlQuery, ROW_MAPPER, d, d);
    }

    public void upsertCompletion(LocalDate date, UUID companyId, boolean completed) {
        log.info("Upserting completion in database: date={}, companyId={}, completed={}",
                date, companyId, completed);

        String sqlQuery = """
                INSERT INTO %s (check_date, company_id, completed, completed_at, updated_at)
                VALUES (?, ?, ?, CASE WHEN ? THEN now() ELSE NULL END, now())
                ON CONFLICT (check_date, company_id)
                DO UPDATE SET
                    completed = EXCLUDED.completed,
                    completed_at = EXCLUDED.completed_at,
                    updated_at = now()
                """.formatted(DatabaseConstants.TABLE_DAILY_CHECKLIST);

        jdbcTemplate.update(sqlQuery, Date.valueOf(date), companyId, completed, completed);
        log.info("Completion upserted successfully in database: date={}, companyId={}", date, companyId);
    }

    public List<CompanyDto> submitDay(LocalDate date) {
        log.info("Submitting day in database: date={}", date);

        String sqlQuery = """
                WITH completed_companies AS (
                    SELECT checklist.company_id
                    FROM %s checklist
                    WHERE checklist.check_date = ?
                      AND checklist.completed = TRUE
                ),
                updated AS (
                    UPDATE %s company
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
                LEFT JOIN %s ct ON ct.company_id = u.company_id
                LEFT JOIN %s t ON t.tag_id = ct.tag_id
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
                """.formatted(
                DatabaseConstants.TABLE_DAILY_CHECKLIST,
                DatabaseConstants.TABLE_COMPANY_TRACKING,
                DatabaseConstants.TABLE_COMPANY_TAG,
                DatabaseConstants.TABLE_TAG
        );

        Date d = Date.valueOf(date);

        CompanyRowMapper mapper = new CompanyRowMapper();
        List<CompanyDto> updated = jdbcTemplate.query(sqlQuery, mapper, d, d, d);
        log.info("Day submitted successfully in database: date={}, updated {} companies", date, updated.size());
        return updated;
    }

    public boolean companyExists(UUID companyId) {
        // This one is fine as normal concatenation because it's NOT inside a text block
        List<Integer> rows = jdbcTemplate.query(
                "SELECT 1 FROM " + DatabaseConstants.TABLE_COMPANY_TRACKING + " WHERE company_id = ?",
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
                    resultSet.getBoolean("completed"),
                    resultSet.getBoolean("in_checklist")
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
