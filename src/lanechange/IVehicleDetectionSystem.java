package lanechange;

import java.util.List;

/**
 * Interface for vehicle detection systems.
 * Defines the contract for detecting and tracking nearby vehicles.
 */
public interface IVehicleDetectionSystem {
    
    /**
     * Performs obstacle detection and returns a list of nearby vehicle IDs.
     * 
     * @return a list of vehicle identifiers, or null if detection fails
     */
    List<String> performObstacleDetection();
    
    /**
     * Updates the tracking information for detected vehicles.
     * 
     * @param vehicleIds the list of vehicle IDs to track
     */
    void updateVehicleTracking(List<String> vehicleIds);
    
    /**
     * Retrieves the current data for a specific vehicle.
     * 
     * @param vehicleId the ID of the vehicle to retrieve
     * @return the vehicle data, or null if not found
     */
    VehicleData getVehicleData(String vehicleId);
    
    /**
     * Gets all currently tracked vehicles.
     * 
     * @return a list of all tracked vehicle data
     */
    List<VehicleData> getAllTrackedVehicles();
}
