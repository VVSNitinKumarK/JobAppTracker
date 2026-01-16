package com.jobapptracker.backend.checklist.repository;

import com.jobapptracker.backend.checklist.dto.ChecklistCompanyDto;
import com.jobapptracker.backend.company.dto.CompanyDto;
import com.jobapptracker.backend.company.repository.CompanyRowMapper;
import com.jobapptracker.backend.config.DatabaseConstants;
import com.jobapptracker.backend.config.SqlFragments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public class ChecklistRepository {

    private static final Logger log = LoggerFactory.getLogger(ChecklistRepository.class);

    private final JdbcTemplate jdbcTemplate;
    private final ChecklistRowMapper checklistRowMapper = new ChecklistRowMapper();
    private final CompanyRowMapper companyRowMapper = new CompanyRowMapper();

    public ChecklistRepository(JdbcTemplate template) {
        this.jdbcTemplate = template;
    }

    public List<ChecklistCompanyDto> getChecklist(LocalDate date) {

        String sqlQuery = """
                SELECT
                %s,
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
                )
                %s,
                    checklist.completed,
                    checklist.company_id
                %s
                """.formatted(
                SqlFragments.SELECT_COMPANY_WITH_TAGS,
                DatabaseConstants.TABLE_COMPANY_TRACKING,
                DatabaseConstants.TABLE_DAILY_CHECKLIST,
                DatabaseConstants.TABLE_COMPANY_TAG,
                DatabaseConstants.TABLE_TAG,
                SqlFragments.GROUP_BY_COMPANY,
                SqlFragments.ORDER_BY_COMPANY_NAME
        );

        Date d = Date.valueOf(date);
        return jdbcTemplate.query(sqlQuery, checklistRowMapper, d, d);
    }

    public boolean upsertCompletion(LocalDate date, UUID companyId, boolean completed) {
        log.debug("Upserting completion in database: date={}, companyId={}, completed={}",
                date, companyId, completed);

        // Atomic check-and-upsert: only inserts if company exists, avoiding TOCTOU race
        String sqlQuery = """
                WITH company_check AS (
                    SELECT company_id FROM %s WHERE company_id = ?
                )
                INSERT INTO %s (check_date, company_id, completed, completed_at, updated_at)
                SELECT ?, company_id, ?, CASE WHEN ? THEN now() ELSE NULL END, now()
                FROM company_check
                ON CONFLICT (check_date, company_id)
                DO UPDATE SET
                    completed = EXCLUDED.completed,
                    completed_at = EXCLUDED.completed_at,
                    updated_at = now()
                """.formatted(
                DatabaseConstants.TABLE_COMPANY_TRACKING,
                DatabaseConstants.TABLE_DAILY_CHECKLIST
        );

        int rowsAffected = jdbcTemplate.update(sqlQuery, companyId, Date.valueOf(date), completed, completed);

        if (rowsAffected > 0) {
            log.debug("Completion upserted successfully in database: date={}, companyId={}", date, companyId);
            return true;
        } else {
            log.warn("Company not found for completion upsert: companyId={}", companyId);
            return false;
        }
    }

    public List<CompanyDto> submitDay(LocalDate date) {
        log.debug("Submitting day in database: date={}", date);

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
                ),
                deleted AS (
                    DELETE FROM %s checklist
                    USING completed_companies completed
                    WHERE checklist.company_id = completed.company_id
                      AND checklist.check_date = ?
                    RETURNING checklist.company_id
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
                DatabaseConstants.TABLE_DAILY_CHECKLIST,
                DatabaseConstants.TABLE_COMPANY_TAG,
                DatabaseConstants.TABLE_TAG
        );

        Date d = Date.valueOf(date);
        List<CompanyDto> updated = jdbcTemplate.query(sqlQuery, companyRowMapper, d, d, d, d);
        log.debug("Day submitted successfully in database: date={}, updated {} companies", date, updated.size());
        return updated;
    }

    public boolean companyExists(UUID companyId) {
        String sql = "SELECT EXISTS(SELECT 1 FROM " + DatabaseConstants.TABLE_COMPANY_TRACKING + " WHERE company_id = ?)";
        Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class, companyId);
        return Boolean.TRUE.equals(exists);
    }

    public boolean deleteChecklistEntry(LocalDate date, UUID companyId) {
        log.debug("Deleting checklist entry: date={}, companyId={}", date, companyId);

        String sql = """
                DELETE FROM %s
                WHERE check_date = ? AND company_id = ?
                """.formatted(DatabaseConstants.TABLE_DAILY_CHECKLIST);

        int deleted = jdbcTemplate.update(sql, Date.valueOf(date), companyId);

        if (deleted > 0) {
            log.debug("Checklist entry deleted: date={}, companyId={}", date, companyId);
            return true;
        }
        return false;
    }
}
