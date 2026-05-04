package com.studyflow.services;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class SmartPlanService {
    private static final AtomicInteger BID_ID = new AtomicInteger(1);
    private static final List<StudentBidView> BIDS = new ArrayList<>();

    public SmartPlanResult buildSmartPlan(int userId, LocalDate startDate, LocalDate endDate) {
        return new SmartPlanResult(List.of());
    }

    public SmartPlanResult buildSmartPlan(int userId, LocalDate anchorDate) {
        return new SmartPlanResult(List.of());
    }

    public boolean saveStudentBid(int userId, String type, String title, String description,
                                  LocalDate periodStart, LocalDate periodEnd, LocalDate date,
                                  LocalTime startTime, LocalTime endTime) {
        synchronized (BIDS) {
            BIDS.add(new StudentBidView(
                    BID_ID.getAndIncrement(), userId, type, title, description, date, startTime, endTime,
                    type != null && type.toLowerCase().contains("day off")
            ));
        }
        return true;
    }

    public List<StudentBidView> loadStudentBidsForPeriod(int userId, LocalDate periodStart, LocalDate periodEnd) {
        synchronized (BIDS) {
            return BIDS.stream()
                    .filter(b -> b.userId() == userId)
                    .filter(b -> b.date() != null && (periodStart == null || !b.date().isBefore(periodStart)))
                    .filter(b -> b.date() != null && (periodEnd == null || !b.date().isAfter(periodEnd)))
                    .toList();
        }
    }

    public boolean deleteStudentBid(int userId, int bidId) {
        synchronized (BIDS) {
            return BIDS.removeIf(b -> b.userId() == userId && b.id() == bidId);
        }
    }

    public record SmartPlanResult(List<SmartPlanTask> tasks) {}

    public record SmartPlanTask(
            String title, String sessionType, LocalDate date, boolean dayOff, String reason,
            LocalTime preferredStart, LocalTime preferredEnd, boolean mandatory
    ) {}

    public record StudentBidView(
            int id, int userId, String type, String title, String description, LocalDate date,
            LocalTime startTime, LocalTime endTime, boolean dayOff
    ) {}
}

