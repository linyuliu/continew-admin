/*
 * Copyright (c) 2022-present Charles7c Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package top.continew.admin.system.service.impl;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;
import software.amazon.awssdk.services.sts.model.Credentials;
import top.continew.admin.system.config.StsProperties;
import top.continew.admin.system.enums.StorageTypeEnum;
import top.continew.admin.system.model.entity.StorageDO;
import top.continew.admin.system.model.req.StsCredentialsReq;
import top.continew.admin.system.model.resp.StsCredentialsResp;
import top.continew.admin.system.service.StorageService;
import top.continew.admin.system.service.StsCredentialsService;
import top.continew.starter.core.exception.BaseException;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.ConcurrentHashMap;

/**
 * STS 临时凭证服务实现
 *
 * @author Charles7c
 * @since 2025/01/01 10:00
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StsCredentialsServiceImpl implements StsCredentialsService {

    private final StorageService storageService;
    private final StsProperties stsProperties;
    private final ConcurrentHashMap<String, StsClient> STS_CLIENT_CACHE = new ConcurrentHashMap<>();

    @Override
    @Cacheable(value = "sts_credentials", key = "#req.storageCode + '_' + #req.pathPrefix", condition = "#req.durationSeconds >= 900", unless = "#result == null")
    public StsCredentialsResp getStsCredentials(StsCredentialsReq req) {
        // 检查STS功能是否启用
        if (!stsProperties.isEnabled()) {
            throw new BaseException("STS临时凭证功能未启用");
        }

        String storageCode = req.getStorageCode();
        StorageDO storage = storageService.getByCode(storageCode);

        if (!StorageTypeEnum.OSS.equals(storage.getType())) {
            throw new BaseException("存储平台不支持STS临时凭证功能");
        }

        // 验证凭证有效期
        int durationSeconds = req.getDurationSeconds();
        if (durationSeconds < stsProperties.getMinDurationSeconds() || durationSeconds > stsProperties
            .getMaxDurationSeconds()) {
            throw new BaseException(String.format("凭证有效期必须在 %d-%d 秒之间", stsProperties
                .getMinDurationSeconds(), stsProperties.getMaxDurationSeconds()));
        }

        try {
            StsClient stsClient = getStsClient(storage);

            // 构建假设角色请求
            AssumeRoleRequest assumeRoleRequest = AssumeRoleRequest.builder()
                .roleArn(getRoleArn(storage))
                .roleSessionName("continew-admin-" + System.currentTimeMillis())
                .durationSeconds(durationSeconds)
                .policy(buildPolicy(storage.getBucketName(), req.getPathPrefix()))
                .build();

            AssumeRoleResponse response = stsClient.assumeRole(assumeRoleRequest);
            Credentials credentials = response.credentials();

            // 构建响应
            StsCredentialsResp result = new StsCredentialsResp();
            result.setAccessKeyId(credentials.accessKeyId());
            result.setSecretAccessKey(credentials.secretAccessKey());
            result.setSessionToken(credentials.sessionToken());
            result.setEndpoint(storage.getEndpoint());
            result.setBucketName(storage.getBucketName());
            result.setRegion("us-east-1"); // 默认区域
            result.setExpiration(LocalDateTime.ofInstant(credentials.expiration(), ZoneId.systemDefault()));
            result.setPathPrefix(StrUtil.blankToDefault(req.getPathPrefix(), ""));

            log.info("生成STS临时凭证成功: storageCode={}, pathPrefix={}, expiration={}", storageCode, req
                .getPathPrefix(), result.getExpiration());

            return result;
        } catch (Exception e) {
            log.error("生成STS临时凭证失败: storageCode={}, error={}", storageCode, e.getMessage(), e);
            throw new BaseException("生成STS临时凭证失败: " + e.getMessage(), e);
        }
    }

    @Override
    @CacheEvict(value = "sts_credentials", key = "#storageCode + '_*'")
    public void refreshStsCredentials(String storageCode) {
        log.info("刷新STS临时凭证缓存: storageCode={}", storageCode);
        // 移除对应的STS客户端缓存
        STS_CLIENT_CACHE.entrySet().removeIf(entry -> entry.getKey().startsWith(storageCode));
    }

    /**
     * 获取STS客户端
     */
    private StsClient getStsClient(StorageDO storage) {
        String key = storage.getCode() + "|" + storage.getAccessKey();
        return STS_CLIENT_CACHE.computeIfAbsent(key, k -> {
            StaticCredentialsProvider auth = StaticCredentialsProvider.create(AwsBasicCredentials.create(storage
                .getAccessKey(), storage.getSecretKey()));

            var builder = StsClient.builder().credentialsProvider(auth).region(Region.US_EAST_1);

            // 如果是自定义端点，配置STS端点
            if (!storage.getEndpoint().contains("amazonaws.com")) {
                // 对于兼容S3的存储服务，通常STS端点与S3端点相同或有特定格式
                String stsEndpoint = storage.getEndpoint().replace("s3.", "sts.");
                builder.endpointOverride(URI.create(stsEndpoint));
            }

            return builder.build();
        });
    }

    /**
     * 获取角色ARN
     */
    private String getRoleArn(StorageDO storage) {
        // 优先使用配置的角色ARN，后续可以支持存储配置中的自定义角色ARN
        return stsProperties.getRoleArn();
    }

    /**
     * 构建角色ARN（已弃用，使用getRoleArn替代）
     * 对于AWS，格式为: arn:aws:iam::account-id:role/role-name
     * 对于其他兼容S3的服务，可能有不同的格式
     */
    @Deprecated
    private String buildRoleArn(StorageDO storage) {
        return getRoleArn(storage);
    }

    /**
     * 构建IAM策略，限制临时凭证的权限
     */
    private String buildPolicy(String bucketName, String pathPrefix) {
        String prefix = StrUtil.blankToDefault(pathPrefix, "");
        if (StrUtil.isNotBlank(prefix) && !prefix.endsWith("/")) {
            prefix += "/";
        }

        return String.format("""
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
                        "Resource": "arn:aws:s3:::%s/%s*"
                    },
                    {
                        "Effect": "Allow",
                        "Action": [
                            "s3:ListBucket"
                        ],
                        "Resource": "arn:aws:s3:::%s",
                        "Condition": {
                            "StringLike": {
                                "s3:prefix": "%s*"
                            }
                        }
                    }
                ]
            }
            """, bucketName, prefix, bucketName, prefix);
    }
}