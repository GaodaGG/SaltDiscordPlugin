# Cloudflare R2 配置指南

### 配置步骤

#### 1. 创建 R2 存储桶
1. 登录 Cloudflare 控制台
2. 进入 R2 Object Storage
3. 创建一个新的存储桶（例如 `my-music-covers`）、
4. 点击 "Settings" 标签
5. 记下 `S3 API`

#### 2. 创建 R2 API Token
1. 进入 R2 Object Storage
2. 点击 "API" 标签
3. 点击 "Manage API Tokens"
4. 点击 "Create Account API Token"
5. 选择 "Object Read & Write" 权限
6. 点击底部创建Token
7. 记下 `Access Key ID` 和 `Secret Access Key`

#### 3. 启用 R2 存储桶公共访问 URL

1. 登录 Cloudflare 控制台
2. 进入 R2 Object Storage
3. 选择您的存储桶
4. 点击 "Settings" 标签
5. 在 "Public Development URL" 部分，创建并复制公共访问 URL
   - 例如：`https://my-music-covers.r2.dev` 或 `https://cdn.yourdomain.com`（如果使用自定义域名）

### 4. 配置插件

- **cfc2Endpoint**: 提取自 S3 API 的 Endpoint（例如 `https://<end_point>.r2.cloudflarestorage.com`）
- **cfc2PublicUrl**: 您的存储桶公开访问 URL（用于生成图片链接）
- **cfc2BucketName**: 存储桶名称
- **cfc2AccessKey** 和 **cfc2SecretKey**: R2 API Token

