package com.yourapp.media;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CloudinaryStorageService {

    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;
    private final String folder;
    private final boolean enabled;
    private final RestTemplate restTemplate = new RestTemplate();

    public CloudinaryStorageService(
            @Value("${cloudinary.cloud-name:}") String cloudName,
            @Value("${cloudinary.api-key:}") String apiKey,
            @Value("${cloudinary.api-secret:}") String apiSecret,
            @Value("${cloudinary.folder:social-app}") String folder) {
        this.cloudName = cloudName;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.folder = StringUtils.hasText(folder) ? folder : "social-app";
        this.enabled = StringUtils.hasText(cloudName) && StringUtils.hasText(apiKey) && StringUtils.hasText(apiSecret);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Map<String, Object> upload(MultipartFile file) throws IOException {
        if (!enabled) {
            throw new IllegalStateException("Cloudinary is not configured");
        }

        String publicId = UUID.randomUUID().toString();
        String url = "https://api.cloudinary.com/v1_1/" + cloudName + "/upload";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        long timestamp = System.currentTimeMillis() / 1000;
        
        // Build parameters for signature (only these, sorted alphabetically, NOT including api_key or resource_type)
        TreeMap<String, String> signatureParams = new TreeMap<>();
        signatureParams.put("folder", folder);
        signatureParams.put("public_id", publicId);
        signatureParams.put("timestamp", String.valueOf(timestamp));

        // Compute signature
        String signature = computeSignature(signatureParams);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new org.springframework.core.io.ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload";
            }
        });
        body.add("api_key", apiKey);
        body.add("timestamp", String.valueOf(timestamp));
        body.add("public_id", publicId);
        body.add("folder", folder);
        body.add("signature", signature);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, requestEntity, Map.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IOException("Cloudinary upload failed: " + response.getBody());
        }

        Map<String, Object> result = new HashMap<>();
        Map<String, Object> bodyMap = response.getBody();
        result.put("url", bodyMap.get("secure_url"));
        result.put("publicId", bodyMap.get("public_id"));
        result.put("resourceType", bodyMap.get("resource_type"));
        result.put("format", bodyMap.get("format"));
        return result;
    }

    private String computeSignature(TreeMap<String, String> params) throws IOException {
        StringBuilder toSign = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (toSign.length() > 0) {
                toSign.append("&");
            }
            toSign.append(entry.getKey()).append("=").append(entry.getValue());
        }
        toSign.append(apiSecret);

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] messageDigest = md.digest(toSign.toString().getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new IOException("Failed to compute signature", e);
        }
    }
}
