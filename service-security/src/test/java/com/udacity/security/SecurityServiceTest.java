package com.udacity.security;

import com.udacity.image.ImageServiceHelper;
import com.udacity.security.data.SecurityRepository;
import com.udacity.security.data.Sensor;
import com.udacity.security.data.SensorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {

    private Sensor sensor;
    private SecurityService securityService;
    private final String randomUID = UUID.randomUUID().toString();

    @Mock
    SecurityRepository securityRepository;
    @Mock
    ImageServiceHelper imageServiceHelper;

    @BeforeEach
    void settingUp() {
        securityService = new SecurityService(securityRepository, imageServiceHelper);
        sensor = new Sensor(randomUID, SensorType.DOOR);
    }





    @Test
    public void shouldAnswerWithTrue()
    {
        assertTrue( true );
    }
}
