package okio;

import java.util.concurrent.TimeUnit;

/* access modifiers changed from: package-private */
public final class PushableTimeout extends Timeout {
    private long originalDeadlineNanoTime;
    private boolean originalHasDeadline;
    private long originalTimeoutNanos;
    private Timeout pushed;

    PushableTimeout() {
    }

    /* access modifiers changed from: package-private */
    public void push(Timeout timeout) {
        this.pushed = timeout;
        boolean hasDeadline = timeout.hasDeadline();
        this.originalHasDeadline = hasDeadline;
        this.originalDeadlineNanoTime = hasDeadline ? timeout.deadlineNanoTime() : -1;
        long timeoutNanos = timeout.timeoutNanos();
        this.originalTimeoutNanos = timeoutNanos;
        timeout.timeout(minTimeout(timeoutNanos, timeoutNanos()), TimeUnit.NANOSECONDS);
        if (this.originalHasDeadline && hasDeadline()) {
            timeout.deadlineNanoTime(Math.min(deadlineNanoTime(), this.originalDeadlineNanoTime));
        } else if (hasDeadline()) {
            timeout.deadlineNanoTime(deadlineNanoTime());
        }
    }

    /* access modifiers changed from: package-private */
    public void pop() {
        this.pushed.timeout(this.originalTimeoutNanos, TimeUnit.NANOSECONDS);
        if (this.originalHasDeadline) {
            this.pushed.deadlineNanoTime(this.originalDeadlineNanoTime);
        } else {
            this.pushed.clearDeadline();
        }
    }
}
