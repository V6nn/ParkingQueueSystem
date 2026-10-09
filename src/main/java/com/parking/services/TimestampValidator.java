
package com.parking.services;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

final class TimestampValidator {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss")
                    .withResolverStyle(ResolverStyle.STRICT);

    private TimestampValidator() {
        // Prevent creating an instance of this utility class.
    }

    static void validate(String timestamp) {
        if (timestamp == null || timestamp.isBlank()) {
            throw new IllegalArgumentException(
                    "Record timestamp is required.");
        }

        try {
            LocalDateTime.parse(timestamp, FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Timestamp must use the format yyyy-MM-dd HH:mm:ss "
                            + "and contain a valid date and time.",
                    e);
        }
    }
}