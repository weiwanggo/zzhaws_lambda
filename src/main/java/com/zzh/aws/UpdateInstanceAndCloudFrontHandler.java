package com.zzh.aws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import java.util.List;

public class UpdateInstanceAndCloudFrontHandler implements RequestHandler<InstanceUpdateRequest, String> {
    public final List<String> INSTANCE_TYPES = List.of(System.getenv(Config.EC2_INSTANCE_TYPES).split(","));
    public final String DEFAULT_INSTANCE_TYPE= System.getenv(Config.DEFAULT_INSTANCE_TYPE);

    public static final String EC2_ALARM_NAME = System.getenv(Config.EC2_ALARM_NAME);

    private static final Logger logger = LoggerFactory.getLogger(UpdateInstanceAndCloudFrontHandler.class);

    @Override
    public String handleRequest(InstanceUpdateRequest request, Context context) {
        String newInstanceType = request.getNewInstanceType();
        String currentInstanceType = Ec2Helper.getCurrentInstanceType();

        if (newInstanceType == null){
            newInstanceType = InstanceUpdateRequest.UPGRADE_TYPE;
        }

        if (InstanceUpdateRequest.UPGRADE_TYPE.equalsIgnoreCase(newInstanceType)) {
            newInstanceType = InstanceTypeUtils.upgradeInstanceType(currentInstanceType, INSTANCE_TYPES);
        } else if (InstanceUpdateRequest.DOWNGRADE_TYPE.equalsIgnoreCase(newInstanceType)) {
            newInstanceType = InstanceTypeUtils.downgradeInstanceType(currentInstanceType, INSTANCE_TYPES);
        }

        logger.info("Current instance type: {}, Update to new instance type : {}", currentInstanceType, newInstanceType);

        if (newInstanceType == null || !INSTANCE_TYPES.contains(newInstanceType)){
            logger.error("Invalid new instance type: {}", newInstanceType);
            return "Invalid new instance type: " + newInstanceType;
        }
        else if (newInstanceType.equalsIgnoreCase(currentInstanceType)){
            return "New instance type is the same as current instance type,  nothing to do!";
        }

        CloudwatchHelper.enableDisableAlarm(EC2_ALARM_NAME,  false);
        logger.info("Alarm {} is disabled", EC2_ALARM_NAME);
        String currentType =  Ec2Helper.changeEc2InstanceType(newInstanceType, context);
        CloudwatchHelper.enableDisableAlarm(EC2_ALARM_NAME, true);
        logger.info("Alarm {} is enabled", EC2_ALARM_NAME);

        return currentType;
    }



}

