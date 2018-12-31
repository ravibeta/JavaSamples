/*
package com.emc.ecs.monitoring.sample;
import java.net.*;
import java.io.*;
import java.util.*;
import javax.net.ssl.HttpsURLConnection;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetMetricsRequest {
    private static final Logger logger = LoggerFactory.getLogger(GetMetricsRequest.class);

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

    protected static Map<String, String> getHeaders(String amz_date, String authorization_header, String apiName, String content_type) {
        Map<String, String> headers = new HashMap<>();
        headers.put("x-amz-date", amz_date);
        headers.put("Authorization", authorization_header);
        headers.put("x-amz-target", "GraniteServiceVersion20100801."+apiName);
        headers.put("Content-Type", content_type);
        headers.put("Accept", "application/json");
        headers.put("Content-Encoding", "amz-1.0");
        headers.put("Connection", "keep-alive");
        return headers;
    }


    public static String getResponse(String httpsURL, Map<String, String> headers, String payload) throws Exception {
            URL myurl = new URL(httpsURL);
            String response = null;
            logger.info("Sending a post request to:"  + httpsURL);
            HttpsURLConnection con = (HttpsURLConnection)myurl.openConnection();
            con.setRequestMethod("POST");
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                con.setRequestProperty(entry.getKey(), entry.getValue());
            }
            con.setDoOutput(true);
            con.setDoInput(true);
            try (DataOutputStream output = new DataOutputStream(con.getOutputStream())) {
                output.writeBytes(payload);
            }
            try (DataInputStream input = new DataInputStream(con.getInputStream())) {
                StringBuffer contents = new StringBuffer();
                String tmp;
                while ((tmp = input.readLine()) != null) {
                    contents.append(tmp);
                    logger.debug("tmp="+tmp);
                }
                response = contents.toString();
            }
            logger.info("Resp Code:" + con.getResponseCode());
            logger.info("Resp Message:" + con.getResponseMessage());
            return response;
        }

    public static void main(String[] args) throws InvalidKeyException, NoSuchAlgorithmException, IllegalStateException, UnsupportedEncodingException {
        String AWS_ACCESS_KEY_ID="MyAccessKey";
        String AWS_SECRET_ACCESS_KEY="MyAccessSecret";
        String service="monitoring";
        String host="monitoring.us-east-1.amazonaws.com";
        String region="us-east-1";
        String endpoint="https://monitoring.us-east-1.amazonaws.com";
        String AWS_request_parameters="Action=GetMetricStatistics&Version=2010-08-01";
        String amz_date = "20181230T125500Z";
        String date_stamp = "20181230";
        String canonical_uri = "/";
        String canonical_querystring = "";
        String method = "POST";
        String apiName = "GetMetricStatistics";
        String content_type = "application/x-amz-json-1.0";
        String amz_target = "GraniteServiceVersion20100801."+apiName;
        String canonical_headers = "content-type:" + content_type + "\n" + "host:" + host + "\n" + "x-amz-date:" + amz_date + "\n" + "x-amz-target:" + amz_target + "\n";
        String signed_headers = "content-type;host;x-amz-date;x-amz-target";
        String accessKey = AWS_ACCESS_KEY_ID;
        String accessSecretKey = AWS_SECRET_ACCESS_KEY;
        String date = "20130806";
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

        try {
            String payload_hash = encodeToString(HmacSHA256(request_parameters, accessSecretKey.getBytes())); // hashlib.sha256(request_parameters.encode('utf-8')).hexdigest()
            String canonical_request = method + "\n" + canonical_uri + "\n" + canonical_querystring + "\n" + canonical_headers + "\n" + signed_headers + "\n" + payload_hash;
            String algorithm = "AWS4-HMAC-SHA256";
            String credential_scope = date_stamp + "/" + region + "/" + service + "/" + "aws4_request";
            String string_to_sign = algorithm + "\n" +  amz_date + "\n" +  credential_scope + "\n" +  encodeToString(HmacSHA256(canonical_request, accessSecretKey.getBytes()));
            // logger.info("signature: {}", getSignatureV4(accessSecretKey, date, region, regionService, signing, request_parameters));
            byte[] signing_key = getSignatureKey(accessSecretKey, date_stamp, region, service);
            String signature = encodeToString(HmacSHA256(string_to_sign, signing_key));
            logger.info("signature: {}", encodeToString(signing_key));
            String authorization_header = algorithm + " " + "Credential=" + accessKey + "/" + credential_scope + ", " +  "SignedHeaders=" + signed_headers + ", " + "Signature=" + signature;
            logger.info("authorization_header="+authorization_header);
            Map<String, String> headers= getHeaders(amz_date, authorization_header, apiName, content_type);
            logger.info("Sending request with:" + request_parameters);
            String response = getResponse(endpoint, headers, request_parameters);
            logger.info("response:"+response);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception:", e);
        }
    }
}
//
// output:
// [main] INFO com.emc.ecs.s3.sample.GetMetricsRequest - signature: c1391d813f0596e30497d180105f3e2a0defd24f4c5d15d0bdfa22dc905f7e42
//package com.emc.ecs.monitoring.sample;
*/
