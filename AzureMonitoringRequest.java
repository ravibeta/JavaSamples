/*

    private static String getToken(String accessKeyId, String accessSecret, String ARMResource, String tenantId, String spnPayload) {
        String TokenEndpoint = "https://login.windows.net/{0}/oauth2/token";
        String address = String.format(TokenEndpoint, tenantId);
        try {
            // String SPNPayload = "resource={0}&client_id={1}&grant_type=client_credentials&client_secret={2}";
            String payload = String.format(spnPayload,
                    java.net.URLEncoder.encode(ARMResource,"UTF-8"),
                    java.net.URLEncoder.encode(accessKeyId, "UTF-8"),
                    java.net.URLEncoder.encode(accessSecret, "UTF-8"));

            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/x-www-form-urlencoded");
            headers.put("Accept", "application/json");
            headers.put("Content-Encoding", "UTF-8");
            headers.put("Connection", "keep-alive");
            return headers;

            String response = getResponse(canonical_uri, headers, request_parameters);
            logger.info("response:"+response);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception:", e);
        }
    }

*/
