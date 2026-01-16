package com.jobapptracker.backend.company.repository;

import com.jobapptracker.backend.company.dto.CompanyDto;
import com.jobapptracker.backend.company.service.CompanyCreationException;
import com.jobapptracker.backend.company.web.DueFilter;
import com.jobapptracker.backend.config.DatabaseConstants;
import com.jobapptracker.backend.config.SqlFragments;
import com.jobapptracker.backend.tag.dto.TagDto;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public class CompanyRepository {

    private static final Logger log = LoggerFactory.getLogger(CompanyRepository.class);

    private final JdbcTemplate jdbcTemplate;
    private final CompanyRowMapper rowMapper = new CompanyRowMapper();

    public CompanyRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
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
        String baseSql = "SELECT " + SqlFragments.SELECT_COMPANY_WITH_TAGS + "\n"
                + SqlFragments.FROM_COMPANY_WITH_TAGS + "\n"
                + "WHERE 1=1\n";

        StringBuilder sqlQuery = new StringBuilder(baseSql);
        List<Object> parameters = new ArrayList<>();

        applyFiltersToQuery(sqlQuery, parameters, q, tagsAny, due, date, lastVisitedOn);

        sqlQuery.append(SqlFragments.GROUP_BY_COMPANY);
        sqlQuery.append(SqlFragments.ORDER_BY_COMPANY_NAME);
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
            sqlQuery.append(" AND company.company_name ILIKE ? ESCAPE '\\' ");
            String escapedQuery = q.trim()
                    .replace("\\", "\\\\")
                    .replace("%", "\\%")
                    .replace("_", "\\_");
            parameters.add(escapedQuery + "%");
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
                case TODAY -> sqlQuery.append(" AND company.next_visit_on = CURRENT_DATE ");
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
        log.debug("Inserting company into database: name={}, url={}", companyName, careersUrl);

        String sqlQuery = """
                INSERT INTO %s (
                    company_name,
                    careers_url,
                    last_visited_on,
                    revisit_after_days
                )
                VALUES (?, ?, ?, ?)
                RETURNING company_id, company_name, careers_url, last_visited_on,
                          revisit_after_days, next_visit_on, created_at, updated_at
                """.formatted(DatabaseConstants.TABLE_COMPANY_TRACKING);

        CompanyDto inserted = jdbcTemplate.query(con -> {
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
        }, resultSet -> {
            if (!resultSet.next()) return null;
            return new CompanyDto(
                    resultSet.getObject("company_id", UUID.class),
                    resultSet.getString("company_name"),
                    resultSet.getString("careers_url"),
                    resultSet.getObject("last_visited_on", LocalDate.class),
                    resultSet.getInt("revisit_after_days"),
                    List.of(), // tags will be set below
                    resultSet.getObject("next_visit_on", LocalDate.class),
                    resultSet.getObject("created_at", java.time.OffsetDateTime.class),
                    resultSet.getObject("updated_at", java.time.OffsetDateTime.class)
            );
        });

        if (inserted == null) {
            log.error("Failed to insert company into database: name={}, url={}, lastVisitedOn={}, revisitAfterDays={}",
                    companyName, careersUrl, lastVisitedOn, revisitAfterDays);
            throw new CompanyCreationException(companyName, careersUrl, lastVisitedOn, revisitAfterDays);
        }

        log.debug("Company inserted successfully: id={}, name={}", inserted.companyId(), companyName);

        List<TagDto> tags = setCompanyTags(inserted.companyId(), tagNamesRaw);

        return new CompanyDto(
                inserted.companyId(),
                inserted.companyName(),
                inserted.careersUrl(),
                inserted.lastVisitedOn(),
                inserted.revisitAfterDays(),
                tags,
                inserted.nextVisitOn(),
                inserted.createdAt(),
                inserted.updatedAt()
        );
    }

    public CompanyDto updateCompany(
            UUID companyId,
            @Nullable String companyName,
            @Nullable String careersUrl,
            @Nullable LocalDate lastVisitedOn,
            @Nullable Integer revisitAfterDays,
            @Nullable List<String> tagNamesRaw
    ) {
        log.debug("Updating company in database: id={}", companyId);

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
        boolean hasTagUpdate = (tagNamesRaw != null);

        if (hasScalarUpdate || hasTagUpdate) {
            sets.add("updated_at = now()");
        }

        CompanyDto updated;
        if (!sets.isEmpty()) {
            String sqlQuery = """
                    UPDATE %s
                    SET %s
                    WHERE company_id = ?
                    RETURNING company_id, company_name, careers_url, last_visited_on,
                              revisit_after_days, next_visit_on, created_at, updated_at
                    """.formatted(
                    DatabaseConstants.TABLE_COMPANY_TRACKING,
                    String.join(", ", sets)
            );

            parameters.add(companyId);

            updated = jdbcTemplate.query(
                    con -> prepareStatement(con, sqlQuery, parameters),
                    resultSet -> {
                        if (!resultSet.next()) return null;
                        return new CompanyDto(
                                resultSet.getObject("company_id", UUID.class),
                                resultSet.getString("company_name"),
                                resultSet.getString("careers_url"),
                                resultSet.getObject("last_visited_on", LocalDate.class),
                                resultSet.getInt("revisit_after_days"),
                                List.of(), // tags handled separately
                                resultSet.getObject("next_visit_on", LocalDate.class),
                                resultSet.getObject("created_at", java.time.OffsetDateTime.class),
                                resultSet.getObject("updated_at", java.time.OffsetDateTime.class)
                        );
                    }
            );

            if (updated == null) {
                return null; // not found
            }
        } else {
            Integer exists = jdbcTemplate.query(
                    "SELECT 1 FROM " + DatabaseConstants.TABLE_COMPANY_TRACKING + " WHERE company_id = ?",
                    resultSet -> resultSet.next() ? 1 : null,
                    companyId
            );
            if (exists == null) {
                return null;
            }
            updated = findCompanyById(companyId);
            if (updated == null) {
                return null;
            }
        }

        List<TagDto> tags;
        if (hasTagUpdate) {
            tags = setCompanyTags(companyId, tagNamesRaw);
        } else {
            tags = fetchTagsForCompany(companyId);
        }

        log.debug("Company updated successfully in database: id={}", companyId);

        return new CompanyDto(
                updated.companyId(),
                updated.companyName(),
                updated.careersUrl(),
                updated.lastVisitedOn(),
                updated.revisitAfterDays(),
                tags,
                updated.nextVisitOn(),
                updated.createdAt(),
                updated.updatedAt()
        );
    }

    private List<TagDto> fetchTagsForCompany(UUID companyId) {
        String sqlQuery = """
                SELECT t.tag_key, t.tag_name
                FROM %s ct
                JOIN %s t ON t.tag_id = ct.tag_id
                WHERE ct.company_id = ?
                ORDER BY t.tag_name
                """.formatted(DatabaseConstants.TABLE_COMPANY_TAG, DatabaseConstants.TABLE_TAG);

        return jdbcTemplate.query(
                sqlQuery,
                (resultSet, rowNum) -> new TagDto(
                        resultSet.getString("tag_key"),
                        resultSet.getString("tag_name")
                ),
                companyId
        );
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
        String sqlQuery = "SELECT " + SqlFragments.SELECT_COMPANY_WITH_TAGS + "\n"
                + SqlFragments.FROM_COMPANY_WITH_TAGS + "\n"
                + "WHERE company.company_id = ?\n"
                + SqlFragments.GROUP_BY_COMPANY;

        List<CompanyDto> rows = jdbcTemplate.query(sqlQuery, rowMapper, companyId);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    private List<TagDto> setCompanyTags(UUID companyId, List<String> tagNamesRaw) {
        List<String> displayNames = normalizeTagNames(tagNamesRaw);

        if (displayNames.isEmpty()) {
            clearCompanyTags(companyId);
            return List.of();
        }

        List<UUID> newTagIds = fetchTagIdsByKeys(displayNames);
        List<UUID> currentTagIds = fetchCurrentTagIds(companyId);

        applyTagChanges(companyId, currentTagIds, newTagIds);

        return buildTagDtos(displayNames);
    }

    private List<String> normalizeTagNames(List<String> tagNamesRaw) {
        if (tagNamesRaw == null || tagNamesRaw.isEmpty()) {
            return List.of();
        }
        // Caller (service layer) already normalizes; this just ensures consistency
        return tagNamesRaw.stream()
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .distinct()
                .toList();
    }

    private void clearCompanyTags(UUID companyId) {
        jdbcTemplate.update(
                "DELETE FROM " + DatabaseConstants.TABLE_COMPANY_TAG + " WHERE company_id = ?",
                companyId
        );
    }

    private List<UUID> fetchTagIdsByKeys(List<String> displayNames) {
        List<String> keys = displayNames.stream()
                .map(CompanyTagUtil::toTagKey)
                .distinct()
                .toList();

        String sql = """
                SELECT tag_id FROM %s WHERE tag_key = ANY(?::text[])
                """.formatted(DatabaseConstants.TABLE_TAG);

        return jdbcTemplate.query(con -> {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setArray(1, con.createArrayOf("text", keys.toArray(String[]::new)));
            return ps;
        }, (rs, rowNum) -> rs.getObject("tag_id", UUID.class));
    }

    private List<UUID> fetchCurrentTagIds(UUID companyId) {
        String sql = """
                SELECT tag_id FROM %s WHERE company_id = ?
                """.formatted(DatabaseConstants.TABLE_COMPANY_TAG);

        return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getObject("tag_id", UUID.class), companyId);
    }

    private void applyTagChanges(UUID companyId, List<UUID> currentTagIds, List<UUID> newTagIds) {
        Set<UUID> currentSet = new HashSet<>(currentTagIds);
        Set<UUID> newSet = new HashSet<>(newTagIds);

        List<UUID> toRemove = currentTagIds.stream()
                .filter(id -> !newSet.contains(id))
                .toList();

        List<UUID> toAdd = newTagIds.stream()
                .filter(id -> !currentSet.contains(id))
                .toList();

        if (!toRemove.isEmpty()) {
            removeCompanyTags(companyId, toRemove);
        }
        if (!toAdd.isEmpty()) {
            addCompanyTags(companyId, toAdd);
        }
    }

    private void removeCompanyTags(UUID companyId, List<UUID> tagIds) {
        String sql = """
                DELETE FROM %s WHERE company_id = ? AND tag_id = ?
                """.formatted(DatabaseConstants.TABLE_COMPANY_TAG);

        jdbcTemplate.batchUpdate(sql, tagIds, tagIds.size(), (ps, tagId) -> {
            ps.setObject(1, companyId);
            ps.setObject(2, tagId);
        });
    }

    private void addCompanyTags(UUID companyId, List<UUID> tagIds) {
        String sql = """
                INSERT INTO %s (company_id, tag_id) VALUES (?, ?) ON CONFLICT DO NOTHING
                """.formatted(DatabaseConstants.TABLE_COMPANY_TAG);

        jdbcTemplate.batchUpdate(sql, tagIds, tagIds.size(), (ps, tagId) -> {
            ps.setObject(1, companyId);
            ps.setObject(2, tagId);
        });
    }

    private List<TagDto> buildTagDtos(List<String> displayNames) {
        return displayNames.stream()
                .map(name -> new TagDto(CompanyTagUtil.toTagKey(name), name))
                .toList();
    }
}
