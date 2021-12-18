package com.udacity.image;

import java.awt.image.BufferedImage;

public interface ImageServiceHelper {
    public boolean imageContainsCat(BufferedImage image, float confidenceThreshhold);
}
