package com.zzh.aws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.*;

import java.time.Instant;

public class CloudwatchHelper {
    private static final Logger logger = LoggerFactory.getLogger(CloudwatchHelper.class);

    private static double getMetricAverage(String metricName, String namespace) {
        CloudWatchClient cloudWatchClient = null;
        try {
            cloudWatchClient = CloudWatchClient.builder().build();

            Instant endTime = Instant.now();
            Instant startTime = endTime.minusSeconds(300); // Last 5 minutes

            GetMetricStatisticsRequest request = GetMetricStatisticsRequest.builder()
                    .namespace(namespace)
                    .metricName(metricName)
                    .startTime(startTime)
                    .endTime(endTime)
                    .period(60) // 1-minute interval
                    .statistics(Statistic.AVERAGE)
                    .dimensions(Dimension.builder()
                            .name("DBInstanceIdentifier")
                            .value(RdsHelper.RDS_INSTANCE_ID)
                            .build())
                    .build();

            GetMetricStatisticsResponse response = cloudWatchClient.getMetricStatistics(request);

            // Get the most recent data point's average (if available)
            return response.datapoints().stream()
                    .mapToDouble(dp -> dp.average())
                    .average() // Get the average of averages over the last 5 minutes
                    .orElse(0.0); // Default to 0 if no data points are available
        } catch (Exception e) {
            logger.error("Error on Cloudwatch getMtricAverage", e);
            return 0.0;
        } finally {
            if (cloudWatchClient != null) {
                cloudWatchClient.close();
            }
        }
    }

    public static double getCpuUtilization() {
        return getMetricAverage("CPUUtilization", "AWS/RDS");
    }

    // Method to get the average Database Connections
    public static double getDatabaseConnections() {
        return getMetricAverage("DatabaseConnections", "AWS/RDS");
    }

    public static void enableDisableAlarm(String alarmName, boolean enabled) {
        CloudWatchClient cloudWatchClient = null;
        try {
            cloudWatchClient = CloudWatchClient.builder().build();

            if (enabled) {
                EnableAlarmActionsRequest enableRequest = EnableAlarmActionsRequest.builder().alarmNames(alarmName).build();
                cloudWatchClient.enableAlarmActions(enableRequest);
            } else {
                DisableAlarmActionsRequest disableRequest = DisableAlarmActionsRequest.builder().alarmNames(alarmName).build();
                cloudWatchClient.disableAlarmActions(disableRequest);
            }
        } catch (Exception e) {
            logger.error("Error on Cloudwatch getMtricAverage", e);
        } finally {
            if (cloudWatchClient != null) {
                cloudWatchClient.close();
            }
        }
    }

}
