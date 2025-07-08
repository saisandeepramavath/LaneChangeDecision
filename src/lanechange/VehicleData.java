package lanechange;

/**
 * Represents vehicle tracking data with position, speed, and identification information.
 * This class is immutable to ensure thread safety and data integrity.
 */
public final class VehicleData {
    private final String id;
    private final double distance;
    private final double speed;
    private final double angle;
    
    /**
     * Creates a new VehicleData instance.
     * 
     * @param id the unique identifier for the vehicle
     * @param distance the distance to the vehicle in meters
     * @param speed the vehicle's speed in km/h
     * @param angle the vehicle's relative angle in degrees
     */
    public VehicleData(String id, double distance, double speed, double angle) {
        this.id = id;
        this.distance = distance;
        this.speed = speed;
        this.angle = angle;
    }
    
    /**
     * Gets the vehicle's unique identifier.
     * 
     * @return the vehicle ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Gets the distance to the vehicle.
     * 
     * @return the distance in meters
     */
    public double getDistance() {
        return distance;
    }
    
    /**
     * Gets the vehicle's speed.
     * 
     * @return the speed in km/h
     */
    public double getSpeed() {
        return speed;
    }
    
    /**
     * Gets the vehicle's relative angle.
     * 
     * @return the angle in degrees
     */
    public double getAngle() {
        return angle;
    }
    
    /**
     * Checks if this vehicle data contains valid readings.
     * 
     * @return true if all data is valid, false otherwise
     */
    public boolean isValid() {
        return !Double.isNaN(distance) && !Double.isNaN(speed) && !Double.isNaN(angle)
                && !Double.isInfinite(distance) && !Double.isInfinite(speed) && !Double.isInfinite(angle);
    }
    
    @Override
    public String toString() {
        return String.format("VehicleData{id='%s', distance=%.2f, speed=%.2f, angle=%.2f}", 
                id, distance, speed, angle);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        VehicleData that = (VehicleData) obj;
        return Double.compare(that.distance, distance) == 0 &&
               Double.compare(that.speed, speed) == 0 &&
               Double.compare(that.angle, angle) == 0 &&
               id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, distance, speed, angle);
    }
}
