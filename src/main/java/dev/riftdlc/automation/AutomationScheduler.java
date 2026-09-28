package dev.riftdlc.automation;

import java.util.Objects;

public final class AutomationScheduler {
    public enum State { IDLE, RUNNING, SUCCESS, FAILED, CANCELLED }
    public interface Task {
        /** Called on the client thread once per tick; return true when complete. */
        boolean tick();
        default void cancel() {}
    }
    private Task task;
    private State state = State.IDLE;
    private long deadline;
    public State state() { return state; }
    public boolean start(Task next, long currentTick, long timeoutTicks) {
        if (state == State.RUNNING || timeoutTicks < 1) return false;
        task = Objects.requireNonNull(next);
        deadline = currentTick + timeoutTicks;
        state = State.RUNNING;
        return true;
    }
    public void tick(long currentTick) {
        if (state != State.RUNNING) return;
        if (currentTick >= deadline) { cancel(); state = State.FAILED; return; }
        try { if (task.tick()) { state = State.SUCCESS; task = null; } }
        catch (RuntimeException e) { cancel(); state = State.FAILED; }
    }
    public void cancel() {
        if (task != null) task.cancel();
        task = null;
        state = State.CANCELLED;
    }
}
