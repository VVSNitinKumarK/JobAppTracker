package com.jobapptracker.backend.checklist.repository;

import com.jobapptracker.backend.checklist.dto.ChecklistCompanyDto;
import com.jobapptracker.backend.company.dto.CompanyDto;
import com.jobapptracker.backend.company.repository.CompanyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Array;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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
                    company.tags,
                    company.next_visit_on,
                    company.created_at,
                    company.updated_at,
                    COALESCE(checklist.completed, FALSE) AS completed
                FROM company_tracking company
                LEFT JOIN daily_checklist checklist
                    ON checklist.company_id = company.company_id
                    AND checklist.check_date = ?
                WHERE company.next_visit_on IS NOT NULL
                AND company.next_visit_on <= ?
                ORDER BY company.company_name ASC
                """;

        Date d = Date.valueOf(date);
        return jdbcTemplate.query(sqlQuery, ROW_MAPPER, d, d);
    }

    public void upsertCompletion(LocalDate date, UUID companyId, boolean completed) {
        String sqlQuery = """
                INSERT INTO daily_checklist (check_date, company_id, completed, completed_at, updated_at)
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
                    FROM daily_checklist checklist
                    WHERE checklist.check_date = ?
                        AND checklist.completed = TRUE
                )
                UPDATE company_tracking company
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
                    company.tags,
                    company.next_visit_on,
                    company.created_at,
                    company.updated_at
                """;

        Date d = Date.valueOf(date);

        CompanyRowMapper mapper = new CompanyRowMapper();

        return jdbcTemplate.query(sqlQuery, mapper, d, d, d);
    }

    public boolean companyExists(UUID companyId) {
        List<Integer> rows = jdbcTemplate.query(
                "SELECT 1 FROM company_tracking WHERE company_id = ?",
                (resultSet, rowNum) -> resultSet.getInt(1),
                companyId
        );

        return !rows.isEmpty();
    }

    private static final RowMapper<ChecklistCompanyDto> ROW_MAPPER = new RowMapper<>() {
        @Override
        public ChecklistCompanyDto mapRow(ResultSet resultSet, int rowNum) throws SQLException {
            UUID companyId = resultSet.getObject("company_id", UUID.class);
            List<String> tags = toStringList(resultSet.getArray("tags"));

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
            if (sqlArray == null) {
                return List.of();
            }

            Object arrayObject = sqlArray.getArray();
            if (arrayObject == null) {
                return List.of();
            }

            return Arrays.asList((String[]) arrayObject);
        }
    };
}
