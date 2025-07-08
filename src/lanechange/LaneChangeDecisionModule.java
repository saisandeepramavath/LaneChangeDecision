package lanechange;

import heartbeat.IHeartbeatEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Main lane change decision module. Handles obstacle detection and decides
 * when it's safe to change lanes. Also sends heartbeat data to the monitor.
 */
public class LaneChangeDecisionModule implements ILaneChangeDecisionSystem, IVehicleDetectionSystem {

    private final IHeartbeatEmitter heartbeatEmitter;
    private volatile boolean running = false;
    private final Random random = new Random();
    private final ConcurrentHashMap<String, VehicleData> detectedVehicles = new ConcurrentHashMap<>();
    private final AtomicInteger processingCycles = new AtomicInteger(0);
    private final Object processingLock = new Object();
    
    // Failure simulation component
    private final FailureSimulator failureSimulator;

    /**
     * Creates a new lane change module with the given heartbeat emitter.
     */
    public LaneChangeDecisionModule(IHeartbeatEmitter heartbeatEmitter) {
        this.heartbeatEmitter = heartbeatEmitter;
        this.failureSimulator = new FailureSimulator();
    }

    @Override
    public void start() {
        if (running) {
            System.out.println("[LaneChangeModule] System already running.");
            return;
        }
        
        running = true;
        System.out.println("[LaneChangeModule] Starting obstacle detection module...");
        heartbeatEmitter.start();

        new Thread(() -> {
            try {
                while (running) {
                    // Send health status to heartbeat monitor
                    sendHealthStatus();
                    
                    // Perform main evaluation
                    evaluateLaneChange();
                    Thread.sleep(1000); // simulate processing interval
                }
            } catch (Exception e) {
                System.err.println("[LaneChangeModule] CRITICAL FAILURE: " + e.getMessage());
                e.printStackTrace();
                stop(); // Simulate module becoming unresponsive
            }
        }, "LaneChangeEvaluationThread").start();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private void sendHealthStatus() {
        try {
            double cpuUsage = getCpuUsage();
            double memoryUsage = getMemoryUsage();
            int errorCount = getErrorCount();
            
            heartbeatEmitter.sendHealthStatus("LaneChangeModule", cpuUsage, memoryUsage, errorCount);
        } catch (Exception e) {
            System.err.println("[LaneChangeModule] Error sending health status: " + e.getMessage());
        }
    }

    private double getCpuUsage() {
        // Simulate CPU usage calculation based on system load
        return Math.min(100.0, failureSimulator.getSystemLoad() * 100 + random.nextDouble() * 20);
    }

    private double getMemoryUsage() {
        // Simulate memory usage calculation
        double baseUsage = 45.0 + (processingCycles.get() * 0.01);
        double corruption = failureSimulator.getMemoryCorruption() > 0 ? (failureSimulator.getMemoryCorruption() * 0.5) : 0;
        return Math.min(100.0, baseUsage + corruption + random.nextDouble() * 10);
    }

    private int getErrorCount() {
        // Count various error conditions
        int errors = 0;
        if (failureSimulator.isSensorMalfunction()) errors += 2;
        if (failureSimulator.getMemoryCorruption() > 50) errors += 3;
        if (failureSimulator.getSystemLoad() > 0.8) errors += 1;
        return errors;
    }

    @Override
    public void stop() {
        running = false;
        heartbeatEmitter.stop();
        System.out.println("[LaneChangeModule] Stopped.");
    }

    @Override
    public void evaluateLaneChange() {
        System.out.println("[LaneChangeModule] Evaluating lane change...");

        // Simulate failure conditions
        failureSimulator.simulateFailures();
        
        // Add corrupt data for network interference simulation
        if (random.nextDouble() < (failureSimulator.getSystemLoad() * 0.03)) {
            int cycleCount = processingCycles.get();
            detectedVehicles.put("CORRUPT_DATA_" + cycleCount, 
                failureSimulator.createCorruptedVehicleData(cycleCount));
        }

        List<String> nearbyVehicles = null;
        try {
            nearbyVehicles = performObstacleDetection();
            updateVehicleTracking(nearbyVehicles);
            
            // Simulate memory corruption causing crashes
            if (failureSimulator.getMemoryCorruption() > 100) {
                // Force array access violation
                int[] corruptedArray = new int[1];
                corruptedArray[failureSimulator.getMemoryCorruption()] = 1; // Will cause IndexOutOfBoundsException
            }
            
            // Simulate processing with crash scenarios
            synchronized (processingLock) {
                if (nearbyVehicles != null) {
                    processLaneChangeDecision(nearbyVehicles);
                } else {
                    System.out.println("[LaneChangeModule] Sensor data unavailable. Holding lane.");
                }
            }
            
        } catch (Exception e) {
            System.err.println("[LaneChangeModule] CRITICAL PROCESSING ERROR: " + e.getMessage());
            // Simulate system becoming unresponsive due to cascading failures
            throw new RuntimeException("Critical obstacle detection failure", e);
        }
    }

    @Override
    public List<String> performObstacleDetection() {
        // Simulate sensor failures returning null
        if (failureSimulator.isSensorMalfunction() && random.nextDouble() < 0.3) {
            return null; // Sensor failure
        }
        
        // Simulate high system load causing processing delays and errors
        if (failureSimulator.shouldSimulateProcessingDelay()) {
            try {
                Thread.sleep(failureSimulator.getProcessingDelayMs()); // Simulate processing delays
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return null; // Timeout scenario
        }
        
        return getNearbyVehicles();
    }

    @Override
    public void updateVehicleTracking(List<String> vehicles) {
        if (vehicles == null) return;
        
        // Simulate vehicle tracking with potential corruption
        for (String vehicleId : vehicles) {
            double distance = random.nextDouble() * 100;
            double speed = random.nextDouble() * 60;
            double angle = random.nextDouble() * 360;
            
            // Simulate data corruption in critical scenarios
            if (failureSimulator.shouldSimulateDataCorruption()) {
                distance = Double.NaN; // Corrupted distance reading
                speed = Double.NEGATIVE_INFINITY; // Corrupted speed reading
            }
            
            detectedVehicles.put(vehicleId, new VehicleData(vehicleId, distance, speed, angle));
        }
    }

    @Override
    public VehicleData getVehicleData(String vehicleId) {
        return detectedVehicles.get(vehicleId);
    }

    @Override
    public List<VehicleData> getAllTrackedVehicles() {
        return new ArrayList<>(detectedVehicles.values());
    }

    private void processLaneChangeDecision(List<String> nearbyVehicles) {
        int count = nearbyVehicles.size();
        
        // Simulate decision making with potential failures
        if (count > 0) {
            System.out.println("[LaneChangeModule] Analyzing " + count + " nearby vehicles...");
            
            // Check for corrupted vehicle data that could cause crashes
            for (String vehicleId : nearbyVehicles) {
                VehicleData data = detectedVehicles.get(vehicleId);
                if (data != null) {
                    // Simulate arithmetic operations that could fail with corrupted data
                    double safetyDistance = calculateSafetyDistance(data);
                    if (Double.isNaN(safetyDistance) || Double.isInfinite(safetyDistance)) {
                        throw new ArithmeticException("Invalid safety calculation for vehicle: " + data.getId());
                    }
                    System.out.println("[LaneChangeModule] Vehicle " + data.getId() + " at distance " + data.getDistance() + 
                                     " with angle " + data.getAngle() + " degrees");
                }
            }
            
            System.out.println("[LaneChangeModule] Lane change evaluation complete.");
        } else {
            System.out.println("[LaneChangeModule] No nearby vehicles. Holding lane.");
        }
    }

    private double calculateSafetyDistance(VehicleData data) {
        // Safety distance calculation that can fail with corrupted data
        if (data.getSpeed() < 0 || Double.isNaN(data.getDistance())) {
            return Double.NaN;
        }
        return data.getDistance() / (data.getSpeed() + 0.1); // Division by near-zero can cause issues
    }

    private List<String> getNearbyVehicles() {
        List<String> vehicles = new ArrayList<>();
        
        // Simulate vehicle detection
        int vehicleCount = random.nextInt(5);
        for (int i = 0; i < vehicleCount; i++) {
            vehicles.add("VEHICLE_" + (System.currentTimeMillis() + i));
        }
        
        return vehicles;
    }
}