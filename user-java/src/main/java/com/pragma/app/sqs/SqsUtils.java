package com.pragma.app.sqs;

import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

public class SqsUtils {

    public SqsUtils(){}

    public static SqsClient getSqsClient() {
        String regionEnv = System.getenv(SdkSystemSetting.AWS_REGION.environmentVariable());
        Region region = regionEnv != null ? Region.of(regionEnv) : Region.US_EAST_1;
        return SqsClient.builder()
                .region(region)
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build();
    }

}
