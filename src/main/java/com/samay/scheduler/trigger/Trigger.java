package com.samay.scheduler.trigger;

public interface Trigger {
    /**
     * Checks if the trigger condition is met.
     * @return true if the job should be executed, false otherwise.
     */
    boolean isTriggered();

    /**
     * Called after the job has been executed, allowing the trigger to reset its state.
     */
    void postExecution();
}