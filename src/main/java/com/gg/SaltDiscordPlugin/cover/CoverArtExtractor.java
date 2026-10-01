package com.gg.SaltDiscordPlugin.cover;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

/**
 * 封面图片处理器：压缩、格式转换与 MIME 嗅探
 */
public class CoverArtExtractor {
    private static final int MAX_FILE_SIZE = 130 * 1024; // 130KB

    /**
     * 处理原始封面图片字节：压缩并转换为 JPEG 格式，控制文件大小在 130KB 以内
     *
     * @param imageData 原始编码图片数据，如 JPEG 或 PNG
     * @param mimeType  原始图片 MIME 类型，压缩失败回退原图时用于命名
     * @return 处理后的封面图片数据，输入为空时返回 null
     */
    public static CoverArtData processImageBytes(byte[] imageData, String mimeType) {
        if (imageData == null || imageData.length == 0) {
            return null;
        }

        byte[] compressedImageData = compressAndConvertToJpg(imageData);
        if (compressedImageData.length == 0) {
            return new CoverArtData(imageData, "cover" + getFileExtensionFromMimeType(mimeType), mimeType);
        }

        return new CoverArtData(compressedImageData, "cover.jpeg", "image/jpeg");
    }

    /**
     * 按魔数嗅探图片 MIME 类型
     */
    public static String sniffMimeType(byte[] data) {
        if (data == null || data.length < 4) {
            return "image/jpeg";
        }

        if ((data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8) {
            return "image/jpeg";
        }
        if ((data[0] & 0xFF) == 0x89 && data[1] == 0x50 && data[2] == 0x4E && data[3] == 0x47) {
            return "image/png";
        }
        if (data[0] == 0x47 && data[1] == 0x49 && data[2] == 0x46) {
            return "image/gif";
        }
        if (data[0] == 0x42 && data[1] == 0x4D) {
            return "image/bmp";
        }
        if (data.length >= 12 && data[0] == 0x52 && data[1] == 0x49 && data[2] == 0x46 && data[3] == 0x46
                && data[8] == 0x57 && data[9] == 0x45 && data[10] == 0x42 && data[11] == 0x50) {
            return "image/webp";
        }
        return "image/jpeg";
    }

    /**
     * 压缩图片并转换为 JPEG 格式，控制文件大小在 130KB 以内
     *
     * @param originalImageData 原始图片数据
     * @return 压缩后的 JPEG 图片数据，失败时返回 null
     */
    private static byte[] compressAndConvertToJpg(byte[] originalImageData) {
        try {
            // 读取原始图片
            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(originalImageData));
            if (originalImage == null) {
                return new byte[0];
            }

            // 获取原始尺寸
            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();
            System.out.println("原始图片尺寸: " + originalWidth + "x" + originalHeight);

            // 计算目标尺寸 (保持宽高比，最大 800x800)
            int targetSize = 800;
            int targetWidth, targetHeight;

            if (originalWidth > originalHeight) {
                targetWidth = targetSize;
                targetHeight = (int) ((double) originalHeight / originalWidth * targetSize);
            } else {
                targetHeight = targetSize;
                targetWidth = (int) ((double) originalWidth / originalHeight * targetSize);
            }

            // 如果原图已经很小，不需要压缩
            if (originalWidth <= targetSize && originalHeight <= targetSize) {
                targetWidth = originalWidth;
                targetHeight = originalHeight;
            } else {
                System.out.println("压缩后尺寸: " + targetWidth + "x" + targetHeight);
            }

            // 创建目标图片 (RGB 格式，去除透明度)
            BufferedImage targetImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = targetImage.createGraphics();

            // 设置高质量渲染
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 填充白色背景 (处理透明图片)
            g2d.setColor(java.awt.Color.WHITE);
            g2d.fillRect(0, 0, targetWidth, targetHeight);

            // 绘制缩放后的图片
            Image scaledImage = originalImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
            g2d.drawImage(scaledImage, 0, 0, null);
            g2d.dispose();

            // 使用质量控制转换为 JPEG 格式
            return compressToJpegWithQuality(targetImage);

        } catch (IOException e) {
            System.err.println("图片压缩失败: " + e.getMessage());
            return new byte[0];
        } catch (Exception e) {
            System.err.println("图片处理异常: " + e.getMessage());
            return new byte[0];
        }
    }

    /**
     * 使用质量控制将图片压缩为 JPEG 格式，确保文件大小在指定范围内
     *
     * @param image 要压缩的图片
     * @return 压缩后的 JPEG 数据，失败时返回 null
     */
    private static byte[] compressToJpegWithQuality(BufferedImage image) {
        try {
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
            if (!writers.hasNext()) {
                return new byte[0];
            }

            ImageWriter writer = writers.next();
            ImageWriteParam writeParam = writer.getDefaultWriteParam();
            writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);

            // 尝试不同的质量级别，从高到低
            float[] qualityLevels = {0.95f, 0.90f, 0.85f, 0.80f, 0.75f, 0.70f, 0.65f, 0.60f, 0.55f, 0.50f};

            for (float quality : qualityLevels) {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

                try (ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(outputStream)) {
                    writer.setOutput(imageOutputStream);
                    writeParam.setCompressionQuality(quality);

                    writer.write(null, new javax.imageio.IIOImage(image, null, null), writeParam);
                    imageOutputStream.flush();

                    byte[] data = outputStream.toByteArray();

                    // 如果文件大小符合要求，返回结果
                    if (data.length <= CoverArtExtractor.MAX_FILE_SIZE) {
                        writer.dispose();
                        return data;
                    }
                } catch (IOException e) {
                    System.err.println("质量 " + quality + " 压缩失败: " + e.getMessage());
                }
            }

            writer.dispose();
            System.err.println("❌ 无法将图片压缩到 " + (CoverArtExtractor.MAX_FILE_SIZE / 1024) + "KB 以内");

            // 如果所有质量级别都无法满足大小要求，返回最低质量的结果
            return compressWithLowestQuality(image);

        } catch (Exception e) {
            System.err.println("JPEG 压缩异常: " + e.getMessage());
            return new byte[0];
        }
    }

    /**
     * 使用最低质量压缩图片
     */
    private static byte[] compressWithLowestQuality(BufferedImage image) {
        try {
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
            if (!writers.hasNext()) {
                return new byte[0];
            }

            ImageWriter writer = writers.next();
            ImageWriteParam writeParam = writer.getDefaultWriteParam();
            writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            writeParam.setCompressionQuality(0.3f); // 最低质量

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            try (ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(outputStream)) {
                writer.setOutput(imageOutputStream);
                writer.write(null, new javax.imageio.IIOImage(image, null, null), writeParam);
                imageOutputStream.flush();

                byte[] data = outputStream.toByteArray();
                System.out.println("⚠️ 使用最低质量 30%，文件大小: " + String.format("%.1f KB", data.length / 1024.0));

                writer.dispose();
                return data;
            }
        } catch (Exception e) {
            System.err.println("最低质量压缩失败: " + e.getMessage());
            return new byte[0];
        }
    }

    /**
     * 根据 MIME 类型获取文件扩展名
     */
    public static String getFileExtensionFromMimeType(String mimeType) {
        if (mimeType == null) {
            return ".jpg"; // 默认扩展名
        }

        return switch (mimeType.toLowerCase()) {
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/bmp" -> ".bmp";
            case "image/webp" -> ".webp";
            default -> ".jpg"; // 默认扩展名
        };
    }

    /**
     * 封面图片数据类
     */
    public record CoverArtData(byte[] imageData, String fileName, String mimeType) {}
}
