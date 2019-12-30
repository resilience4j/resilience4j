package io.github.resilience4j.bulkhead.adaptive.internal.amid;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * @author romeh
 */
public class LogEntry implements Comparable<LogEntry> {

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LogEntry logEntry = (LogEntry) o;
        return Double.compare(logEntry.time, time) == 0 &&
            Double.compare(logEntry.concurrentCalls, concurrentCalls) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(time, concurrentCalls);
    }

    public double getTime() {
        return time;
    }

    public void setTime(double time) {
        this.time = time;
    }

    public double getConcurrentCalls() {
        return concurrentCalls;
    }

    public void setConcurrentCalls(double concurrentCalls) {
        this.concurrentCalls = concurrentCalls;
    }

    public LogEntry(double time, double concurrentCalls) {
        this.time = time;
        this.concurrentCalls = concurrentCalls;
    }

    double time;
    double concurrentCalls;

    @Override
    public int compareTo(
        @NotNull LogEntry o) {
        return (int) (this.time - o.time);
    }
}
