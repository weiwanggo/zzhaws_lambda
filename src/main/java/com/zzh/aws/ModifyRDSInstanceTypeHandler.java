package com.zzh.aws;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

public class ModifyRDSInstanceTypeHandler implements RequestHandler<InstanceUpdateRequest, String> {
    public final List<String> RDS_INSTANCE_TYPES = List.of(System.getenv(Config.RDS_INSTANCE_TYPES).split(","));
    public final String DEFAULT_RDS_INSTANCE_TYPE= System.getenv(Config.DEFAULT_RDS_INSTANCE_TYPE);

    private static final Logger logger = LoggerFactory.getLogger(ModifyRDSInstanceTypeHandler.class);

    @Override
    public String handleRequest(InstanceUpdateRequest input, Context context) {
        String newInstanceType = input.getNewInstanceType(); // Desired instance type, e.g., "db.m5.large"
        if (newInstanceType == null){
            newInstanceType = "upgrade";
        }

        String currentInstanceType = RdsHelper.getCurrentRDSType();
        logger.info("Retrieved current RDS instance Type: {}", currentInstanceType);

        if ("upgrade".equalsIgnoreCase(newInstanceType)) {
            newInstanceType = InstanceTypeUtils.upgradeInstanceType(currentInstanceType, RDS_INSTANCE_TYPES);
        } else if ("downgrade".equalsIgnoreCase(newInstanceType)) {
            newInstanceType = InstanceTypeUtils.downgradeInstanceType(currentInstanceType, RDS_INSTANCE_TYPES);
        } else if ("reset".equalsIgnoreCase(newInstanceType)) {
           logger.info("reset to default RDS Type");
           return resetDefaultRDSType(currentInstanceType, context);
        }

        logger.info("Current instance type: {}, Update to new instance type : {}", currentInstanceType, newInstanceType);

        if (newInstanceType == null || !RDS_INSTANCE_TYPES.contains(newInstanceType)){
            logger.error("Invalid new instance type: {}", newInstanceType);
            return "Invalid new instance type: " + newInstanceType;
        }
        else if (newInstanceType.equalsIgnoreCase(currentInstanceType)){
            return "New instance type is the same as current instance type,  nothing to do!";
        }

        logger.info("Start update RDS type to {}", newInstanceType);
        return RdsHelper.changeRdsInstanceType(newInstanceType, context);
    }

    private String resetDefaultRDSType(String currentInstanceType, Context context){
        if (RdsHelper.DEFAULT_RDS_TYPE == null || RdsHelper.DEFAULT_RDS_TYPE.isEmpty()){
            logger.error("Unknown default rds type,  unable to reset");
            return "Unknown default rds type,  unable to reset";
        }

        // check for current type
        if (RdsHelper.DEFAULT_RDS_TYPE.equalsIgnoreCase(currentInstanceType)){
            logger.info("Already on default RDS type: {}", RdsHelper.DEFAULT_RDS_TYPE);
            return "Already on default RDS type: " + RdsHelper.DEFAULT_RDS_TYPE;
        }

        // check for db metrics
        double cpuUtilization = CloudwatchHelper.getCpuUtilization();
        double databaseConnections = CloudwatchHelper.getDatabaseConnections();
        logger.info("Current average cpuUnitilization : {},  DB connections: {}", cpuUtilization, databaseConnections);
        if (cpuUtilization > 50 || databaseConnections > 350){
            logger.error("Database is busy,  reset is not done");
            return "Database is busy, reset is not done";
        }

        logger.info("Start update RDS type to {}", RdsHelper.DEFAULT_RDS_TYPE);
        return RdsHelper.changeRdsInstanceType(RdsHelper.DEFAULT_RDS_TYPE, context);
    }

    private boolean needUpgrade(){
        double cpuUtilization = CloudwatchHelper.getCpuUtilization();
        double databaseConnections = CloudwatchHelper.getDatabaseConnections();
        logger.info("Current average cpuUnitilization : {},  DB connections: {}", cpuUtilization, databaseConnections);

        return cpuUtilization > 50 && databaseConnections > 350;
    }

}
