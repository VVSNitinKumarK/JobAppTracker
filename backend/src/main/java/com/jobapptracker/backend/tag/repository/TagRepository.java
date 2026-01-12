package com.jobapptracker.backend.tag.repository;

import com.jobapptracker.backend.company.repository.CompanyTagUtil;
import com.jobapptracker.backend.config.DatabaseConstants;
import com.jobapptracker.backend.config.PaginationConstants;
import com.jobapptracker.backend.tag.dto.TagDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class TagRepository {

    private static final Logger log = LoggerFactory.getLogger(TagRepository.class);

    private final JdbcTemplate jdbcTemplate;

    public TagRepository(JdbcTemplate template) {
        this.jdbcTemplate = template;
    }

    public List<TagDto> listAll() {

        String sqlQuery = """
            SELECT tag_key, tag_name
            FROM %s
            ORDER BY tag_name ASC
        """.formatted(DatabaseConstants.TABLE_TAG);

        return jdbcTemplate.query(sqlQuery, (resultSet, rowNum) ->
                new TagDto(
                        resultSet.getString("tag_key"),
                        resultSet.getString("tag_name")
                )
        );
    }

    /**
     * Ensures tags exist in the database by their display names.
     * Creates tag_key by normalizing the display name.
     *
     * @param tagDisplayNames List of tag display names (e.g., "Big Tech", "ML/AI")
     */
    public void ensureTagsExist(List<String> tagDisplayNames) {
        if (tagDisplayNames == null || tagDisplayNames.isEmpty()) {
            return;
        }

        log.info("Ensuring {} tags exist in database", tagDisplayNames.size());

        String sqlQuery = """
            INSERT INTO %s (tag_key, tag_name)
            VALUES (?, ?)
            ON CONFLICT (tag_key) DO NOTHING
        """.formatted(DatabaseConstants.TABLE_TAG);

        jdbcTemplate.batchUpdate(
                sqlQuery,
                tagDisplayNames,
                PaginationConstants.MAX_PAGE_SIZE,
                (prepareStatement, displayName) -> {
                    String key = CompanyTagUtil.toTagKey(displayName);
                    prepareStatement.setString(1, key);
                    prepareStatement.setString(2, displayName);
                }
        );

        log.info("Tags ensured successfully in database: {} tags processed", tagDisplayNames.size());
    }
}
