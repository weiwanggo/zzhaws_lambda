package com.zzh.aws;

import com.amazonaws.services.lambda.runtime.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesRequest;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesResponse;
import software.amazon.awssdk.services.rds.model.ModifyDbInstanceRequest;
import software.amazon.awssdk.services.rds.model.ModifyDbInstanceResponse;

public class RdsHelper {
    public static final String RDS_INSTANCE_ID = System.getenv(Config.RDS_INSTANCE_ID);
    public static final String DEFAULT_RDS_TYPE = System.getenv(Config.DEFAULT_RDS_INSTANCE_TYPE);

    public static final Logger logger = LoggerFactory.getLogger(RdsHelper.class);

    public static String getCurrentRDSType() {
        RdsClient rdsClient = null;
        try {
            rdsClient = RdsClient.builder().build();
            DescribeDbInstancesRequest describeRequest = DescribeDbInstancesRequest.builder()
                    .dbInstanceIdentifier(RDS_INSTANCE_ID)
                    .build();
            DescribeDbInstancesResponse describeResponse = rdsClient.describeDBInstances(describeRequest);
            return describeResponse.dbInstances().get(0).dbInstanceClass();
        } catch (Exception e) {
            logger.error("Error getting current RDS instance type", e);
            return null;
        } finally {
            if(rdsClient != null){
                rdsClient.close();
            }
        }
    }

    public static String changeRdsInstanceType(String newInstanceType, Context context) {
        RdsClient rdsClient = null;
        try {
            rdsClient = RdsClient.builder().build();
            ModifyDbInstanceRequest modifyRequest = ModifyDbInstanceRequest.builder()
                    .dbInstanceIdentifier(RDS_INSTANCE_ID)
                    .dbInstanceClass(newInstanceType)
                    .applyImmediately(true)  // Apply changes immediately
                    .build();

            ModifyDbInstanceResponse modifyResponse = rdsClient.modifyDBInstance(modifyRequest);
            logger.info("Modify request status: {}", modifyResponse.dbInstance().dbInstanceClass());

            return "Successfully modified RDS instance " + RDS_INSTANCE_ID + " to " + newInstanceType;
        } catch (Exception e) {
            context.getLogger().log("Error modifying RDS instance: " + e.getMessage());
            return "Failed to modify RDS instance: " + e.getMessage();
        } finally {
            if(rdsClient != null){
                rdsClient.close();
            }
        }
    }

}
