package com.jobapptracker.backend.tag.repository;

import com.jobapptracker.backend.tag.dto.TagDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class TagRepository {

    private final JdbcTemplate jdbcTemplate;

    public TagRepository(JdbcTemplate template) {
        this.jdbcTemplate = template;
    }

    public List<TagDto> listAll() {

        String sqlQuery = """
            SELECT tag_key, tag_name
            FROM jobapps.tag
            ORDER BY tag_name ASC
        """;

        return jdbcTemplate.query(sqlQuery, (resultSet, rowNum) ->
                new TagDto(
                        resultSet.getString("tag_key"),
                        resultSet.getString("tag_name")
                )
        );
    }

    public void ensureTagExist(List<String> tagKeys) {
        if (tagKeys == null || tagKeys.isEmpty()) {
            return;
        }

        String sqlQuery = """
            INSERT INTO jobapps.tag (tag_key, tag_name)
            VALUES (?, ?)
            ON CONFLICT (tag_key) DO NOTHING
        """;

        jdbcTemplate.batchUpdate(sqlQuery, tagKeys, 200, (prepareStatement, key) -> {
            prepareStatement.setString(1, key);
            prepareStatement.setString(2, key);
        });
    }
}
