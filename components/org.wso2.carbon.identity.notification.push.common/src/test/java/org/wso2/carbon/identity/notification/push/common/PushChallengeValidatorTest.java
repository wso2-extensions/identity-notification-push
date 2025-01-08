/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.notification.push.common;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.notification.push.common.exception.PushTokenValidationException;

import java.text.ParseException;
import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertNotNull;

public class PushChallengeValidatorTest {

    @Mock
    private SignedJWT mockSignedJWT;

    @Mock
    private JWTClaimsSet mockClaimsSet;

    private String validJwt;
    private String invalidJwt;
    private String publicKey;
    private String invalidPublicKey;
    private String validChallenge;
    private String invalidChallenge;

    @BeforeTest
    public void setUp() throws ParseException {
        MockitoAnnotations.openMocks(this);

        validJwt = "eyJhbGciOiJSUzI1NiIsImRpZCI6IjM4NWViMDg1LWQ1NzUtNDU1ZS1iYjdhLTE5MmJmYTE1NTVkMCIsInR5cCI6IkpXVCJ9." +
                "eyJ0ZCI6ImNhcmJvbi5zdXBlciIsInBpZCI6ImY1YWU2YTBkLTM5MGEtNGVlYS1hMzgwLThiY2M4NmU0YTE0OCIsImNoZyI6ImUw" +
                "YzNkMDRjLTc1MGItNDMwMS04Zjc2LWUwN2ViZjAyZTUzYSIsInJlcyI6IkFQUFJPVkVEIiwibnVtIjoiMTAiLCJleHAiOjE3MzY" +
                "wNzYxMDUsIm5iZiI6MTczNjA3NTIwNX0.pBis-9330OboGW0fYGQavBY67G-QNfduB4dJQYQj58zfzKbCDjMfpdzjWAeIJXp28" +
                "WuYbxgsDzYVCTjbPHpAKRnghwD_a40K3mFQdeIxwRw3szaPDXK7n-OesSpR6T--1-HFs3n5OzSRUI9SxT-Cr-7cMQeyFa_" +
                "mYvTy1FrdlWUzfjlfeyjtd7bU_APn1jL3dEo6GctiaeTsTwVR9fo-toRJHVMBDMeRBuvGjApczeFC0EEO2Ug7vlms5WFkJLABLgN" +
                "dus9Vqel9rbOebvqyuFVAJh3Tj7uDvvQQRENnvtov7EiVQYDfJ66UfEQAjHQgWE43OzStUZbTvMMopERHiw";
        invalidJwt = "invalid.jwt";
        publicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAwSzv6RX1FZuWAekyZ8YNcZimTPHP+J9KEsFdmFUisKsF" +
                "CauIDkt3HBHwvHdZl4E2s09Bk6byBqNkF40h6N/Ag5RDptzgSjGDWSAWfYggD9bhh5kTNgZdp6tkMFOaoy3DDw46Z8ZG3XVm" +
                "Bam/935a88yPav3JCC2CiH5yJJ3ebKIsvl91Z6bAMi1IdMkrmOa2nBziZebZZ/JYJfqyYKPX44HUaAQmTc1hYvshI4FizPnE" +
                "t0PxM7dNrpLxXsb+vJJqV9Ua4wzNrl2GTW7Eb/5KEeCTu2FMhgFz6JQan3givV+G5vceGboNkKSKxIF5jBgBHLmkyIIf+/YAD4" +
                "38qJc+4wIDAQAB";
        invalidPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAst6/EZkLCR2MHIOE7366TzpiicBPzyOvIoUp6xm/YF" +
                "4OLpZ+WEL2QT8Xl3zUbr34aFuEWN0OUXTviSaM+yvp3B5vwglkCOkMv3BQNGZZB4HI9Wjj9OYD6K4Bdhr6nJidUX3LIIkslfTF" +
                "8oXGhIjBVKzd9BKDCaVmwpCFrQ8OtF/jCxFrNxTzYFVtulUSa8LW8E5jZhgbu6kLPU9dGTpq+6vwG+it7YFGUaFVtBOVcZgq1" +
                "pSnLcwP5iQAaHym+EGSu0I91yzTA5jWAer37BrfePRREmvg5E3Oi01Xr5dhvMSatlJjpZNaBsQ7tr/CKgM+uhRYBEejbSh43+" +
                "7QYWdvxQIDAQAB";
        validChallenge = "e0c3d04c-750b-4301-8f76-e07ebf02e53a";
        invalidChallenge = "invalid.challenge";
    }

    @Test
    public void testGetValidatedClaimSetWithValidToken() throws Exception {
        try (MockedStatic<SignedJWT> mockedStatic = Mockito.mockStatic(SignedJWT.class)) {
            mockedStatic.when(() -> SignedJWT.parse(validJwt)).thenReturn(mockSignedJWT);

            when(mockSignedJWT.getJWTClaimsSet()).thenReturn(mockClaimsSet);
            when(mockClaimsSet.getExpirationTime()).thenReturn(new Date(System.currentTimeMillis() + 3000000));
            when(mockClaimsSet.getNotBeforeTime()).thenReturn(new Date(System.currentTimeMillis() - 3000000));
            when(mockSignedJWT.verify(any())).thenReturn(true);

            JWTClaimsSet claimsSet = PushChallengeValidator.getValidatedClaimSet(validJwt, publicKey);
            assertNotNull(claimsSet);
        }
    }

