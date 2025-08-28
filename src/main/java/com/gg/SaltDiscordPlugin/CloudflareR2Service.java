package com.gg.SaltDiscordPlugin;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Cloudflare R2 存储服务
 */
public class CloudflareR2Service {
    private final S3Client s3Client;
    private final String bucketName;
    private final String publicUrl;

    public CloudflareR2Service(String accessKey, String secretKey, String bucketName, String endPoint, String publicUrl) {
        this.bucketName = bucketName;
        // 使用用户提供的公开 URL
        this.publicUrl = publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;

        // 创建 AWS 凭证
        AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(accessKey, secretKey);
        System.out.println("Cloudflare R2 凭证创建成功");

        // 构建 Cloudflare R2 的端点 URL
        String endpointUrl = String.format("https://%s.r2.cloudflarestorage.com", endPoint);

        S3Configuration serviceConfiguration = S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .build();

        // 创建 S3 客户端，配置为使用 Cloudflare R2
        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create(endpointUrl))
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .region(Region.of("auto"))
                .serviceConfiguration(serviceConfiguration)
                .build();
    }

    /**
     * 上传封面图片到 R2 存储
     *
     * @param imageData 图片数据
     * @param fileName  文件名
     * @param mimeType  MIME 类型
     * @return 上传后的公共 URL
     */
    public String uploadCoverImage(byte[] imageData, String fileName, String mimeType) {
        try {
            // 生成唯一的文件名（使用 MD5 哈希）
            String uniqueFileName = generateUniqueFileName(imageData, fileName);

            // 创建上传请求
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key("covers/" + uniqueFileName) // 将封面放在 covers 目录下
                    .contentType(mimeType)
                    .build();

            RequestBody requestBody = RequestBody.fromBytes(imageData);

            // 执行上传
            PutObjectResponse response = s3Client.putObject(putObjectRequest, requestBody);

            if (response.sdkHttpResponse().isSuccessful()) {
                // 构建公共访问 URL
                String finalUrl = String.format("%s/covers/%s", this.publicUrl, uniqueFileName);
                System.out.println("封面上传成功: " + finalUrl);
                System.out.println("使用的公共URL: " + this.publicUrl);

                return finalUrl;
            } else {
                System.err.println("封面上传失败: " + response.sdkHttpResponse().statusText());
                System.err.println("HTTP 状态码: " + response.sdkHttpResponse().statusCode());
                return null;
            }

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 生成唯一的文件名
     */
    private String generateUniqueFileName(byte[] data, String originalFileName) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(data);
            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            // 获取文件扩展名
            String extension = "";
            int lastDotIndex = originalFileName.lastIndexOf('.');
            if (lastDotIndex > 0) {
                extension = originalFileName.substring(lastDotIndex);
            }

            return hexString.toString() + extension;

        } catch (NoSuchAlgorithmException e) {
            // 如果 MD5 不可用，使用时间戳
            return System.currentTimeMillis() + "_" + originalFileName;
        }
    }

    /**
     * 关闭客户端连接
     */
    public void close() {
        if (s3Client != null) {
            s3Client.close();
        }
    }
}
