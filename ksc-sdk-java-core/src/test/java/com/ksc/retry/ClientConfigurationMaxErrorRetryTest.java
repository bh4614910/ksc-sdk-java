/*
 * Copyright 2010-2016 ksyun.com, Inc. or its affiliates. All Rights
 * Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://ksyun.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is
 * distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either
 * express or implied. See the License for the specific language
 * governing
 * permissions and limitations under the License.
 */
package com.ksc.retry;

import com.ksc.KscClientException;
import com.ksc.KscServiceException;
import com.ksc.KscWebServiceRequest;
import com.ksc.ClientConfiguration;
import com.ksc.http.KSCHttpClient;
import com.ksc.http.ExecutionContext;
import com.ksc.http.response.NullResponseHandler;
import com.ksc.retry.PredefinedRetryPolicies;
import com.ksc.retry.RetryPolicy;
import com.ksc.util.RetryTestUtils;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;

/**
 * Tests the behavior when both
 * {@link ClientConfiguration#setMaxErrorRetry(int)} and
 * {@link ClientConfiguration#setRetryPolicy(RetryPolicy)} are used.
 */
public class ClientConfigurationMaxErrorRetryTest extends RetryPolicyTestBase {

    private static final Random random = new Random();
    
    @Before
    public void resetClientConfiguration() {
        clientConfiguration = new ClientConfiguration();
        testedClient = new KSCHttpClient(clientConfiguration);
        injectMockHttpClient(testedClient, new ReturnServiceErrorHttpClient(500, "fake 500 service error"));
    }

    /**
     * -- No explicit calls on ClientConfiguration#setMaxErrorRetry(int);
     * -- Default RetryPolicy's.
     */
    @Test
    public void testDefaultMaxErrorRetry() {
        /* SDK default */
        Assert.assertTrue(clientConfiguration.getRetryPolicy() == PredefinedRetryPolicies.DEFAULT);
        
        // Don't change any of the default settings in ClientConfiguration
        testActualRetries(PredefinedRetryPolicies.DEFAULT_MAX_ERROR_RETRY);
    }
    
    /**
     * -- Explicitly set maxErrorRetry in ClientConfiguration level;
     * -- Default/custom RetryPolicy's that don't override such setting.
     */
    @Test
    public void testClientConfigLevelMaxErrorRetry() {
        int CLIENT_CONFIG_LEVEL_MAX_RETRY = random.nextInt(3);
        clientConfiguration.setMaxErrorRetry(CLIENT_CONFIG_LEVEL_MAX_RETRY);

        // SDK default policy should honor the ClientConfig level maxErrorRetry
        testActualRetries(CLIENT_CONFIG_LEVEL_MAX_RETRY);

        // A custom policy that honors the ClientConfig level maxErrorRetry
        clientConfiguration.setRetryPolicy(new RetryPolicy(null, null, 5, true));
        testActualRetries(CLIENT_CONFIG_LEVEL_MAX_RETRY);
    }

    @Test
    public void testNoRetry() {
        final int CLIENT_CONFIG_LEVEL_MAX_RETRY = 3;
        clientConfiguration.setRetryPolicy(PredefinedRetryPolicies.NO_RETRY_POLICY);
        clientConfiguration.setMaxErrorRetry(CLIENT_CONFIG_LEVEL_MAX_RETRY);
        
        // Ignore the ClientConfig level maxErrorRetry
        testActualRetries(0);
    }
    
    /**
     * -- Explicitly set maxErrorRetry in ClientConfiguration level;
     * -- Custom RetryPolicy's that want to override such setting.
     */
    @Test
    public void testRetryPolicyLevelMaxErrorRetry() {
        // This should be ignored
        clientConfiguration.setMaxErrorRetry(random.nextInt(3));
        
        // A custom policy that doesn't honor the ClientConfig level maxErrorRetry
        int RETRY_POLICY_LEVEL_MAX_ERROR_RETRY = 5;
        clientConfiguration.setRetryPolicy(new RetryPolicy(null, null, RETRY_POLICY_LEVEL_MAX_ERROR_RETRY, false));
        testActualRetries(RETRY_POLICY_LEVEL_MAX_ERROR_RETRY);
        
        // A custom policy that "honors" the ClientConfig level maxErrorRetry,
        // but actually denies any retry in its condition.
        clientConfiguration.setRetryPolicy(new RetryPolicy(
                new RetryPolicy.RetryCondition() {

                    @Override
                    public boolean shouldRetry(
                            KscWebServiceRequest originalRequest,
                            KscClientException exception,
                            int retriesAttempted) {
                        return false;
                    }
                }, null, RETRY_POLICY_LEVEL_MAX_ERROR_RETRY, true)
        );
        // No retry is expected
        testActualRetries(0);
    }
    
    /**
     * Verifies the request is actually retried for the expected times.
     */
    private static void testActualRetries(int expectedRetryAttempts) {
        // The ExecutionContext should collect the expected RequestCount
        ExecutionContext context = new ExecutionContext(true);

        try {
            testedClient.execute(getSampleRequestWithRepeatableContent(originalRequest),
                                 new NullResponseHandler(),
                                 errorResponseHandler,
                                 context);
            Assert.fail("AmazonServiceException is expected.");
        } catch (KscServiceException ase) {}

        RetryTestUtils.assertExpectedRetryCount(expectedRetryAttempts, context);
    }
}
