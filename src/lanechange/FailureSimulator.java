package lanechange;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simulates various things going wrong with the system.
 * Makes sensors fail, memory get corrupted, etc.
 */
public class FailureSimulator {
    private final Random random = new Random();
    private final AtomicInteger processingCycles = new AtomicInteger(0);
    
    private volatile boolean sensorMalfunction = true;
    private volatile int memoryCorruption = 0;
    private volatile double systemLoad = 0.0;
    
    /**
     * Runs the failure simulation. Makes things progressively worse over time.
     */
    public void simulateFailures() {
        int cycleCount = processingCycles.incrementAndGet();
        
        // Simulate increasing system load causing failures
        systemLoad += random.nextDouble() * 0.05;
        if (systemLoad > 1.0) systemLoad = 1.0;
        
        // Random sensor malfunctions (increases over time)
        if (random.nextDouble() < (0.01 + cycleCount * 0.0001)) {
            sensorMalfunction = true;
            System.out.println("[FailureSimulator] WARNING: Sensor malfunction detected!");
        }
        
        // Memory corruption simulation (gradual corruption)
        if (random.nextDouble() < (systemLoad * 0.02)) {
            memoryCorruption += random.nextInt(10);
            if (memoryCorruption > 50) {
                System.out.println("[FailureSimulator] WARNING: Memory corruption level: " + memoryCorruption);
            }
        }
    }
    
    /**
     * Checks if sensor malfunction is currently active.
     * 
     * @return true if sensor malfunction is detected, false otherwise
     */
    public boolean isSensorMalfunction() {
        return sensorMalfunction;
    }
    
    /**
     * Gets the current memory corruption level.
     * 
     * @return the memory corruption level (0-100+)
     */
    public int getMemoryCorruption() {
        return memoryCorruption;
    }
    
    /**
     * Gets the current system load.
     * 
     * @return the system load (0.0-1.0)
     */
    public double getSystemLoad() {
        return systemLoad;
    }
    
    /**
     * Determines if data corruption should occur based on current system state.
     * 
     * @return true if data corruption should be simulated, false otherwise
     */
    public boolean shouldSimulateDataCorruption() {
        return memoryCorruption > 75 && random.nextDouble() < 0.1;
    }
    
    /**
     * Determines if processing delays should occur based on system load.
     * 
     * @return true if processing delays should be simulated, false otherwise
     */
    public boolean shouldSimulateProcessingDelay() {
        return systemLoad > 0.8 && random.nextDouble() < 0.2;
    }
    
    /**
     * Gets a random processing delay time in milliseconds.
     * 
     * @return delay time in milliseconds
     */
    public int getProcessingDelayMs() {
        return random.nextInt(5000);
    }
    
    /**
     * Creates corrupted vehicle data for testing error handling.
     * 
     * @param cycleCount the current processing cycle count
     * @return corrupted VehicleData instance
     */
    public VehicleData createCorruptedVehicleData(int cycleCount) {
        return new VehicleData("CORRUPT_DATA_" + cycleCount, -999.0, -999.0, Double.NaN);
    }
    
    /**
     * Resets the failure simulator to initial state.
     */
    public void reset() {
        sensorMalfunction = false;
        memoryCorruption = 0;
        systemLoad = 0.0;
        processingCycles.set(0);
    }
}
