package com.workrh.telework.service;

import java.time.LocalDate;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class LuxembourgHolidayService {

    public boolean isHoliday(LocalDate date) {
        Set<LocalDate> fixedHolidays = Set.of(
                LocalDate.of(date.getYear(), 1, 1),
                LocalDate.of(date.getYear(), 5, 1),
                LocalDate.of(date.getYear(), 6, 23),
                LocalDate.of(date.getYear(), 8, 15),
                LocalDate.of(date.getYear(), 11, 1),
                LocalDate.of(date.getYear(), 12, 25),
                LocalDate.of(date.getYear(), 12, 26)
        );
        return fixedHolidays.contains(date);
    }
}
