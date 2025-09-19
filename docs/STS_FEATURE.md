# STS 临时访问凭证功能说明

## 概述

STS (Security Token Service) 临时访问凭证功能为 ContiNew Admin 提供了更安全、更高效的文件上传解决方案。通过使用STS临时凭证，客户端可以直接向S3兼容的存储服务上传文件，而无需通过服务器中转，从而减少服务器带宽消耗并提高上传安全性。

## 核心优势

### 安全性
- **临时凭证**：生成的凭证具有时效性，过期后自动失效
- **权限限制**：通过IAM策略限制临时凭证只能访问指定桶和路径前缀
- **最小权限原则**：临时凭证仅授予必要的上传权限

### 性能优化
- **直接上传**：客户端直接上传到存储服务，减少服务器负载
- **缓存机制**：避免频繁调用STS服务，减少API调用次数
- **分片支持**：支持大文件分片上传和断点续传

### 降低成本
- **带宽节省**：服务器无需处理文件传输，节省带宽成本
- **资源优化**：减少服务器CPU和内存使用

## 配置说明

### 应用配置 (application-sts.yml)

```yaml
continew:
  admin:
    sts:
      # 是否启用STS功能
      enabled: true
      # 默认凭证有效期(秒)
      default-duration-seconds: 3600
      # 最大凭证有效期(秒)
      max-duration-seconds: 7200
      # 最小凭证有效期(秒)
      min-duration-seconds: 900
      # 缓存过期时间(秒)，应该小于最小凭证有效期
      cache-expire-seconds: 600
      # 角色ARN配置 - 需要根据实际环境配置
      role-arn: "arn:aws:iam::123456789012:role/S3TempRole"
      # 是否严格模式，严格模式下只允许上传到指定前缀路径
      strict-mode: true
```

### IAM角色配置

在AWS或兼容S3的服务中，需要创建一个IAM角色，示例信任策略：

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Principal": {
                "AWS": "arn:aws:iam::YOUR_ACCOUNT:user/continew-admin"
            },
            "Action": "sts:AssumeRole"
        }
    ]
}
```

示例权限策略：

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "s3:PutObject",
                "s3:PutObjectAcl",
                "s3:AbortMultipartUpload",
                "s3:ListMultipartUploadParts"
            ],
            "Resource": "arn:aws:s3:::your-bucket/*"
        },
        {
            "Effect": "Allow",
            "Action": [
                "s3:ListBucket"
            ],
            "Resource": "arn:aws:s3:::your-bucket"
        }
    ]
}
```

## API接口

### 获取STS临时凭证

**接口**: `POST /system/sts/credentials`

**权限**: `system:storage:getStsCredentials`

**请求参数**:
```json
{
    "storageCode": "oss-default",
    "pathPrefix": "upload/files/",
    "durationSeconds": 3600
}
```

**响应示例**:
```json
{
    "accessKeyId": "ASIA...",
    "secretAccessKey": "...",
    "sessionToken": "...",
    "endpoint": "https://s3.amazonaws.com",
    "bucketName": "my-bucket",
    "region": "us-east-1",
    "expiration": "2025-01-01T11:00:00",
    "pathPrefix": "upload/files/"
}
```

### 刷新STS临时凭证缓存

**接口**: `POST /system/sts/refresh/{storageCode}`

**权限**: `system:storage:refreshStsCredentials`

## 使用方式

### 客户端实现

1. **获取临时凭证**
```javascript
const credentials = await fetch('/system/sts/credentials', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
        storageCode: 'oss-default',
        pathPrefix: 'upload/files/',
        durationSeconds: 3600
    })
}).then(res => res.json());
```

2. **使用临时凭证上传文件**
```javascript
const s3Client = new AWS.S3({
    accessKeyId: credentials.accessKeyId,
    secretAccessKey: credentials.secretAccessKey,
    sessionToken: credentials.sessionToken,
    endpoint: credentials.endpoint,
    region: credentials.region
});

// 上传文件
await s3Client.upload({
    Bucket: credentials.bucketName,
    Key: credentials.pathPrefix + fileName,
    Body: fileData
}).promise();
```

### 分片上传示例

```javascript
// 初始化分片上传
const multipart = await s3Client.createMultipartUpload({
    Bucket: credentials.bucketName,
    Key: credentials.pathPrefix + fileName
}).promise();

// 上传分片
const partPromises = [];
for (let i = 0; i < partCount; i++) {
    const partParams = {
        Bucket: credentials.bucketName,
        Key: credentials.pathPrefix + fileName,
        PartNumber: i + 1,
        UploadId: multipart.UploadId,
        Body: fileParts[i]
    };
    partPromises.push(s3Client.uploadPart(partParams).promise());
}

const parts = await Promise.all(partPromises);

// 完成分片上传
await s3Client.completeMultipartUpload({
    Bucket: credentials.bucketName,
    Key: credentials.pathPrefix + fileName,
    UploadId: multipart.UploadId,
    MultipartUpload: {
        Parts: parts.map((part, index) => ({
            ETag: part.ETag,
            PartNumber: index + 1
        }))
    }
}).promise();
```

## 最佳实践

### 1. 缓存策略
- 客户端应缓存临时凭证，避免频繁请求
- 在凭证过期前适当时间（如提前5分钟）刷新凭证
- 服务端设置合理的缓存时间，避免STS API限流

### 2. 错误处理
- 处理STS服务不可用的情况
- 实现降级机制，在STS不可用时回退到传统上传方式
- 对凭证过期错误进行重试

### 3. 安全考虑
- 合理设置凭证有效期，平衡安全性和用户体验
- 严格控制路径前缀，防止越权访问
- 定期轮换IAM角色密钥

### 4. 监控和日志
- 监控STS API调用频率和成功率
- 记录临时凭证使用情况
- 监控异常上传行为

## 故障排除

### 常见问题

1. **STS临时凭证功能未启用**
   - 检查配置文件中 `continew.admin.sts.enabled` 是否为 `true`

2. **存储平台不支持STS**
   - 确保存储类型为 OSS (S3兼容)
   - 检查存储配置是否正确

3. **凭证有效期超出范围**
   - 检查请求的 `durationSeconds` 是否在配置的最小和最大值范围内

4. **角色ARN配置错误**
   - 确认角色ARN格式正确
   - 检查角色是否存在和权限是否正确

5. **STS端点不可达**
   - 检查网络连通性
   - 确认STS服务端点配置正确

### 调试模式

启用调试日志：
```yaml
logging:
  level:
    top.continew.admin.system.service.impl.StsCredentialsServiceImpl: DEBUG
```

## 更新历史

- **v4.1.0**: 首次实现STS临时访问凭证功能
  - 支持AWS STS服务集成
  - 实现临时凭证生成和缓存
  - 提供完整的API接口
  - 支持权限策略限制