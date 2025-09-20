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

package top.continew.admin.system.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * STS 临时凭证请求
 *
 * @author Charles7c
 * @since 2025/01/01 10:00
 */
@Data
@Schema(description = "STS 临时凭证请求")
public class StsCredentialsReq {

    /**
     * 存储平台编码
     */
    @Schema(description = "存储平台编码", example = "oss-default")
    @NotBlank(message = "存储平台编码不能为空")
    private String storageCode;

    /**
     * 文件路径前缀，用于限制上传路径
     */
    @Schema(description = "文件路径前缀", example = "upload/files/")
    private String pathPrefix;

    /**
     * 临时凭证有效期(秒)，默认3600秒(1小时)
     */
    @Schema(description = "临时凭证有效期(秒)", example = "3600")
    private Integer durationSeconds = 3600;
}