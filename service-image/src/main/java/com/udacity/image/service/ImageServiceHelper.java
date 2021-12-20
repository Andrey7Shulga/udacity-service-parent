package com.udacity.image.service;

import java.awt.image.BufferedImage;

public interface ImageServiceHelper {
    boolean imageContainsCat(BufferedImage image, float confidenceThreshhold);
}
