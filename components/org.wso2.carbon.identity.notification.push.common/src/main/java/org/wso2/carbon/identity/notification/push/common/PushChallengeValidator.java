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

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.notification.push.common.exception.PushTokenValidationException;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.text.ParseException;
import java.util.Base64;
import java.util.Date;

/**
 * JWT token validator for Push notification scenarios.
 */
public class PushChallengeValidator {

    public static final String SIGNING_ALGORITHM = "RSA";
    private static final String DOT_SEPARATOR = ".";
    private static final Log log = LogFactory.getLog(PushChallengeValidator.class);

    /**
     * Validate the JWT token.
     *
     * @param jwt       JWT token to be validated
     * @param publicKey Public key used for signing the JWT
     * @return JWTClaimsSet
     * @throws PushTokenValidationException Error when validating the JWT token
     */
    public static JWTClaimsSet getValidatedClaimSet(String jwt, String publicKey) throws PushTokenValidationException {

        if (!isJWT(jwt)) {
            throw new PushTokenValidationException("Token is not a valid JWT.");
        }

        try {
            SignedJWT signedJWT = SignedJWT.parse(jwt);
            JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
            if (claimsSet == null) {
                throw new PushTokenValidationException("Token validation failed. Claim values were not found.");
            }

            if (!validateSignature(publicKey, signedJWT)) {
                throw new PushTokenValidationException("Token signature validation failed.");
            }

            if (!checkExpirationTime(claimsSet.getExpirationTime())) {
                throw new PushTokenValidationException("Token validation failed. JWT is expired.");
            }

            return claimsSet;
        } catch (ParseException e) {
            throw new PushTokenValidationException("Error occurred while parsing the JWT token.", e);
        }
    }

    /**
     * Validate the legitimacy of JWT token.
     *
     * @param jwt JWT token.
     * @return True if the JWT token is valid.
     */
    private static boolean isJWT(String jwt) {

        if (StringUtils.isBlank(jwt)) {
            return false;
        }
        if (StringUtils.countMatches(jwt, DOT_SEPARATOR) != 2) {
            return false;
        }
        try {
            SignedJWT.parse(jwt);
            return true;
        } catch (ParseException e) {
            if (log.isDebugEnabled()) {
                log.debug("Error occurred while parsing the JWT token.", e);
            }
            return false;
        }
    }

    /**
     * Validate the signature of the JWT.
     *
     * @param publicKeyStr Public key for used for signing the JWT
     * @param signedJWT    Signed JWT
     * @return Boolean value for signature validation
     * @throws PushTokenValidationException Error when validating the signature
     */
    private static boolean validateSignature(String publicKeyStr, SignedJWT signedJWT)
            throws PushTokenValidationException {

        try {
            byte[] publicKeyData = Base64.getDecoder().decode(publicKeyStr);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(publicKeyData);
            KeyFactory keyFactory = KeyFactory.getInstance(SIGNING_ALGORITHM);
            RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(spec);

            JWSVerifier verifier = new RSASSAVerifier(publicKey);
            return signedJWT.verify(verifier);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | JOSEException e) {
            throw new PushTokenValidationException("Error occurred when validating token signature.", e);
        }
    }

    /**
     * Validate if the JWT is expired.
     *
     * @param expirationTime Time set for the JWT to expire
     * @return Boolean validating if the JWT is not expired
     */
    private static boolean checkExpirationTime(Date expirationTime) {

        long expirationTimeInMillis = expirationTime.getTime();
        long currentTimeInMillis = System.currentTimeMillis();
        return (currentTimeInMillis) <= expirationTimeInMillis;
    }

    /**
     * Validate the challenge received from the device.
     *
     * @param claimsSet JWT claim set
     * @param challenge Challenge received from the device
     * @param deviceId  Device ID
     * @return Boolean value for challenge validation
     */
    public static boolean validateChallenge(JWTClaimsSet claimsSet, String challengeType,
                                            String challenge, String deviceId) {

        if (claimsSet == null) {
            if (log.isDebugEnabled()) {
                String message = String.format("Failed to validate the challenge. JWT claim set " +
                        "received from device %s  was null.", deviceId);
                log.debug(message);
            }
            return false;
        }

        try {
            String tokenChallenge = getClaimFromClaimSet(claimsSet, challengeType, deviceId);
            if (!challenge.equals(tokenChallenge)) {
                if (log.isDebugEnabled()) {
                    String message = String
                            .format("The challenge: %s received from deviceId: %s does not match the provided " +
                                    "challenge: %s. Returning false.", tokenChallenge, deviceId, challenge);
                    log.debug(message);
                }
                return false;
            } else {
                return true;
            }
        } catch (PushTokenValidationException e) {
            log.error("Error when getting the claims from the claim set", e);
            return false;
        }
    }

    /**
     * Get JWT claim from the claim set.
     *
     * @param claimsSet JWT claim set
     * @param claim     Required claim
     * @param deviceId  Device ID
     * @return Claim string
     * @throws PushTokenValidationException if an error occurs while getting a claim
     */
    public static String getClaimFromClaimSet(JWTClaimsSet claimsSet, String claim, String deviceId)
            throws PushTokenValidationException {

        try {
            return claimsSet.getStringClaim(claim);
        } catch (ParseException e) {
            String errorMessage = String.format("Failed to get %s from the claim set received from device: "
                    + "%s.", claim, deviceId);
            throw new PushTokenValidationException(errorMessage, e);
        }
    }
}
