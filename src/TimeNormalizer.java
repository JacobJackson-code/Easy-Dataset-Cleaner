import java.time.*;
import java.time.format.*;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.regex.*;

public class TimeNormalizer {

    // ── Patterns to try parsing ───────────────────────────────────────────────
    // Each pattern: formatter, hasSeconds, has12Hour, hasTimezone
    private static final List<DateTimeFormatter> PARSERS = new ArrayList<>();

    static {
        // 24-hour with seconds and timezone offset
        PARSERS.add(fmt("H:mm:ss Z"));
        PARSERS.add(fmt("H:mm:ss z"));
        PARSERS.add(fmt("H:mm:ss X"));
        PARSERS.add(fmt("HH:mm:ssZ"));
        PARSERS.add(fmt("HH:mm:ssX"));
        // 24-hour with seconds, no timezone
        PARSERS.add(fmt("H:mm:ss"));
        PARSERS.add(fmt("HH:mm:ss"));
        // 24-hour no seconds with timezone
        PARSERS.add(fmt("H:mm Z"));
        PARSERS.add(fmt("H:mm z"));
        PARSERS.add(fmt("H:mm X"));
        // 24-hour no seconds
        PARSERS.add(fmt("H:mm"));
        PARSERS.add(fmt("HH:mm"));
        // 12-hour with seconds and timezone
        PARSERS.add(fmt("h:mm:ss a Z"));
        PARSERS.add(fmt("h:mm:ss a z"));
        PARSERS.add(fmt("h:mm:ss a X"));
        PARSERS.add(fmt("h:mm:ssa Z"));
        PARSERS.add(fmt("h:mm:ssa z"));
        // 12-hour with seconds
        PARSERS.add(fmt("h:mm:ss a"));
        PARSERS.add(fmt("h:mm:ssa"));
        PARSERS.add(fmt("hh:mm:ss a"));
        // 12-hour no seconds with timezone
        PARSERS.add(fmt("h:mm a Z"));
        PARSERS.add(fmt("h:mm a z"));
        PARSERS.add(fmt("h:mm a X"));
        PARSERS.add(fmt("h:mma Z"));
        PARSERS.add(fmt("h:mma z"));
        // 12-hour no seconds
        PARSERS.add(fmt("h:mm a"));
        PARSERS.add(fmt("h:mma"));
        PARSERS.add(fmt("hh:mm a"));
        // Hour-only formats (no minutes): 2 PM, 14, 2PM
        PARSERS.add(fmt("h a"));
        PARSERS.add(fmt("ha"));
        PARSERS.add(fmt("hh a"));
        PARSERS.add(fmt("H"));
        PARSERS.add(fmt("HH"));
    }

