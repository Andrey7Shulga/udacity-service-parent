package com.udacity.security;

import com.udacity.image.ImageServiceHelper;
import com.udacity.security.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.UUID;

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
        sensor = new Sensor(UUID.randomUUID().toString(), SensorType.DOOR);
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
}
