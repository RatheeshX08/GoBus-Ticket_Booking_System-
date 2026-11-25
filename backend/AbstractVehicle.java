package com.busbooking.model;

/**
 * AbstractVehicle base class demonstrating abstraction, inheritance,
 * and Object class method overriding (toString, equals).
 */
public abstract class AbstractVehicle {
    protected String vehicleNumber;
    protected String operatorName;
    protected int totalCapacity;

    public AbstractVehicle() {
    }

    public AbstractVehicle(String vehicleNumber, String operatorName, int totalCapacity) {
        this.vehicleNumber = vehicleNumber;
        this.operatorName = operatorName;
        this.totalCapacity = totalCapacity;
    }

    public abstract String getVehicleCategory();

    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public void setVehicleNumber(String vehicleNumber) {
        this.vehicleNumber = vehicleNumber;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public int getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(int totalCapacity) {
        this.totalCapacity = totalCapacity;
    }

    @Override
    public String toString() {
        return "Vehicle [Reg=" + vehicleNumber + ", Operator=" + operatorName + ", Capacity=" + totalCapacity + "]";
    }
}
