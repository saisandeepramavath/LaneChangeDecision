package lanechange;

/**
 * Interface for lane change decision making systems.
 * Defines the contract for evaluating lane change maneuvers in autonomous vehicles.
 */
public interface ILaneChangeDecisionSystem {
    
    /**
     * Starts the lane change decision system.
     * Initializes monitoring and begins evaluation cycles.
     */
    void start();
    
    /**
     * Stops the lane change decision system.
     * Terminates all monitoring and evaluation processes.
     */
    void stop();
    
    /**
     * Evaluates whether a lane change is safe and appropriate.
     * This method performs obstacle detection, analyzes traffic conditions,
     * and determines the feasibility of a lane change maneuver.
     */
    void evaluateLaneChange();
    
    /**
     * Checks if the decision system is currently running.
     * 
     * @return true if the system is active, false otherwise
     */
    boolean isRunning();
}
