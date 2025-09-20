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

package top.continew.admin.system.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * STS 临时凭证响应
 *
 * @author Charles7c
 * @since 2025/01/01 10:00
 */
@Data
@Schema(description = "STS 临时凭证响应")
public class StsCredentialsResp {

    /**
     * 临时访问密钥ID
     */
    @Schema(description = "临时访问密钥ID")
    private String accessKeyId;

    /**
     * 临时访问密钥Secret
     */
    @Schema(description = "临时访问密钥Secret")
    private String secretAccessKey;

    /**
     * 会话令牌
     */
    @Schema(description = "会话令牌")
    private String sessionToken;

    /**
     * 存储端点
     */
    @Schema(description = "存储端点")
    private String endpoint;

    /**
     * 存储桶名称
     */
    @Schema(description = "存储桶名称")
    private String bucketName;

    /**
     * 区域
     */
    @Schema(description = "区域")
    private String region;

    /**
     * 凭证过期时间
     */
    @Schema(description = "凭证过期时间")
    private LocalDateTime expiration;

    /**
     * 允许的路径前缀
     */
    @Schema(description = "允许的路径前缀")
    private String pathPrefix;
}