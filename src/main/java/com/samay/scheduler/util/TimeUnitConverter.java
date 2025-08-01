package com.samay.scheduler.util;

import com.samay.scheduler.persistence.thread.ScheduledThreadConfig;
import org.springframework.cglib.core.Local;

import java.time.*;

public class TimeUnitConverter {

    public static Duration convertToDuration(Long value, String unit) {
        return switch (unit.toUpperCase()) {
            case "MINUTES" -> Duration.ofMinutes(value);
            case "HOURS" -> Duration.ofHours(value);
            case "DAYS" -> Duration.ofDays(value);
            case "WEEKS" -> Duration.ofDays(value * 7);
            case "MONTHS" -> Duration.ofDays(value * 30);
            default -> throw new IllegalArgumentException("Unsupported frequency unit");
        };
    }

    public static ZonedDateTime getNextRunTimeDaily(LocalTime timeOfDay) {
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime next = now.with(timeOfDay);
        return now.isAfter(next) ? next.plusDays(1) : next;
    }

    public static ZonedDateTime getNextRunTimeWeekly(DayOfWeek dayOfWeek, LocalTime timeOfDay) {
        LocalDate now = LocalDate.now();
        int daysUntil = (dayOfWeek.getValue() - now.getDayOfWeek().getValue() + 7) % 7;
        return now.plusDays(daysUntil).atTime(timeOfDay).atZone(ZoneId.systemDefault());
    }

    public static ZonedDateTime getNextRunTimeMonthly(Integer dayOfMonth, LocalTime timeOfDay) {
        LocalDate now = LocalDate.now();
        LocalDate next = now.withDayOfMonth(Math.min(dayOfMonth, now.lengthOfMonth()));
        if (now.getDayOfMonth() >= dayOfMonth) {
            next = next.plusMonths(1).withDayOfMonth(Math.min(dayOfMonth, next.lengthOfMonth()));
        }
        return next.atTime(timeOfDay).atZone(ZoneId.systemDefault());
    }
}
