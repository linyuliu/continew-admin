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

package top.continew.admin.system.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * STS 配置属性
 *
 * @author Charles7c
 * @since 2025/01/01 10:00
 */
@Data
@Component
@ConfigurationProperties(prefix = "continew.admin.sts")
public class StsProperties {

    /**
     * 是否启用STS功能
     */
    private boolean enabled = true;

    /**
     * 默认凭证有效期(秒)
     */
    private int defaultDurationSeconds = 3600;

    /**
     * 最大凭证有效期(秒)
     */
    private int maxDurationSeconds = 7200;

    /**
     * 最小凭证有效期(秒)
     */
    private int minDurationSeconds = 900;

    /**
     * 缓存过期时间(秒)，应该小于最小凭证有效期
     */
    private int cacheExpireSeconds = 600;

    /**
     * 角色ARN配置
     */
    private String roleArn = "arn:aws:iam::123456789012:role/S3TempRole";

    /**
     * 是否严格模式，严格模式下只允许上传到指定前缀路径
     */
    private boolean strictMode = true;
}