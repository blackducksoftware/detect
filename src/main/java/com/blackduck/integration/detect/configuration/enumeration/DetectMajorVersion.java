package com.blackduck.integration.detect.configuration.enumeration;

import com.blackduck.integration.configuration.util.ProductMajorVersion;

public class DetectMajorVersion extends ProductMajorVersion {
    public static final DetectMajorVersion ONE = new DetectMajorVersion(1);
    public static final DetectMajorVersion TWO = new DetectMajorVersion(2);
    public static final DetectMajorVersion THREE = new DetectMajorVersion(3);
    public static final DetectMajorVersion FOUR = new DetectMajorVersion(4);
    public static final DetectMajorVersion FIVE = new DetectMajorVersion(5);
    public static final DetectMajorVersion SIX = new DetectMajorVersion(6);
    public static final DetectMajorVersion SEVEN = new DetectMajorVersion(7);
    public static final DetectMajorVersion EIGHT = new DetectMajorVersion(8);
    public static final DetectMajorVersion NINE = new DetectMajorVersion(9);
    public static final DetectMajorVersion TEN = new DetectMajorVersion(10);
    public static final DetectMajorVersion ELEVEN = new DetectMajorVersion(11);

    public DetectMajorVersion(Integer intValue) {
        super(intValue);
    }
}
