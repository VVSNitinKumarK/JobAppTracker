package com.jobapptracker.backend.config;

import java.util.List;

public final class SqlFragments {

    private SqlFragments() {
        throw new UnsupportedOperationException("Utility class");
    }

    private static final List<String> COMPANY_COLUMNS = List.of(
            "company.company_id",
            "company.company_name",
            "company.careers_url",
            "company.last_visited_on",
            "company.revisit_after_days",
            "company.next_visit_on",
            "company.created_at",
            "company.updated_at"
    );

    private static final String TAG_AGGREGATIONS = """
                COALESCE(
                    array_agg(t.tag_key ORDER BY t.tag_name)
                        FILTER (WHERE t.tag_id IS NOT NULL),
                    '{}'::text[]
                ) AS tag_keys,
                COALESCE(
                    array_agg(t.tag_name ORDER BY t.tag_name)
                        FILTER (WHERE t.tag_id IS NOT NULL),
                    '{}'::text[]
                ) AS tag_names""";

    public static final String SELECT_COMPANY_WITH_TAGS =
            String.join(",\n            ", COMPANY_COLUMNS) + ",\n" + TAG_AGGREGATIONS;

    public static final String FROM_COMPANY_WITH_TAGS = """
            FROM %s company
            LEFT JOIN %s ct ON ct.company_id = company.company_id
            LEFT JOIN %s t ON t.tag_id = ct.tag_id""".formatted(
            DatabaseConstants.TABLE_COMPANY_TRACKING,
            DatabaseConstants.TABLE_COMPANY_TAG,
            DatabaseConstants.TABLE_TAG
    );

    public static final String GROUP_BY_COMPANY =
            " GROUP BY " + String.join(", ", COMPANY_COLUMNS);

    public static final String ORDER_BY_COMPANY_NAME = " ORDER BY company.company_name ASC ";
}