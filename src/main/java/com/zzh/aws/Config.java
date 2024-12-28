package com.zzh.aws;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config {
    public static final String CLOUDFRONT_INSTANCE_ID = "CLOUDFRONT_INSTANCE_ID";
    public static final String CLOUDFRONT_ORIGIN_ID = "CLOUDFRONT_ORIGIN_ID";
    public static final String EC2_INSTANCE_ID = "EC2_INSTANCE_ID";
    public static final String EC2_INSTANCE_TYPES="EC2_INSTANCE_TYPES";
    public static final String RDS_INSTANCE_ID = "RDS_INSTANCE_ID";
    public static final String RDS_INSTANCE_TYPES="RDS_INSTANCE_TYPES";
    public static final String DEFAULT_INSTANCE_TYPE="DEFAULT_INSTANCE_TYPE";
    public static final String DEFAULT_RDS_INSTANCE_TYPE="DEFAULT_RDS_INSTANCE_TYPE";
    public static final String EC2_ALARM_NAME="EC2_ALARM_NAME";
    public static final String RDS_ALARM_NAME="RDS_ALARM_NAME";

    private static Properties properties = new Properties();

    static {
        try (InputStream input = Config.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                System.out.println("Sorry, unable to find application.properties");
            }
            properties.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static String getProperty(String propNm){
        return properties.getProperty(propNm);
    }
}

