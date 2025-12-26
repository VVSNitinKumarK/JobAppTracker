package com.jobapptracker.backend.company.web;

public enum DueFilter {
        TODAY,
        OVERDUE,
        UPCOMING;

        public static DueFilter fromStringOrNull(String raw) {
            if (raw == null || raw.isBlank()) {
                return null;
            }

            return switch (raw.trim().toLowerCase()) {
                case "today" -> TODAY;
                case "overdue" -> OVERDUE;
                case "upcoming" -> UPCOMING;
                default -> throw new IllegalStateException("Invalid due value: " + raw + " (allowed: today, overdue, upcoming)");
            };
        }
}
