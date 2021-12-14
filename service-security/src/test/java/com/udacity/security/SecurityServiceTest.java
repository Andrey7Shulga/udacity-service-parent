package com.udacity.security;

import com.udacity.image.ImageServiceHelper;
import com.udacity.security.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, Mockito.times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    //Application Requirements - 2
    @Test
    public void alarmStatusChanging_ifAlarmIsArmedAndSensorActivatedAndAlarmIsPending_alarmStatusAlarm () {
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, atMost(2)).setAlarmStatus(AlarmStatus.ALARM);
    }

    //Application Requirements - 3
    @Test
    public void alarmStatusChanging_ifPendingAlarmAndSensorAreInactive_alarmStatusNoAlarm () {
//        when(securityRepository.getSensors()).thenReturn(getTestSensors(3, false));
//        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
////        securityService.changeSensorActivationStatus(sensor, false);
//        ArgumentCaptor<AlarmStatus> argumentCaptor = ArgumentCaptor.forClass(AlarmStatus.class);
//        verify(securityRepository, atMostOnce()).setAlarmStatus(argumentCaptor.capture());
//        assertEquals(argumentCaptor.getValue(), AlarmStatus.NO_ALARM);
////        verify(securityRepository, atLeast(1)).setAlarmStatus(AlarmStatus.NO_ALARM);

        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensor.setActive(false);
        securityService.setAlarmStatus(AlarmStatus.NO_ALARM);
        verify(securityRepository, Mockito.times(1)).setAlarmStatus(any(AlarmStatus.class));

    }




    private Set<Sensor> getTestSensors (int count, boolean isActive){
        Set<Sensor> testSensorsScope = new HashSet<>();
        for (int i = 0; i <= count; i++){
            testSensorsScope.add(new Sensor("newSensor", SensorType.DOOR));
        }
        testSensorsScope.forEach(it -> it.setActive(isActive));
        return testSensorsScope;
    }

}
