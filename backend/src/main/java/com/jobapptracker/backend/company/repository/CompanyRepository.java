package com.jobapptracker.backend.company.repository;

import com.jobapptracker.backend.company.dto.CompanyDto;
import com.jobapptracker.backend.company.web.DueFilter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Repository
public class CompanyRepository {

    private final JdbcTemplate jdbcTemplate;
    private final CompanyRowMapper rowMapper = new CompanyRowMapper();

    public CompanyRepository(JdbcTemplate template) {
        this.jdbcTemplate = template;
    }

    public List<CompanyDto> findCompanies(
            int page,
            int size,
            String q,
            List<String> tagsAny,
            DueFilter due,
            LocalDate date
    ) {
        boolean hasDateFilter = (date != null);
        DueFilter effectiveDue = hasDateFilter ? null : due;

        StringBuilder sqlQuery = new StringBuilder("""
                SELECT
                    company_id,
                    company_name,
                    careers_url,
                    last_visited_on,
                    revisit_after_days,
                    tags,
                    next_visit_on,
                    created_at,
                    updated_at
                FROM company_tracking
                WHERE 1=1
                """);

        List<Object> parameters = new ArrayList<>();

        if (q != null && !q.isBlank()) {
            sqlQuery.append(" AND company_name ILIKE ? ");
            parameters.add(q.trim() + "%");
        }

        if (tagsAny != null && !tagsAny.isEmpty()) {
            sqlQuery.append(" AND tags && ? ");
            parameters.add(tagsAny);
        }

        if (hasDateFilter) {
            sqlQuery.append(" AND next_visit_on = ? ");
            parameters.add(date);
        }

        if (effectiveDue != null) {
            switch (effectiveDue) {
                case TODAY -> sqlQuery.append(" AND next_visit_on <= CURRENT_DATE ");
                case OVERDUE -> sqlQuery.append(" AND next_visit_on < CURRENT_DATE ");
                case UPCOMING -> sqlQuery.append(" AND next_visit_on > CURRENT_DATE ");
            }
        }

        sqlQuery.append(" ORDER BY company_name ASC ");
        sqlQuery.append(" LIMIT ? OFFSET ? ");

        int limit = size;
        int offset = page * size;

        parameters.add(limit);
        parameters.add(offset);

        return jdbcTemplate.query(
                con -> prepareStatement(con, sqlQuery.toString(), parameters),
                rowMapper
        );
    }

    private PreparedStatement prepareStatement(Connection con, String sqlQuery, List<Object> parameters) throws java.sql.SQLException {
        PreparedStatement preparedStatement = con.prepareStatement(sqlQuery);

        int index = 1;
        for (Object parameter : parameters) {
            if (parameter == null) {
                preparedStatement.setNull(index, Types.NULL);
            } else if (parameter instanceof List<?> listVal) {
                String[] array = listVal.stream().map(String::valueOf).toArray(String[]::new);
                preparedStatement.setArray(index, con.createArrayOf("text", array));
            } else if (parameter instanceof LocalDate localDate) {
                preparedStatement.setDate(index, Date.valueOf(localDate));
            } else if (parameter instanceof Integer integer) {
                preparedStatement.setInt(index, integer);
            } else if (parameter instanceof String string) {
                preparedStatement.setString(index, string);
            } else {
                preparedStatement.setObject(index, parameter);
            }
            index++;
        }


        return preparedStatement;
    }

