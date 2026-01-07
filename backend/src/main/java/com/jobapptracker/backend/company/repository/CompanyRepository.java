package com.jobapptracker.backend.company.repository;

import com.jobapptracker.backend.company.dto.CompanyDto;
import com.jobapptracker.backend.company.web.DueFilter;
import com.jobapptracker.backend.tag.repository.TagRepository;
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
import java.util.Objects;
import java.util.UUID;

@Repository
public class CompanyRepository {

    private final JdbcTemplate jdbcTemplate;
    private final TagRepository tagRepository;
    private final CompanyRowMapper rowMapper = new CompanyRowMapper();

    public CompanyRepository(JdbcTemplate template, TagRepository tagRepository) {
        this.jdbcTemplate = template;
        this.tagRepository = tagRepository;
    }

    public List<CompanyDto> findCompanies(
            int page,
            int size,
            String q,
            List<String> tagsAny,
            DueFilter due,
            LocalDate date,
            LocalDate lastVisitedOn
    ) {
        boolean hasDateFilter = (date != null);
        DueFilter effectiveDue = hasDateFilter ? null : due;

        StringBuilder sqlQuery = new StringBuilder("""
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
                    company.updated_at
                FROM jobapps.company_tracking company
                LEFT JOIN jobapps.company_tag ct ON ct.company_id = company.company_id
                LEFT JOIN jobapps.tag t ON t.tag_id = ct.tag_id
                WHERE 1=1
                """);

        List<Object> parameters = new ArrayList<>();

        if (q != null && !q.isBlank()) {
            sqlQuery.append(" AND company.company_name ILIKE ? ");
            parameters.add(q.trim() + "%");
        }

        // Filter by tag KEYs (OR / any-of)
        if (tagsAny != null && !tagsAny.isEmpty()) {
            sqlQuery.append("""
                    AND EXISTS (
                        SELECT 1
                        FROM jobapps.company_tag ct2
                        JOIN jobapps.tag t2 ON t2.tag_id = ct2.tag_id
                        WHERE ct2.company_id = company.company_id
                          AND t2.tag_key = ANY(?::text[])
                    )
                    """);
            parameters.add(tagsAny);
        }

        if (hasDateFilter) {
            sqlQuery.append(" AND company.next_visit_on = ? ");
            parameters.add(date);
        }

        if (lastVisitedOn != null) {
            sqlQuery.append(" AND company.last_visited_on = ? ");
            parameters.add(lastVisitedOn);
        }

        if (effectiveDue != null) {
            switch (effectiveDue) {
                case TODAY -> sqlQuery.append(" AND company.next_visit_on <= CURRENT_DATE ");
                case OVERDUE -> sqlQuery.append(" AND company.next_visit_on < CURRENT_DATE ");
                case UPCOMING -> sqlQuery.append(" AND company.next_visit_on > CURRENT_DATE ");
            }
        }

        sqlQuery.append("""
                GROUP BY
                    company.company_id,
                    company.company_name,
                    company.careers_url,
                    company.last_visited_on,
                    company.revisit_after_days,
                    company.next_visit_on,
                    company.created_at,
                    company.updated_at
                """);

        sqlQuery.append(" ORDER BY company.company_name ASC ");
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

    private PreparedStatement prepareStatement(Connection con, String sqlQuery, List<Object> parameters)
            throws java.sql.SQLException {

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
            } else if (parameter instanceof UUID uuid) {
                preparedStatement.setObject(index, uuid);
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
            List<String> tagNamesRaw
    ) {
        String sqlQuery = """
                INSERT INTO jobapps.company_tracking (
                    company_name,
                    careers_url,
                    last_visited_on,
                    revisit_after_days
                )
                VALUES (?, ?, ?, ?)
                RETURNING company_id
                """;

        UUID companyId = jdbcTemplate.query(con -> {
            PreparedStatement preparedStatement = con.prepareStatement(sqlQuery);
            preparedStatement.setString(1, companyName);
            preparedStatement.setString(2, careersUrl);

            if (lastVisitedOn == null) {
                preparedStatement.setNull(3, Types.DATE);
            } else {
                preparedStatement.setDate(3, Date.valueOf(lastVisitedOn));
            }

            preparedStatement.setInt(4, revisitAfterDays);
            return preparedStatement;
        }, resultSet -> resultSet.next() ? resultSet.getObject("company_id", UUID.class) : null);

        if (companyId == null) {
            throw new RuntimeException("Failed to insert company");
        }

        // Tags now live in jobapps.tag + jobapps.company_tag
        setCompanyTags(companyId, tagNamesRaw);

        return findCompanyById(companyId);
    }

    public CompanyDto updateCompany(
            UUID companyId,
            @Nullable String companyName,
            @Nullable String careersUrl,
            @Nullable LocalDate lastVisitedOn,
            @Nullable Integer revisitAfterDays,
            @Nullable List<String> tagNamesRaw
    ) {
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

        boolean hasScalarUpdate = !sets.isEmpty();

        if (hasScalarUpdate) {
            sets.add("updated_at = now()");

            String sqlQuery = """
                    UPDATE jobapps.company_tracking
                    SET %s
                    WHERE company_id = ?
                    """.formatted(String.join(", ", sets));

            parameters.add(companyId);

            int updated = jdbcTemplate.update(con -> prepareStatement(con, sqlQuery, parameters));
            if (updated == 0) {
                return null; // not found
            }
        } else {
            // No scalar fields; still must verify company exists if tags update might happen.
            Integer exists = jdbcTemplate.query(
                    "SELECT 1 FROM jobapps.company_tracking WHERE company_id = ?",
                    resultSet -> resultSet.next() ? 1 : null,
                    companyId
            );
            if (exists == null) {
                return null;
            }
        }

        if (tagNamesRaw != null) {
            setCompanyTags(companyId, tagNamesRaw);
            jdbcTemplate.update(
                    "UPDATE jobapps.company_tracking SET updated_at = now() WHERE company_id = ?",
                    companyId
            );
        }

        return findCompanyById(companyId);
    }

    public CompanyDto markVisitedToday(UUID companyId) {
        String sqlQuery = """
                UPDATE jobapps.company_tracking
                SET
                    last_visited_on = CURRENT_DATE,
                    updated_at = now()
                WHERE company_id = ?
                RETURNING company_id
                """;

        UUID updatedId = jdbcTemplate.query(con -> {
            PreparedStatement preparedStatement = con.prepareStatement(sqlQuery);
            preparedStatement.setObject(1, companyId);
            return preparedStatement;
        }, resultSet -> resultSet.next() ? resultSet.getObject("company_id", UUID.class) : null);

        if (updatedId == null) {
            return null;
        }

        return findCompanyById(companyId);
    }

    public int deleteCompany(UUID companyId) {
        String sqlQuery = """
                DELETE FROM jobapps.company_tracking
                WHERE company_id = ?
                """;

        return jdbcTemplate.update(con -> {
            PreparedStatement preparedStatement = con.prepareStatement(sqlQuery);
            preparedStatement.setObject(1, companyId);
            return preparedStatement;
        });
    }

    private CompanyDto findCompanyById(UUID companyId) {
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
                    company.updated_at
                FROM jobapps.company_tracking company
                LEFT JOIN jobapps.company_tag ct ON ct.company_id = company.company_id
                LEFT JOIN jobapps.tag t ON t.tag_id = ct.tag_id
                WHERE company.company_id = ?
                GROUP BY
                    company.company_id,
                    company.company_name,
                    company.careers_url,
                    company.last_visited_on,
                    company.revisit_after_days,
                    company.next_visit_on,
                    company.created_at,
                    company.updated_at
                """;

        List<CompanyDto> rows = jdbcTemplate.query(sqlQuery, rowMapper, companyId);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    /**
     * Takes raw "display names" from the client (e.g., "Big Tech", "backend", "ML infra"),
     * creates tags if missing (by tag_key), then sets the mapping for the company.
     *
     * NOTE: This expects CompanyTagUtil.toTagKey(...) to normalize into a stable key (e.g. "big-tech" or "bigtech").
     */
    private void setCompanyTags(UUID companyId, List<String> tagNamesRaw) {
        if (tagNamesRaw == null) {
            tagNamesRaw = List.of();
        }

        List<String> displayNames = tagNamesRaw.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .distinct()
                .toList();

        if (displayNames.isEmpty()) {
            jdbcTemplate.update("DELETE FROM jobapps.company_tag WHERE company_id = ?", companyId);
            return;
        }

        // Ensure tags exist using TagRepository (removes duplication)
        tagRepository.ensureTagsExist(displayNames);

        // Generate keys for fetching tag IDs
        List<String> keys = displayNames.stream()
                .map(CompanyTagUtil::toTagKey)
                .distinct()
                .toList();

        // Fetch tag_ids for these keys
        String fetchSql = """
                SELECT tag_id
                FROM jobapps.tag
                WHERE tag_key = ANY(?::text[])
                """;

        List<UUID> tagIds = jdbcTemplate.query(con -> {
            PreparedStatement preparedStatement = con.prepareStatement(fetchSql);
            preparedStatement.setArray(1, con.createArrayOf("text", keys.toArray(String[]::new)));
            return preparedStatement;
        }, (resultSet, rowNum) -> resultSet.getObject("tag_id", UUID.class));

        // Replace mappings
        jdbcTemplate.update("DELETE FROM jobapps.company_tag WHERE company_id = ?", companyId);

        String insertMapSql = """
                INSERT INTO jobapps.company_tag (company_id, tag_id)
                VALUES (?, ?)
                ON CONFLICT DO NOTHING
                """;

        jdbcTemplate.batchUpdate(insertMapSql, tagIds, tagIds.size(), (preparedStatement, tagId) -> {
            preparedStatement.setObject(1, companyId);
            preparedStatement.setObject(2, tagId);
        });
    }
}