    private static DateTimeFormatter fmt(String pattern) {
        return new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern(pattern)
                .toFormatter(Locale.ENGLISH);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NORMALIZE
    //
    // outputFormat:     "HH:mm" / "HH:mm:ss" / "hh:mm a" / "hh:mm:ss a"
    // timezoneHandling: "strip" / "preserve" / "UTC" / "America/New_York" etc.
    // globalTimezone:   assumed timezone for inputs with no timezone info
    //                   null = no assumption made
    // ─────────────────────────────────────────────────────────────────────────
    public String normalize(String input, String outputFormat,
                            String timezoneHandling, String globalTimezone) {
        if (input == null || input.trim().isEmpty()) return input;

        String trimmed = input.trim();

        // Fast pre-screen: must contain a digit and a colon or AM/PM to be a time
        boolean couldBeTime = false;
        String tl = trimmed.toLowerCase();
        if (tl.contains(":") || tl.contains("am") || tl.contains("pm")) {
            for (int ci = 0; ci < trimmed.length(); ci++) {
                if (Character.isDigit(trimmed.charAt(ci))) { couldBeTime = true; break; }
            }
        } else if (trimmed.matches("\\d{1,2}")) {
            couldBeTime = true; // bare hour like "14"
        }
        if (!couldBeTime) return "INVALID_TIME: " + trimmed + " (not a recognizable time)";

        // Pre-clean: normalize spacing around AM/PM and separators
        String cleaned = trimmed
                .replaceAll("(?i)([ap])\\.([m])\\.?", "$1$2") // a.m. → am
                .replaceAll("(?i)(\\d)(am|pm)", "$1 $2")       // 3pm → 3 pm
                .replaceAll("\\s+", " ")
                .trim();

        // Try to detect and extract timezone abbreviation from end
        // before attempting parse (some zone IDs confuse the parser)
        String detectedZone = null;
        cleaned = extractZoneAbbreviation(cleaned);

        // Attempt parse with each formatter
        OffsetTime offsetTime  = null;
        LocalTime  localTime   = null;

        for (DateTimeFormatter parser : PARSERS) {
            try {
                TemporalAccessor ta = parser.parseBest(cleaned,
                        OffsetTime::from, LocalTime::from);
                if (ta instanceof OffsetTime) {
                    offsetTime = (OffsetTime) ta;
                    break;
                } else if (ta instanceof LocalTime) {
                    localTime = (LocalTime) ta;
                    break;
                }
            } catch (Exception ignored) {}
        }

        if (offsetTime == null && localTime == null) {
            return "INVALID_TIME: " + trimmed;
        }

        // Convert to ZonedTime for timezone operations if needed
        ZoneId targetZone = resolveZone(timezoneHandling);

        // Build output
        try {
            ZonedDateTime zdt = null;

            if (offsetTime != null) {
                // We have timezone offset info
                ZonedDateTime base = offsetTime.atDate(LocalDate.now())
                        .atZoneSameInstant(offsetTime.getOffset());

                if (timezoneHandling != null
                        && !timezoneHandling.equalsIgnoreCase("strip")
                        && !timezoneHandling.equalsIgnoreCase("preserve")
                        && targetZone != null) {
                    zdt = base.withZoneSameInstant(targetZone);
                } else {
                    zdt = base;
                }
            } else {
                // Local time — no timezone in input
                if (globalTimezone != null && !globalTimezone.isEmpty()
                        && !timezoneHandling.equalsIgnoreCase("strip")) {
                    try {
                        ZoneId sourceZone = ZoneId.of(globalTimezone);
                        ZonedDateTime sourceZdt = localTime.atDate(LocalDate.now())
                                .atZone(sourceZone);
                        if (targetZone != null
                                && !timezoneHandling.equalsIgnoreCase("preserve")) {
                            zdt = sourceZdt.withZoneSameInstant(targetZone);
                        } else {
                            zdt = sourceZdt;
                        }
                    } catch (Exception e) {
                        // Bad globalTimezone config — fall back to local time
                    }
                }
            }

            // Format output
            DateTimeFormatter outputFmt = DateTimeFormatter.ofPattern(
                    outputFormat, Locale.ENGLISH);

            if (zdt != null) {
                String base = zdt.format(DateTimeFormatter.ofPattern(
                        stripZonePart(outputFormat), Locale.ENGLISH));

                if (timezoneHandling != null
                        && !timezoneHandling.equalsIgnoreCase("strip")) {
                    if (timezoneHandling.equalsIgnoreCase("preserve")) {
                        // Append original offset if we had one
                        if (offsetTime != null) {
                            return base + " " + offsetTime.getOffset().toString();
                        }
                        return base;
                    }
                    // Append converted timezone
                    return base + " " + zdt.getZone().toString();
                }
                return base;
            } else {
                // Pure local time formatting
                LocalTime lt = (localTime != null) ? localTime
                        : offsetTime.toLocalTime();
                return lt.format(DateTimeFormatter.ofPattern(
                        stripZonePart(outputFormat), Locale.ENGLISH));
            }

        } catch (Exception e) {
            return "INVALID_TIME: " + trimmed + " (" + e.getMessage() + ")";
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    // Resolves a timezone handling string to a ZoneId (null for strip/preserve)
    private ZoneId resolveZone(String timezoneHandling) {
        if (timezoneHandling == null) return null;
        if (timezoneHandling.equalsIgnoreCase("strip"))    return null;
        if (timezoneHandling.equalsIgnoreCase("preserve")) return null;
        try { return ZoneId.of(timezoneHandling); }
        catch (Exception e) {
            System.out.println("WARNING: unrecognized timezone '"
                    + timezoneHandling + "' — treating as strip");
            return null;
        }
    }

    // Strips timezone format codes from output format string
    // so we can append them manually
    private String stripZonePart(String format) {
        return format.replaceAll("\\s*[Zz Xx VV]+$", "").trim();
    }

    // Handles common timezone abbreviations that Java doesn't parse natively
    // e.g. EST, PST, CST, IST — strips them and returns cleaned string
    private String extractZoneAbbreviation(String input) {
        // Strip trailing timezone abbreviations like EST, PST, GMT etc.
        return input.replaceAll("(?i)\\s+(EST|EDT|CST|CDT|MST|MDT|PST|PDT|GMT|"
                        + "IST|JST|AEST|AEDT|CET|CEST|BST|EET|EEST|HKT|SGT|WIB|KST)$", "")
                .trim();
    }
}