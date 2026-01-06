package com.jobapptracker.backend.company.repository;

import java.text.Normalizer;

public final class CompanyTagUtil {
    private CompanyTagUtil() {}

    public static String toTagKey(String raw) {
        if (raw == null) {
            return "";
        }

        String s = raw.trim().toLowerCase();

        // normalize unicode (optional but safe)
        s = Normalizer.normalize(s, Normalizer.Form.NFKD);

        // remove all non-alphanumeric characters
        s = s.replaceAll("[^a-z0-9]", "");

        return s;
    }
}
