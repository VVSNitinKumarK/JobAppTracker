package com.jobapptracker.backend.config;

import com.jobapptracker.backend.tag.dto.TagDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Array;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class SqlArrayUtils {

    private static final Logger log = LoggerFactory.getLogger(SqlArrayUtils.class);

    private SqlArrayUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static List<String> toStringList(Array sqlArray) throws SQLException {
        if (sqlArray == null) {
            return List.of();
        }
        Object arrayObject = sqlArray.getArray();
        if (arrayObject == null) {
            return List.of();
        }
        return Arrays.asList((String[]) arrayObject);
    }

    public static List<TagDto> zipTags(List<String> keys, List<String> names) {
        if (keys.size() != names.size()) {
            log.error("Tag array size mismatch: keys.size()={}, names.size()={}. " +
                      "This indicates a bug in the SQL ORDER BY clause.", keys.size(), names.size());
            throw new IllegalStateException(
                    "Tag arrays must have same size. keys=" + keys.size() + ", names=" + names.size()
            );
        }

        List<TagDto> result = new ArrayList<>(keys.size());

        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            String name = names.get(i);

            if (key != null && !key.isBlank() && name != null && !name.isBlank()) {
                result.add(new TagDto(key.trim(), name.trim()));
            }
        }

        return result;
    }
}