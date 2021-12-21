package com.udacity.security.service;

import com.udacity.image.service.ImageServiceHelper;
import com.udacity.security.application.StatusListener;
import com.udacity.security.data.AlarmStatus;
import com.udacity.security.data.ArmingStatus;
import com.udacity.security.data.SecurityRepository;
import com.udacity.security.data.Sensor;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Service that receives information about changes to the security system. Responsible for
 * forwarding updates to the repository and making any decisions about changing the system state.
 *
 * This is the class that should contain most of the business logic for our system, and it is the
 * class you will be writing unit tests for.
 */
public class SecurityService {

    private ImageServiceHelper imageService;
    private SecurityRepository securityRepository;
    private Set<StatusListener> statusListeners = new HashSet<>();
    private boolean isCatDetected = false;

    public SecurityService(SecurityRepository securityRepository, ImageServiceHelper imageService) {
        this.securityRepository = securityRepository;
        this.imageService = imageService;
    }

    /**
     * Sets the current arming status for the system. Changing the arming status
     * may update both the alarm status.
     * @param armingStatus
     */
    public void setArmingStatus(ArmingStatus armingStatus) {
        if(armingStatus == ArmingStatus.ARMED_HOME && isCatDetected){
            setAlarmStatus(AlarmStatus.ALARM);
        }
        if(armingStatus == ArmingStatus.DISARMED) {
            setAlarmStatus(AlarmStatus.NO_ALARM);
        } else {
                changeSensorStatus(getSensors(), false);
        }
        securityRepository.setArmingStatus(armingStatus);
        statusListeners.forEach(StatusListener::sensorStatusChanged);
    }

    private void changeSensorStatus(Set<Sensor> sensors, boolean active) {
        sensors.forEach(sensor ->
                System.out.printf("Before changing Sensor(#ID-%s) is active - %s%n",
                        sensor.getSensorId(), sensor.getActive()));

        System.out.println("\nChanging sensors and alarm statuses...\n");
        sensors.parallelStream().forEachOrdered(sensor -> changeSensorActivationStatus(sensor, active));

        sensors.forEach(sensor ->
                System.out.printf("After changing Sensor(#ID-%s) is active - %s%n",
                        sensor.getSensorId(), sensor.getActive()));
    }

    /**
     * Internal method that handles alarm status changes based on whether
     * the camera currently shows a cat.
     * @param cat True if a cat is detected, otherwise false.
     */
    private void catDetected(Boolean cat) {
        isCatDetected = cat;
        if(getArmingStatus() == ArmingStatus.ARMED_HOME && cat) {
            setAlarmStatus(AlarmStatus.ALARM);
        } else if(isAllTheSensorsHaveTheSameState(false) && !cat) {
            setAlarmStatus(AlarmStatus.NO_ALARM);
        }
        statusListeners.forEach(sl -> sl.catDetected(cat));
    }

    private boolean isAllTheSensorsHaveTheSameState (boolean sensorState) {
        return getSensors().stream().allMatch(x -> x.getActive() == sensorState);
    }


    /**
     * Register the StatusListener for alarm system updates from within the SecurityService.
     * @param statusListener
     */
    public void addStatusListener(StatusListener statusListener) {statusListeners.add(statusListener);}
    public void removeStatusListener(StatusListener statusListener) {statusListeners.remove(statusListener);}

    /**
     * Change the alarm status of the system and notify all listeners.
     * @param status
     */
    public void setAlarmStatus(AlarmStatus status) {
        securityRepository.setAlarmStatus(status);
        statusListeners.forEach (sl -> sl.notify(status));
    }

    /**
     * Internal method for updating the alarm status when a sensor has been activated.
     */
    private void handleSensorActivated() {
        if(securityRepository.getArmingStatus() == ArmingStatus.DISARMED) {
            return; //no problem if the system is disarmed
        }
        switch(securityRepository.getAlarmStatus()) {
            case NO_ALARM -> setAlarmStatus(AlarmStatus.PENDING_ALARM);
            case PENDING_ALARM -> setAlarmStatus(AlarmStatus.ALARM);
        }
    }

    /**
     * Internal method for updating the alarm status when a sensor has been deactivated
     */
    private void handleSensorDeactivated() {
        switch(securityRepository.getAlarmStatus()) {
            case PENDING_ALARM -> setAlarmStatus(AlarmStatus.NO_ALARM);
            case ALARM -> setAlarmStatus(AlarmStatus.PENDING_ALARM);
        }
    }

    /**
     * Change the activation status for the specified sensor and update alarm status if necessary.
     * @param sensor
     * @param active
     */
    public void changeSensorActivationStatus(Sensor sensor, Boolean active) {
        AlarmStatus alarm = securityRepository.getAlarmStatus();

        if (alarm != AlarmStatus.ALARM) {
            if (active) {
                handleSensorActivated();
            } else if (sensor.getActive()) {
                handleSensorDeactivated();
            }
        }
        sensor.setActive(active);
        securityRepository.updateSensor(sensor);
    }

    /**
     * Change the activation status for the specified sensor when no activate status is passed and update alarm status if necessary.
     * @param sensor
     */
    public void deactivateSensor(Sensor sensor) {
        AlarmStatus alarm = this.getAlarmStatus();
        ArmingStatus arming = this.getArmingStatus();

        if (!sensor.getActive() && alarm == AlarmStatus.PENDING_ALARM) {
            handleSensorDeactivated();
        } else if (arming == ArmingStatus.DISARMED && alarm == AlarmStatus.ALARM) {
            handleSensorDeactivated();
        }
        securityRepository.updateSensor(sensor);
    }

    /**
     * Send an image to the SecurityService for processing. The securityService will use its provided
     * ImageService to analyze the image for cats and update the alarm status accordingly.
     * @param currentCameraImage
     */
    public void processImage(BufferedImage currentCameraImage) {
        catDetected(imageService.imageContainsCat(currentCameraImage, 50.0f));
    }

    public AlarmStatus getAlarmStatus() {
        return securityRepository.getAlarmStatus();
    }

    public Set<Sensor> getSensors() {
        return securityRepository.getSensors();
    }

    public void addSensor(Sensor sensor) {
        securityRepository.addSensor(sensor);
    }

    public void removeSensor(Sensor sensor) {
        securityRepository.removeSensor(sensor);
    }

    public ArmingStatus getArmingStatus() {
        return securityRepository.getArmingStatus();
    }
}
