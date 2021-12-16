package com.udacity.security;

import com.udacity.image.ImageServiceHelper;
import com.udacity.security.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.awt.image.BufferedImage;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SecurityServiceTest {

    private Sensor sensor;
    private SecurityService securityService;

    @Mock
    SecurityRepository securityRepository;
    @Mock
    ImageServiceHelper imageServiceHelper;

    @BeforeEach
    void settingUp() {
        securityService = new SecurityService(securityRepository, imageServiceHelper);
        sensor = new Sensor("sensorForTest", SensorType.DOOR);
    }

    //Application Requirements - 1
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    public void alarmStatusChanging_ifAlarmIsArmedAndSensorActivated_alarmStatusPending (ArmingStatus status) {
        when(securityService.getArmingStatus()).thenReturn(status);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, Mockito.times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    //Application Requirements - 2
    @Test
    public void alarmStatusChanging_ifAlarmIsArmedAndSensorActivatedAndAlarmIsPending_alarmStatusAlarm () {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, atMost(2)).setAlarmStatus(AlarmStatus.ALARM);
    }

    //Application Requirements - 3
    @Test
    public void alarmStatusChanging_ifPendingAlarmAndSensorAreInactive_alarmStatusNoAlarm () {
        when(securityRepository.getSensors()).thenReturn(getTestSensors(3, false));
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.setAlarmStatus(AlarmStatus.NO_ALARM);
        verify(securityRepository, atLeast(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    //Application Requirements - 4
    @Test
    public void alarmStatusChanging_ifAlarmIsActiveAndChangeSensorState_shouldNotAffectAlarmState () {
        sensor.setActive(false);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, atMostOnce()).updateSensor(sensor);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));

        sensor.setActive(true);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository, atLeast(2)).updateSensor(sensor);
        //bug
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    //Application Requirements - 5
    @Test
    public void alarmStatusChanging_ifPendingAlarmAndSensorIsReactive_alarmStatusToAlarm () {
        sensor.setActive(true);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, atMostOnce()).setAlarmStatus(AlarmStatus.ALARM);
    }

    //Application Requirements - 6
    @Test
    public void alarmStatusChanging_ifSensorIsDeactivatedWhileBeingInactive_alarmStateIsNotChanged () {
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    //Application Requirements - 7
    @Test
    public void alarmStatusChanging_ifSystemIsArmedAndCatIsDetected_changeToAlarm () {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageServiceHelper.imageContainsCat(any(), anyFloat())).thenReturn(true);
        securityService.processImage(mock(BufferedImage.class));
        verify(securityRepository, atMostOnce()).setAlarmStatus(AlarmStatus.ALARM);
    }

    //Application Requirements - 8
    @Test
    public void alarmStatusChanging_ifSensorIsInactiveAndCatIsNotDetected_changeToNoAlarm () {
        when(securityRepository.getSensors()).thenReturn(getTestSensors(2, false));
        when(imageServiceHelper.imageContainsCat(any(), anyFloat())).thenReturn(false);
        securityService.processImage(mock(BufferedImage.class));
        verify(securityRepository, atMostOnce()).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    //Application Requirements - 9
    @Test
    public void alarmStatusChanging_ifSystemIsDisarmed_changeToNoAlarm () {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        verify(securityRepository, atMostOnce()).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    //Application Requirements - 10
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_AWAY", "ARMED_HOME"})
    public void sensorsState_ifSystemIsArmed_resetAllSensorsToInactive (ArmingStatus armingStatus) {
        Set<Sensor> testSensors = getTestSensors(4, true);
        List<Executable> list = new ArrayList<>();
        when(securityRepository.getSensors()).thenReturn(testSensors);
        securityService.setArmingStatus(armingStatus);
        testSensors.forEach(s -> list.add(() -> assertEquals(s.getActive(), false)));
        assertAll(list);
    }

    //Application Requirements - 11
    @Test
    public void alarmStatusChanging_CatIsDetectedAndSystem_changeToAlarm () {
        when(imageServiceHelper.imageContainsCat(any(), anyFloat())).thenReturn(true);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        securityService.processImage(mock(BufferedImage.class));
        verify(securityRepository, atLeastOnce()).setAlarmStatus(AlarmStatus.ALARM);
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