    @Test(expectedExceptions = PushTokenValidationException.class)
    public void testGetValidatedClaimSetWithBlankToken() throws Exception {

        PushChallengeValidator.getValidatedClaimSet("", publicKey);
    }

    @Test(expectedExceptions = PushTokenValidationException.class)
    public void testGetValidatedClaimSetWithInvalidJwtToken() throws Exception {

        PushChallengeValidator.getValidatedClaimSet(invalidJwt, publicKey);
    }

    @Test(expectedExceptions = PushTokenValidationException.class)
    public void testGetValidatedClaimSetWithEmptyClaimsSet() throws Exception {

        try (MockedStatic<SignedJWT> mockedStatic = Mockito.mockStatic(SignedJWT.class)) {
            mockedStatic.when(() -> SignedJWT.parse(validJwt)).thenReturn(mockSignedJWT);
            when(mockSignedJWT.getJWTClaimsSet()).thenReturn(null);
            PushChallengeValidator.getValidatedClaimSet(validJwt, publicKey);
        }
    }

    @Test
    public void testGetValidatedClaimSetWithValidSignature() throws Exception {

        SignedJWT signedJWT = SignedJWT.parse(validJwt);
        Assert.assertTrue(PushChallengeValidator.validateSignature(publicKey, signedJWT));
    }

    @Test
    public void testGetValidatedClaimSetWithInvalidSignature() throws Exception {

        SignedJWT signedJWT = SignedJWT.parse(validJwt);
        Assert.assertFalse(PushChallengeValidator.validateSignature(invalidPublicKey, signedJWT));
    }

    @Test(expectedExceptions = PushTokenValidationException.class)
    public void testGetValidatedClaimSetWithExpiredToken() throws Exception {

        try (MockedStatic<SignedJWT> mockedStatic = Mockito.mockStatic(SignedJWT.class)) {
            mockedStatic.when(() -> SignedJWT.parse(validJwt)).thenReturn(mockSignedJWT);
            when(mockSignedJWT.getJWTClaimsSet()).thenReturn(mockClaimsSet);
            when(mockClaimsSet.getExpirationTime()).thenReturn(new Date(System.currentTimeMillis() - 3000000));
            when(mockSignedJWT.verify(any())).thenReturn(true);
            PushChallengeValidator.getValidatedClaimSet(validJwt, publicKey);
        }
    }

    @Test(expectedExceptions = PushTokenValidationException.class)
    public void testGetValidatedClaimSetWithNotActiveToken() throws Exception {

        try (MockedStatic<SignedJWT> mockedStatic = Mockito.mockStatic(SignedJWT.class)) {
            mockedStatic.when(() -> SignedJWT.parse(validJwt)).thenReturn(mockSignedJWT);
            when(mockSignedJWT.getJWTClaimsSet()).thenReturn(mockClaimsSet);
            when(mockClaimsSet.getExpirationTime()).thenReturn(new Date(System.currentTimeMillis() + 3000000));
            when(mockClaimsSet.getNotBeforeTime()).thenReturn(new Date(System.currentTimeMillis() + 3000000));
            when(mockSignedJWT.verify(any())).thenReturn(true);
            PushChallengeValidator.getValidatedClaimSet(validJwt, publicKey);
        }
    }

    @Test(expectedExceptions = PushTokenValidationException.class)
    public void testGetValidatedClaimSetWithNoNotBeforeTime() throws Exception {

        try (MockedStatic<SignedJWT> mockedStatic = Mockito.mockStatic(SignedJWT.class)) {
            mockedStatic.when(() -> SignedJWT.parse(validJwt)).thenReturn(mockSignedJWT);
            when(mockSignedJWT.getJWTClaimsSet()).thenReturn(mockClaimsSet);
            when(mockClaimsSet.getExpirationTime()).thenReturn(new Date(System.currentTimeMillis() + 3000000));
            when(mockClaimsSet.getNotBeforeTime()).thenReturn(null);
            when(mockSignedJWT.verify(any())).thenReturn(true);
            PushChallengeValidator.getValidatedClaimSet(validJwt, publicKey);
        }
    }

    @Test
    public void testValidateChallengeWithEmptyClaimsSet() {

        Assert.assertFalse(PushChallengeValidator.validateChallenge(null, "", "", ""));
    }

    @Test
    public void testValidateChallengeSuccess() throws ParseException {

        SignedJWT signedJWT = SignedJWT.parse(validJwt);
        JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
        Assert.assertTrue(PushChallengeValidator.validateChallenge(claimsSet, "chg", validChallenge, "sampleDevice"));
    }

    @Test
    public void testValidateChallengeWithInvalidChallenge() throws ParseException {

        SignedJWT signedJWT = SignedJWT.parse(validJwt);
        JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
        Assert.assertFalse(PushChallengeValidator.validateChallenge(claimsSet, "chg", invalidChallenge,
                "sampleDevice"));
    }

    @Test
    public void testGetClaimFromClaimSet() throws ParseException, PushTokenValidationException {

        SignedJWT signedJWT = SignedJWT.parse(validJwt);
        JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
        Assert.assertEquals(
                PushChallengeValidator.getClaimFromClaimSet(claimsSet, "chg", "sampleDevice"),
                validChallenge);
    }

    @Test
    public void testGetClaimFromClaimSetFail() throws ParseException, PushTokenValidationException {

        SignedJWT signedJWT = SignedJWT.parse(validJwt);
        JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
        Assert.assertNull(PushChallengeValidator.getClaimFromClaimSet(claimsSet, "chg2", "sampleDevice"));
    }
}