    public CompanyDto insertCompany(
            String companyName,
            String careersUrl,
            LocalDate lastVisitedOn,
            int revisitAfterDays,
            List<String> tags
    ) {
        String sqlQuery = """
                INSERT INTO company_tracking (
                    company_name,
                    careers_url,
                    last_visited_on,
                    revisit_after_days,
                    tags
                )
                VALUES (?, ?, ?, ?, ?::text[])
                RETURNING
                    company_id,
                    company_name,
                    careers_url,
                    last_visited_on,
                    revisit_after_days,
                    tags,
                    next_visit_on,
                    created_at,
                    updated_at
                """;

        List<CompanyDto> results = jdbcTemplate.query(con -> {
            PreparedStatement ps = con.prepareStatement(sqlQuery);

            ps.setString(1, companyName);
            ps.setString(2, careersUrl);

            if (lastVisitedOn == null) {
                ps.setNull(3, Types.DATE);
            } else {
                ps.setDate(3, Date.valueOf(lastVisitedOn));
            }

            ps.setInt(4, revisitAfterDays);

            String[] array = tags.stream()
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .distinct()
                    .toArray(String[]::new);
            ps.setArray(5, con.createArrayOf("text", array));

            return ps;
        }, rowMapper);

        if (results.isEmpty()) {
            throw new RuntimeException("Failed to insert company");
        }

        return results.getFirst();
    }

    public CompanyDto updateCompany (
            UUID companyId,
            @Nullable String companyName,
            @Nullable String careersUrl,
            @Nullable LocalDate lastVisitedOn,
            @Nullable Integer revisitAfterDays,
            @Nullable List<String> tags
    ) {
        StringBuilder sqlQuery = new StringBuilder("""
                UPDATE company_tracking
                SET
                """);

        List<Object> parameters = new ArrayList<>();
        List<String> sets = new ArrayList<>();

        if (companyName != null) {
            sets.add("company_name = ?");
            parameters.add(companyName.trim());
        }

        if (careersUrl != null) {
            sets.add("careers_url = ?");
            parameters.add(careersUrl.trim());
        }

        if (lastVisitedOn != null) {
            sets.add("last_visited_on = ?");
            parameters.add(lastVisitedOn);
        }

        if (revisitAfterDays != null) {
            sets.add("revisit_after_days = ?");
            parameters.add(revisitAfterDays);
        }

        if (tags != null) {
            sets.add("tags = ?");
            parameters.add(tags);
        }

        if (sets.isEmpty()) {
            throw new IllegalArgumentException("No fields to update");
        }

        sets.add("updated_at = now()");

        sqlQuery.append(String.join(", ", sets));
        sqlQuery.append("""
                
                WHERE company_id = ?
                RETURNING
                    company_id,
                    company_name,
                    careers_url,
                    last_visited_on,
                    revisit_after_days,
                    tags,
                    next_visit_on,
                    created_at,
                    updated_at
                """);

        parameters.add(companyId);

        List<CompanyDto> results = jdbcTemplate.query( con -> {
                PreparedStatement ps = con.prepareStatement(sqlQuery.toString());

        int idx = 1;
        for (Object param : parameters) {
            if (param == null) {
                ps.setNull(idx, Types.NULL);
            } else if (param instanceof List<?> listVal) {
                // IMPORTANT: bind as SQL array explicitly
                String[] array = listVal.stream()
                        .map(String::valueOf)
                        .toArray(String[]::new);
                ps.setArray(idx, con.createArrayOf("text", array));
            } else if (param instanceof LocalDate d) {
                ps.setDate(idx, Date.valueOf(d));
            } else if (param instanceof Integer i) {
                ps.setInt(idx, i);
            } else if (param instanceof UUID u) {
                ps.setObject(idx, u);
            } else if (param instanceof String s) {
                ps.setString(idx, s);
            } else {
                ps.setObject(idx, param);
            }
            idx++;
        }

        return ps;
    }, rowMapper);

        return results.isEmpty() ? null : results.getFirst();
    }

    public CompanyDto markVisitedToday(UUID companyId) {
        String sqlQuery = """
                UPDATE company_tracking
                SET
                    last_visited_on = CURRENT_DATE,
                    updated_at = now()
                WHERE company_id = ?
                RETURNING
                    company_id,
                    company_name,
                    careers_url,
                    last_visited_on,
                    revisit_after_days,
                    tags,
                    next_visit_on,
                    created_at,
                    updated_at
                """;

        List<CompanyDto> results = jdbcTemplate.query(con -> {
            PreparedStatement preparedStatement = con.prepareStatement(sqlQuery);
            preparedStatement.setObject(1, companyId);
            return preparedStatement;
        }, rowMapper);

        return results.isEmpty() ? null : results.getFirst();
    }
}