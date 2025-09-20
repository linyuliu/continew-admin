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

package top.continew.admin.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.x.file.storage.core.FileStorageProperties;
import org.springframework.web.bind.annotation.*;
import top.continew.admin.system.model.req.StsCredentialsReq;
import top.continew.admin.system.model.resp.StsCredentialsResp;
import top.continew.admin.system.service.StsCredentialsService;

/**
 * STS 临时凭证管理 API
 *
 * @author Charles7c
 * @since 2025/01/01 10:00
 */
@Tag(name = "STS 临时凭证管理 API")
@RestController
@RequestMapping("/system/sts")
@RequiredArgsConstructor
public class StsCredentialsController {

    private final StsCredentialsService stsCredentialsService;

    /**
     * 获取 STS 临时凭证
     *
     * @param req 请求参数
     * @return STS 临时凭证
     */
    @Operation(summary = "获取 STS 临时凭证", description = "获取用于客户端直接上传的临时访问凭证")
    @SaCheckPermission("system:storage:getStsCredentials")
    @PostMapping("/credentials")
    public StsCredentialsResp getStsCredentials(@RequestBody @Valid StsCredentialsReq req) {
        return stsCredentialsService.getStsCredentials(req);
    }

    /**
     * 获取 X File Storage 配置（含STS临时凭证）
     *
     * @param req 请求参数
     * @return X File Storage AmazonS3Config配置
     */
    @Operation(summary = "获取 X File Storage 配置", description = "获取包含STS临时凭证的X File Storage配置，用于集成X File Storage框架")
    @SaCheckPermission("system:storage:getStsCredentials")
    @PostMapping("/x-file-storage-config")
    public FileStorageProperties.AmazonS3Config getStsAmazonS3Config(@RequestBody @Valid StsCredentialsReq req) {
        return stsCredentialsService.createStsAmazonS3Config(req);
    }

    /**
     * 刷新 STS 临时凭证缓存
     *
     * @param storageCode 存储平台编码
     */
    @Operation(summary = "刷新 STS 临时凭证缓存", description = "清除指定存储平台的临时凭证缓存")
    @Parameter(name = "storageCode", description = "存储平台编码", example = "oss-default", in = ParameterIn.PATH)
    @SaCheckPermission("system:storage:refreshStsCredentials")
    @PostMapping("/refresh/{storageCode}")
    public void refreshStsCredentials(@PathVariable("storageCode") String storageCode) {
        stsCredentialsService.refreshStsCredentials(storageCode);
    }
}