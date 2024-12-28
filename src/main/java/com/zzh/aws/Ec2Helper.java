package com.zzh.aws;

import com.amazonaws.services.lambda.runtime.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

public class Ec2Helper {
    public static final String EC2_INSTANCE_ID = System.getenv(Config.EC2_INSTANCE_ID);

    private static final Logger logger = LoggerFactory.getLogger(Ec2Helper.class);

    public static String changeEc2InstanceType(String newInstanceType, Context context) {
        Ec2Client ec2Client = null;
        logger.info("Update to new instance type : {}", newInstanceType);

        try {
            ec2Client = Ec2Client.builder().build();
            // Step 1: Stop the instance
            ec2Client.stopInstances(StopInstancesRequest.builder().instanceIds(EC2_INSTANCE_ID).build());
            waitForInstanceToStop(ec2Client, EC2_INSTANCE_ID, context);
            logger.info("EC2 instance {} is stopped", EC2_INSTANCE_ID);

            // Step 2: Modify the instance type
            ec2Client.modifyInstanceAttribute(ModifyInstanceAttributeRequest.builder()
                    .instanceId(EC2_INSTANCE_ID)
                    .instanceType(AttributeValue.builder().value(newInstanceType).build())
                    .build());
            logger.info("EC2 instance is modified to new instance type {}", newInstanceType);

            // Step 3: Start the instance
            ec2Client.startInstances(StartInstancesRequest.builder().instanceIds(EC2_INSTANCE_ID).build());
            waitForInstanceToStart(ec2Client, EC2_INSTANCE_ID, context);
            logger.info("EC2 instance {} is started", EC2_INSTANCE_ID);

            // Step 4: Retrieve the Public IPv4 DNS
            DescribeInstancesResponse response = ec2Client.describeInstances(DescribeInstancesRequest.builder()
                    .instanceIds(EC2_INSTANCE_ID)
                    .build());
            String publicDns = response.reservations().get(0).instances().get(0).publicDnsName();

            // Step 5: Update CloudFront with new origin DNS
            CloudfrontHelper.updateCloudFrontOrigin(publicDns, context);

            logger.info("Done updating EC2 instance type to {}", newInstanceType);
            return "Instance modified to " + newInstanceType + ", restarted, and CloudFront updated successfully.";
        } catch (Exception e) {
            context.getLogger().log("Failed to modify instance or update CloudFront: " + e.getMessage());
            return "Error: " + e.getMessage();
        } finally {
            if (ec2Client != null){
                ec2Client.close();
            }
        }
    }

    private static void waitForInstanceToStop(Ec2Client ec2Client, String ec2InstanceId, Context context) {
        boolean isStopped = false;
        while (!isStopped) {
            DescribeInstancesResponse response = ec2Client.describeInstances(DescribeInstancesRequest.builder()
                    .instanceIds(ec2InstanceId)
                    .build());
            isStopped = response.reservations().stream()
                    .flatMap(r -> r.instances().stream())
                    .anyMatch(i -> i.state().name().equals(InstanceStateName.STOPPED));
            if (!isStopped) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private static void waitForInstanceToStart(Ec2Client ec2Client, String ec2InstanceId, Context context) {
        boolean isRunning = false;
        while (!isRunning) {
            DescribeInstancesResponse response = ec2Client.describeInstances(DescribeInstancesRequest.builder()
                    .instanceIds(ec2InstanceId)
                    .build());
            isRunning = response.reservations().stream()
                    .flatMap(r -> r.instances().stream())
                    .anyMatch(i -> i.state().name().equals(InstanceStateName.RUNNING));
            if (!isRunning) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public static String getCurrentInstanceType() {
        Ec2Client ec2Client = null;

        try{
            ec2Client = Ec2Client.builder().build();
            DescribeInstancesRequest request = DescribeInstancesRequest.builder()
                    .instanceIds(EC2_INSTANCE_ID)
                    .build();

            DescribeInstancesResponse response = ec2Client.describeInstances(request);
            Instance instance = response.reservations().get(0).instances().get(0); // Get the first instance

            // Get the current instance type
            String currentType = instance.instanceTypeAsString();
            logger.info("Current Instance Type: {}", currentType);
            return currentType;
        } catch (Exception e) {
            logger.error("Error on getCurrentEc2InstanceType", e);
            return null;
        } finally {
            if (ec2Client != null){
                ec2Client.close();
            }
        }
    }

}
