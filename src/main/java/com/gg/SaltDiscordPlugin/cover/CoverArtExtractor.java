package com.gg.SaltDiscordPlugin.cover;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;

/**
 * 封面图片提取器
 */
public class CoverArtExtractor {

    /**
     * 从音频文件中提取封面图片
     *
     * @param audioFilePath 音频文件路径
     * @return 封面图片数据，如果没有封面则返回 null
     */
    public static CoverArtData extractCoverArt(String audioFilePath) {
        try {
            File audioFile = new File(audioFilePath);
            if (!audioFile.exists()) {
                return null;
            }

            AudioFile f = AudioFileIO.read(audioFile);
            Tag tag = f.getTag();
            
            if (tag == null) {
                return null;
            }

            Artwork artwork = tag.getFirstArtwork();
            if (artwork == null) {
                System.out.println("音频文件没有封面图片: " + audioFilePath);
                return null;
            }

            byte[] originalImageData = artwork.getBinaryData();
            String originalMimeType = artwork.getMimeType();

            if (originalImageData == null || originalImageData.length == 0) {
                System.out.println("封面图片数据为空: " + audioFilePath);
                return null;
            }

            // 压缩并转换为 JPEG 格式，控制文件大小在 150KB 以内
            byte[] compressedImageData = compressAndConvertToJpg(originalImageData);
            if (compressedImageData == null) {
                return new CoverArtData(originalImageData, "cover" + getFileExtensionFromMimeType(originalMimeType), originalMimeType);
            }

            String fileName = "cover.jpeg";
            String mimeType = "image/jpeg";

            return new CoverArtData(compressedImageData, fileName, mimeType);

        } catch (Exception e) {
            System.err.println("提取封面图片失败: " + audioFilePath + ", 错误: " + e.getMessage());
            return null;
        }
    }

    /**
     * 压缩图片并转换为 JPEG 格式，控制文件大小在 130KB 以内
     *
     * @param originalImageData 原始图片数据
     * @return 压缩后的 JPEG 图片数据，失败时返回 null
     */
    private static byte[] compressAndConvertToJpg(byte[] originalImageData) {
        final int MAX_FILE_SIZE = 130 * 1024; // 130KB
        try {
            // 读取原始图片
            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(originalImageData));
            if (originalImage == null) {
                return null;
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
            byte[] compressedData = compressToJpegWithQuality(targetImage, MAX_FILE_SIZE);

            if (compressedData == null) {
                return null;
            }

            return compressedData;

        } catch (IOException e) {
            System.err.println("图片压缩失败: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("图片处理异常: " + e.getMessage());
            return null;
        }
    }

    /**
     * 使用质量控制将图片压缩为 JPEG 格式，确保文件大小在指定范围内
     *
     * @param image 要压缩的图片
     * @param maxFileSize 最大文件大小（字节）
     * @return 压缩后的 JPEG 数据，失败时返回 null
     */
    private static byte[] compressToJpegWithQuality(BufferedImage image, int maxFileSize) {
        try {
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
            if (!writers.hasNext()) {
                return null;
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
                    if (data.length <= maxFileSize) {
                        writer.dispose();
                        return data;
                    }
                } catch (IOException e) {
                    System.err.println("质量 " + quality + " 压缩失败: " + e.getMessage());
                }
            }

            writer.dispose();
            System.err.println("❌ 无法将图片压缩到 " + (maxFileSize / 1024) + "KB 以内");

            // 如果所有质量级别都无法满足大小要求，返回最低质量的结果
            return compressWithLowestQuality(image);

        } catch (Exception e) {
            System.err.println("JPEG 压缩异常: " + e.getMessage());
            return null;
        }
    }

    /**
     * 使用最低质量压缩图片
     */
    private static byte[] compressWithLowestQuality(BufferedImage image) {
        try {
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
            if (!writers.hasNext()) {
                return null;
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
            return null;
        }
    }

    /**
     * 根据 MIME 类型获取文件扩展名
     */
    public static String getFileExtensionFromMimeType(String mimeType) {
        if (mimeType == null) {
            return ".jpg"; // 默认扩展名
        }
        
        switch (mimeType.toLowerCase()) {
            case "image/jpeg":
            case "image/jpg":
                return ".jpg";
            case "image/png":
                return ".png";
            case "image/gif":
                return ".gif";
            case "image/bmp":
                return ".bmp";
            case "image/webp":
                return ".webp";
            default:
                return ".jpg"; // 默认扩展名
        }
    }

    /**
     * 封面图片数据类
     */
    public static class CoverArtData {
        private final byte[] imageData;
        private final String fileName;
        private final String mimeType;

        public CoverArtData(byte[] imageData, String fileName, String mimeType) {
            this.imageData = imageData;
            this.fileName = fileName;
            this.mimeType = mimeType;
        }

        public byte[] getImageData() {
            return imageData;
        }

        public String getFileName() {
            return fileName;
        }

        public String getMimeType() {
            return mimeType;
        }
    }
}
