package com.jobapptracker.backend.company.repository;

import com.jobapptracker.backend.company.dto.CompanyDto;
import com.jobapptracker.backend.company.web.DueFilter;
import com.jobapptracker.backend.config.DatabaseConstants;
import com.jobapptracker.backend.tag.repository.TagRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(CompanyRepository.class);

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
        String baseSql = """
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
                FROM %s company
                LEFT JOIN %s ct ON ct.company_id = company.company_id
                LEFT JOIN %s t ON t.tag_id = ct.tag_id
                WHERE 1=1
                """.formatted(
                DatabaseConstants.TABLE_COMPANY_TRACKING,
                DatabaseConstants.TABLE_COMPANY_TAG,
                DatabaseConstants.TABLE_TAG
        );

        StringBuilder sqlQuery = new StringBuilder(baseSql);
        List<Object> parameters = new ArrayList<>();

        applyFiltersToQuery(sqlQuery, parameters, q, tagsAny, due, date, lastVisitedOn);

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

        long offsetLong = (long) page * size;
        if (offsetLong > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Offset overflow: page=" + page + ", size=" + size +
                    " results in offset=" + offsetLong + " which exceeds Integer.MAX_VALUE");
        }
        int offset = (int) offsetLong;

        parameters.add(limit);
        parameters.add(offset);

        return jdbcTemplate.query(
                con -> prepareStatement(con, sqlQuery.toString(), parameters),
                rowMapper
        );
    }

    public long countCompanies(
            String q,
            List<String> tagsAny,
            DueFilter due,
            LocalDate date,
            LocalDate lastVisitedOn
    ) {
        String baseSql = """
                SELECT COUNT(DISTINCT company.company_id)
                FROM %s company
                LEFT JOIN %s ct ON ct.company_id = company.company_id
                LEFT JOIN %s t ON t.tag_id = ct.tag_id
                WHERE 1=1
                """.formatted(
                DatabaseConstants.TABLE_COMPANY_TRACKING,
                DatabaseConstants.TABLE_COMPANY_TAG,
                DatabaseConstants.TABLE_TAG
        );

        StringBuilder sqlQuery = new StringBuilder(baseSql);
        List<Object> parameters = new ArrayList<>();

        applyFiltersToQuery(sqlQuery, parameters, q, tagsAny, due, date, lastVisitedOn);

        Long count = jdbcTemplate.queryForObject(
                sqlQuery.toString(),
                Long.class,
                parameters.toArray()
        );

        return (count != null) ? count : 0L;
    }

    private void applyFiltersToQuery(
            StringBuilder sqlQuery,
            List<Object> parameters,
            String q,
            List<String> tagsAny,
            DueFilter due,
            LocalDate date,
            LocalDate lastVisitedOn
    ) {
        boolean hasDateFilter = (date != null);
        DueFilter effectiveDue = hasDateFilter ? null : due;

        if (q != null && !q.isBlank()) {
            sqlQuery.append(" AND company.company_name ILIKE ? ");
            parameters.add(q.trim() + "%");
        }

        if (tagsAny != null && !tagsAny.isEmpty()) {
            String tagsFilter = """
                    AND EXISTS (
                        SELECT 1
                        FROM %s ct2
                        JOIN %s t2 ON t2.tag_id = ct2.tag_id
                        WHERE ct2.company_id = company.company_id
                          AND t2.tag_key = ANY(?::text[])
                    )
                    """.formatted(
                    DatabaseConstants.TABLE_COMPANY_TAG,
                    DatabaseConstants.TABLE_TAG
            );
            sqlQuery.append(tagsFilter);
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
        log.info("Inserting company into database: name={}, url={}", companyName, careersUrl);

        String sqlQuery = """
                INSERT INTO %s (
                    company_name,
                    careers_url,
                    last_visited_on,
                    revisit_after_days
                )
                VALUES (?, ?, ?, ?)
                RETURNING company_id
                """.formatted(DatabaseConstants.TABLE_COMPANY_TRACKING);

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
            log.error("Failed to insert company into database: name={}, url={}, lastVisitedOn={}, revisitAfterDays={}",
                    companyName, careersUrl, lastVisitedOn, revisitAfterDays);
            throw new RuntimeException("Failed to insert company: companyName='" + companyName +
                    "', careersUrl='" + careersUrl +
                    "', lastVisitedOn=" + lastVisitedOn +
                    ", revisitAfterDays=" + revisitAfterDays);
        }

        log.info("Company inserted successfully: id={}, name={}", companyId, companyName);
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
        log.info("Updating company in database: id={}", companyId);

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
                    UPDATE %s
                    SET %s
                    WHERE company_id = ?
                    """.formatted(
                    DatabaseConstants.TABLE_COMPANY_TRACKING,
                    String.join(", ", sets)
            );

            parameters.add(companyId);

            int updated = jdbcTemplate.update(con -> prepareStatement(con, sqlQuery, parameters));
            if (updated == 0) {
                return null; // not found
            }
        } else {
            // FIXED: this string was broken in your code
            Integer exists = jdbcTemplate.query(
                    "SELECT 1 FROM " + DatabaseConstants.TABLE_COMPANY_TRACKING + " WHERE company_id = ?",
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
                    "UPDATE " + DatabaseConstants.TABLE_COMPANY_TRACKING + " SET updated_at = now() WHERE company_id = ?",
                    companyId
            );
        }

        log.info("Company updated successfully in database: id={}", companyId);
        return findCompanyById(companyId);
    }

    public int deleteCompany(UUID companyId) {
        log.info("Deleting company from database: id={}", companyId);

        String sqlQuery = """
                DELETE FROM %s
                WHERE company_id = ?
                """.formatted(DatabaseConstants.TABLE_COMPANY_TRACKING);

        int deleted = jdbcTemplate.update(con -> {
            PreparedStatement preparedStatement = con.prepareStatement(sqlQuery);
            preparedStatement.setObject(1, companyId);
            return preparedStatement;
        });

        if (deleted > 0) {
            log.info("Company deleted successfully from database: id={}", companyId);
        }

        return deleted;
    }

    public int deleteCompanies(List<UUID> companyIds) {
        if (companyIds == null || companyIds.isEmpty()) {
            return 0;
        }

        log.info("Batch deleting {} companies from database", companyIds.size());

        String sqlQuery = """
                DELETE FROM %s
                WHERE company_id = ANY(?::uuid[])
                """.formatted(DatabaseConstants.TABLE_COMPANY_TRACKING);

        int deleted = jdbcTemplate.update(con -> {
            PreparedStatement preparedStatement = con.prepareStatement(sqlQuery);
            UUID[] uuidArray = companyIds.toArray(new UUID[0]);
            preparedStatement.setArray(1, con.createArrayOf("uuid", uuidArray));
            return preparedStatement;
        });

        log.info("Batch delete completed: {} companies deleted from database", deleted);
        return deleted;
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
                FROM %s company
                LEFT JOIN %s ct ON ct.company_id = company.company_id
                LEFT JOIN %s t ON t.tag_id = ct.tag_id
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
                """.formatted(
                DatabaseConstants.TABLE_COMPANY_TRACKING,
                DatabaseConstants.TABLE_COMPANY_TAG,
                DatabaseConstants.TABLE_TAG
        );

        List<CompanyDto> rows = jdbcTemplate.query(sqlQuery, rowMapper, companyId);
        return rows.isEmpty() ? null : rows.getFirst();
    }

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
            jdbcTemplate.update("DELETE FROM " + DatabaseConstants.TABLE_COMPANY_TAG + " WHERE company_id = ?", companyId);
            return;
        }

        tagRepository.ensureTagsExist(displayNames);

        List<String> keys = displayNames.stream()
                .map(CompanyTagUtil::toTagKey)
                .distinct()
                .toList();

        String fetchSql = """
                SELECT tag_id
                FROM %s
                WHERE tag_key = ANY(?::text[])
                """.formatted(DatabaseConstants.TABLE_TAG);

        List<UUID> newTagIds = jdbcTemplate.query(con -> {
            PreparedStatement preparedStatement = con.prepareStatement(fetchSql);
            preparedStatement.setArray(1, con.createArrayOf("text", keys.toArray(String[]::new)));
            return preparedStatement;
        }, (resultSet, rowNum) -> resultSet.getObject("tag_id", UUID.class));

        String currentTagsSql = """
                SELECT tag_id
                FROM %s
                WHERE company_id = ?
                """.formatted(DatabaseConstants.TABLE_COMPANY_TAG);

        List<UUID> currentTagIds = jdbcTemplate.query(
                currentTagsSql,
                (resultSet, rowNum) -> resultSet.getObject("tag_id", UUID.class),
                companyId
        );

        List<UUID> toAdd = newTagIds.stream()
                .filter(id -> !currentTagIds.contains(id))
                .toList();

        List<UUID> toRemove = currentTagIds.stream()
                .filter(id -> !newTagIds.contains(id))
                .toList();

        if (!toRemove.isEmpty()) {
            String deleteSql = """
                    DELETE FROM %s
                    WHERE company_id = ? AND tag_id = ?
                    """.formatted(DatabaseConstants.TABLE_COMPANY_TAG);

            jdbcTemplate.batchUpdate(deleteSql, toRemove, toRemove.size(), (preparedStatement, tagId) -> {
                preparedStatement.setObject(1, companyId);
                preparedStatement.setObject(2, tagId);
            });
        }

        if (!toAdd.isEmpty()) {
            String insertMapSql = """
                    INSERT INTO %s (company_id, tag_id)
                    VALUES (?, ?)
                    ON CONFLICT DO NOTHING
                    """.formatted(DatabaseConstants.TABLE_COMPANY_TAG);

            jdbcTemplate.batchUpdate(insertMapSql, toAdd, toAdd.size(), (preparedStatement, tagId) -> {
                preparedStatement.setObject(1, companyId);
                preparedStatement.setObject(2, tagId);
            });
        }
    }
}
