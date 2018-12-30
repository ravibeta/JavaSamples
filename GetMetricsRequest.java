package com.emc.ecs.s3.sample;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetMetricsRequest {
    private static final Logger logger = LoggerFactory.getLogger(GetMetricsRequest.class);

    public static byte[] computeHmacSHA256(byte[] key, String data) throws NoSuchAlgorithmException, InvalidKeyException, IllegalStateException,
            UnsupportedEncodingException {
        String algorithm = "HmacSHA256";
        String charsetName = "UTF-8";

        Mac sha256_HMAC = Mac.getInstance(algorithm);
        SecretKeySpec secret_key = new SecretKeySpec(key, algorithm);
        sha256_HMAC.init(secret_key);

        return sha256_HMAC.doFinal(data.getBytes(charsetName));
    }

    public static byte[] computeHmacSHA256(String key, String data) throws NoSuchAlgorithmException, InvalidKeyException, IllegalStateException,
            UnsupportedEncodingException {
        return computeHmacSHA256(key.getBytes(), data);
    }

    public static String getSignatureV4(String accessSecretKey, String date, String region, String regionService, String signing, String stringToSign)
            throws InvalidKeyException, NoSuchAlgorithmException, IllegalStateException, UnsupportedEncodingException {

        byte[] dateKey = computeHmacSHA256(accessSecretKey, date);
        logger.debug("dateKey: {}", encodeToString(dateKey));

        byte[] dateRegionKey = computeHmacSHA256(dateKey, region);
        logger.debug("dateRegionKey: {}", encodeToString(dateRegionKey));

        byte[] dateRegionServiceKey = computeHmacSHA256(dateRegionKey, regionService);
        logger.debug("dateRegionServiceKey: {}", encodeToString(dateRegionServiceKey));

        byte[] signingKey = computeHmacSHA256(dateRegionServiceKey, signing);
        logger.debug("signingKey: {}", encodeToString(signingKey));

        byte[] signature = computeHmacSHA256(signingKey, stringToSign);
        logger.debug("signature: {}", encodeToString(signature));

        return encodeToString(signature);
    }
    private static final char[] DIGITS = {
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };
    public static String encodeToString(byte[] bytes) {
        char[] encodedChars = encode(bytes);
        return new String(encodedChars);
    }
    public static char[] encode(byte[] data) {

        int l = data.length;

        char[] out = new char[l << 1];

        // two characters form the hex value.
        for (int i = 0, j = 0; i < l; i++) {
            out[j++] = DIGITS[(0xF0 & data[i]) >>> 4];
            out[j++] = DIGITS[0x0F & data[i]];
        }

        return out;
    }
    protected static int toDigit(char ch, int index) throws IllegalArgumentException {
        int digit = Character.digit(ch, 16);
        if (digit == -1) {
            throw new IllegalArgumentException("Illegal hexadecimal character " + ch + " at index " + index);
        }
        return digit;
    }
    protected static byte[] HmacSHA256(String data, byte[] key) throws Exception {
        String algorithm="HmacSHA256";
        Mac mac = Mac.getInstance(algorithm);
        mac.init(new SecretKeySpec(key, algorithm));
        return mac.doFinal(data.getBytes("UTF8"));
    }

    protected static byte[] getSignatureKey(String key, String dateStamp, String regionName, String serviceName) throws Exception {
        byte[] kSecret = ("AWS4" + key).getBytes("UTF8");
        byte[] kDate = HmacSHA256(dateStamp, kSecret);
        byte[] kRegion = HmacSHA256(regionName, kDate);
        byte[] kService = HmacSHA256(serviceName, kRegion);
        byte[] kSigning = HmacSHA256("aws4_request", kService);
        return kSigning;
    }

    public static void main(String[] args) throws InvalidKeyException, NoSuchAlgorithmException, IllegalStateException, UnsupportedEncodingException {
        String AWS_ACCESS_KEY_ID="My_access_key";
        String AWS_SECRET_ACCESS_KEY="My_access_secret";
        String AWS_service="monitoring";
        String AWS_host="monitoring.us-east-1.amazonaws.com";
        String AWS_region="us-east-1";
        String AWS_endpoint="https://monitoring.us-east-1.amazonaws.com";
        String AWS_request_parameters="Action=GetMetricStatistics&Version=2010-08-01";

        String accessSecretKey = "My_access_secret";
        String date = "20130806";
        String region = "us-east-1";
        String regionService = "monitoring";
        String signing = "aws4_request";
        String request_parameters = "{";
        request_parameters += "    \"Action\": \"GetMetricStatistics\", ";
        request_parameters += "    \"Namespace\": \"On-PremiseObjectStorageMetrics\",";
        request_parameters += "    \"MetricName\": \"BucketSizeBytes \",";
        request_parameters += "    \"Dimensions\": [";
        request_parameters += "        {";
        request_parameters += "            \"Name\": \"BucketName\",";
        request_parameters += "            \"Value\": \"ExampleBucket\"";
        request_parameters += "        }";
        request_parameters += "    ],";
        request_parameters += "    \"StartTime\": 1545884562,";
        request_parameters += "    \"EndTime\":  1545884662,";
        request_parameters += "    \"Period\": 86400,";
        request_parameters += "    \"Statistics\": [";
        request_parameters += "        \"Average\"";
        request_parameters += "    ],";
        request_parameters += "    \"Unit\": \"Bytes\"";
        request_parameters += "}";

        logger.info("signature: {}", getSignatureV4(accessSecretKey, date, region, regionService, signing, request_parameters));
    }

}
//
// output:
// [main] INFO com.emc.ecs.s3.sample.GetMetricsRequest - signature: 3758baea5fa2e4dd731c5e9804b6c0f41a3b230dbd530223842ba1afbb56c015
//
