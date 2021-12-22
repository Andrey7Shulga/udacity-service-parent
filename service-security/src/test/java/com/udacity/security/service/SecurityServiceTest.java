package com.udacity.security.service;

import com.udacity.image.service.FakeImageService;
import com.udacity.security.data.*;
import com.udacity.security.service.SecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {

    private Sensor sensor;
    private SecurityService securityService;

    @Mock
    private SecurityRepository securityRepository;
    @Mock
    private FakeImageService imageServiceHelper;

    @BeforeEach
    void settingUp() {
        securityService = new SecurityService(securityRepository, imageServiceHelper);
        sensor = new Sensor("sensorForTest", SensorType.DOOR);
    }

    @ParameterizedTest
    @DisplayName("Application Requirements - 1")
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    public void alarmStatusChanging_ifAlarmIsArmedAndSensorActivated_alarmStatusPending (ArmingStatus status) {
        when(securityService.getArmingStatus()).thenReturn(status);
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, Mockito.times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    @ParameterizedTest
    @DisplayName("Application Requirements - 2")
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    public void alarmStatusChanging_ifAlarmIsArmedAndSensorActivatedAndAlarmIsPending_alarmStatusAlarm (ArmingStatus status) {
        when(securityRepository.getArmingStatus()).thenReturn(status);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @ParameterizedTest
    @DisplayName("Application Requirements - 3")
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    public void alarmStatusChanging_ifPendingAlarmAndSensorAreInactive_alarmStatusNoAlarm (ArmingStatus status) {
        when(securityRepository.getArmingStatus()).thenReturn(status);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensor.setActive(false);
        securityService.deactivateSensor(sensor);
        verify(securityRepository, atLeastOnce()).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("Application Requirements - 4")
    public void alarmStatusChanging_ifAlarmIsActiveAndChangeSensorState_shouldNotAffectAlarmState (boolean b) {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensor, b);
        verify(securityRepository, atMostOnce()).updateSensor(sensor);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    @DisplayName("Application Requirements - 5")
    public void alarmStatusChanging_ifPendingAlarmAndSensorIsReactivated_alarmStatusToAlarm (ArmingStatus status) {
        sensor.setActive(true);
        when(securityRepository.getArmingStatus()).thenReturn(status);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, atMostOnce()).setAlarmStatus(AlarmStatus.ALARM);
    }

    @ParameterizedTest
    @EnumSource(value = AlarmStatus.class, names = {"ALARM", "PENDING_ALARM", "NO_ALARM"})
    @DisplayName("Application Requirements - 6")
    public void alarmStatusChanging_ifSensorIsDeactivatedWhileBeingInactive_alarmStateIsNotChanged (AlarmStatus status) {
        when(securityRepository.getAlarmStatus()).thenReturn(status);
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    @Test
    @DisplayName("Application Requirements - 7")
    public void alarmStatusChanging_ifSystemIsArmedHomeAndCatIsDetected_changeToAlarm () {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageServiceHelper.imageContainsCat(any(), anyFloat())).thenReturn(true);
        securityService.processImage(mock(BufferedImage.class));
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    @DisplayName("Application Requirements - 8")
    public void alarmStatusChanging_ifSensorIsInactiveAndCatIsNotDetected_changeToNoAlarm () {
        securityService.changeSensorActivationStatus(sensor, false);
        when(imageServiceHelper.imageContainsCat(any(), anyFloat())).thenReturn(false);
        securityService.processImage(mock(BufferedImage.class));
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    @DisplayName("Application Requirements - 9")
    public void alarmStatusChanging_ifSystemIsDisarmed_changeToNoAlarm () {
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        verify(securityRepository, atLeastOnce()).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @ParameterizedTest
    @DisplayName("Application Requirements - 10")
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_AWAY", "ARMED_HOME"})
    public void sensorsState_ifSystemIsArmed_resetAllSensorsToInactive (ArmingStatus armingStatus) {
        Set<Sensor> testSensors = getTestSensors(4, true);
        List<Executable> list = new ArrayList<>();
        when(securityRepository.getSensors()).thenReturn(testSensors);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.setArmingStatus(armingStatus);
        securityService.getSensors().forEach(s -> list.add(() -> assertEquals(s.getActive(), false)));
        assertAll(list);
    }

    @Test
    @DisplayName("Application Requirements - 11")
    public void alarmStatusChanging_CatIsDetectedAndSystemArmedHome_changeToAlarm () {
        when(imageServiceHelper.imageContainsCat(any(), anyFloat())).thenReturn(true);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        securityService.processImage(mock(BufferedImage.class));
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        verify(securityRepository, atLeastOnce()).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    @DisplayName("Additional test - 01")
    void alarmStatusChanging_ifSystemDisarmedAndAlarmState_changeToPending() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        securityService.deactivateSensor(sensor);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }


    private Set<Sensor> getTestSensors (int count, boolean isActive){
        Set<Sensor> testSensorsScope = new HashSet<>();
        for (int i = 0; i <= count; i++){
            testSensorsScope.add(new Sensor("newSensor", SensorType.DOOR));
        }
        testSensorsScope.forEach(s -> s.setActive(isActive));
        return testSensorsScope;
    }

}
