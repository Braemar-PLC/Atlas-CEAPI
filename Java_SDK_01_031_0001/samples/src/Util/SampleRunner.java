package Util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class to run a sample for a specified duration and then trigger its exit.
 */
public class SampleRunner {

    private static final Logger LOGGER = Logger.getLogger(SampleRunner.class.getName());

    /**
     * Runs the provided sample for the specified time duration and schedules its exit.
     *
     * @param time   The duration in the format "HH:MM:SS".
     * @param sample The sample object to be managed. Must have an "exitSample" method.
     * @throws IllegalArgumentException if the time format is invalid.
     */
    public static void runSampleForTime(String time, Object sample) {
        validateTimeFormat(time);
        long delay = convertTimeToSeconds(time);
        scheduleExit(sample, delay);
    }

    /**
     * Converts a time string in "HH:MM:SS" format to seconds.
     *
     * @param time The time string.
     * @return The total time in seconds.
     */
    private static long convertTimeToSeconds(String time) {
        String[] parts = time.split(":");
        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);
        int seconds = Integer.parseInt(parts[2]);
        return hours * 3600 + minutes * 60 + seconds;
    }

    /**
     * Schedules the exit of the provided sample after the specified delay.
     *
     * @param sample The sample object.
     * @param delay  The delay in seconds before calling "exitSample".
     */
    private static void scheduleExit(final Object sample, long delay) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.schedule(() -> {
            try {
                sample.getClass().getMethod("exitSample", boolean.class).invoke(sample, true);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to invoke exitSample method on the sample object.", e);
            } finally {
                scheduler.shutdown();
            }
        }, delay, TimeUnit.SECONDS);
    }

    /**
     * Validates the format of the time string.
     *
     * @param time The time string to validate.
     * @throws IllegalArgumentException if the time string is not in "HH:MM:SS" format.
     */
    private static void validateTimeFormat(String time) {
        if (!time.matches("\\d{2}:\\d{2}:\\d{2}")) {
            throw new IllegalArgumentException("Invalid time format. Expected format is HH:MM:SS.");
        }
    }
}
