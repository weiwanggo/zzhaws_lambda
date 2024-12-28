package com.zzh.aws;

import com.amazonaws.services.lambda.runtime.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.cloudfront.CloudFrontClient;
import software.amazon.awssdk.services.cloudfront.model.*;

import java.util.List;
import java.util.stream.Collectors;

public class CloudfrontHelper {
    public static final String CLOUDFRONT_DIST_ID = System.getenv(Config.CLOUDFRONT_INSTANCE_ID);
    public static final String CLOUDFRONT_ORIGIN_ID = System.getenv(Config.CLOUDFRONT_ORIGIN_ID);

    private static CloudFrontClient cloudFrontClient;

    private static final Logger logger = LoggerFactory.getLogger(CloudfrontHelper.class);

    public static void updateCloudFrontOrigin(String publicDns, Context context) {
        try {
            cloudFrontClient = CloudFrontClient.builder().build();
            // Step 1: Retrieve the current distribution configuration
            GetDistributionConfigResponse getConfigResponse = cloudFrontClient.getDistributionConfig(
                    GetDistributionConfigRequest.builder()
                            .id(CLOUDFRONT_DIST_ID)
                            .build()
            );

            // Step 2: Identify and update only the EC2 origin
            List<Origin> updatedOrigins = getConfigResponse.distributionConfig().origins().items().stream()
                    .map(origin -> {
                        if (CLOUDFRONT_ORIGIN_ID.equals(origin.id())) { // Replace with your EC2 origin ID
                            return origin.toBuilder()
                                    .domainName(publicDns) // Update the domain name
                                    .build();
                        }
                        return origin;
                    })
                    .collect(Collectors.toList());

            // Step 3: Create the updated Origins object
            Origins origins = getConfigResponse.distributionConfig().origins().toBuilder()
                    .items(updatedOrigins)
                    .build();

            // Step 4: Build the updated DistributionConfig with the modified origins
            DistributionConfig config = getConfigResponse.distributionConfig().toBuilder()
                    .origins(origins)
                    .build();

            // Step 5: Update the CloudFront distribution with the new configuration
            cloudFrontClient.updateDistribution(UpdateDistributionRequest.builder()
                    .id(CLOUDFRONT_DIST_ID)
                    .ifMatch(getConfigResponse.eTag())
                    .distributionConfig(config)
                    .build()
            );

            context.getLogger().log("CloudFront origin updated to: " + publicDns);
        }catch(CloudFrontException e){
            logger.error("Error happened on updating cloudfront", e);
        }finally {
            cloudFrontClient.close();
        }
    }

}
