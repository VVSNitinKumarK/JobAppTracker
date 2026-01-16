package com.jobapptracker.backend.company.repository;

import java.text.Normalizer;

public final class CompanyTagUtil {
    private CompanyTagUtil() {}

    public static String toTagKey(String raw) {
        if (raw == null) {
            return "";
        }

        String s = raw.trim().toLowerCase();

        s = Normalizer.normalize(s, Normalizer.Form.NFKD);

        s = s.replaceAll("[^a-z0-9]", "");

        return s;
    }
}
