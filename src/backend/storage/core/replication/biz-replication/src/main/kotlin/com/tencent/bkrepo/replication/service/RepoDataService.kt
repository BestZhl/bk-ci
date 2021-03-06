/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.  
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package com.tencent.bkrepo.replication.service

import com.tencent.bkrepo.auth.api.ServicePermissionResource
import com.tencent.bkrepo.auth.api.ServiceRoleResource
import com.tencent.bkrepo.auth.api.ServiceUserResource
import com.tencent.bkrepo.auth.pojo.Permission
import com.tencent.bkrepo.auth.pojo.Role
import com.tencent.bkrepo.auth.pojo.User
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.auth.pojo.enums.RoleType
import com.tencent.bkrepo.common.api.constant.StringPool.ROOT
import com.tencent.bkrepo.common.artifact.stream.Range
import com.tencent.bkrepo.common.storage.core.StorageService
import com.tencent.bkrepo.replication.message.ReplicationException
import com.tencent.bkrepo.repository.api.MetadataClient
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.api.ProjectClient
import com.tencent.bkrepo.repository.api.RepositoryClient
import com.tencent.bkrepo.repository.pojo.node.NodeInfo
import com.tencent.bkrepo.repository.pojo.project.ProjectInfo
import com.tencent.bkrepo.repository.pojo.repo.RepositoryInfo
import org.springframework.stereotype.Service
import java.io.InputStream

@Service
class RepoDataService(
    private val projectClient: ProjectClient,
    private val repositoryClient: RepositoryClient,
    private val nodeClient: NodeClient,
    private val metadataClient: MetadataClient,
    private val permissionResource: ServicePermissionResource,
    private val roleResource: ServiceRoleResource,
    private val userResource: ServiceUserResource,
    private val storageService: StorageService
) {

    fun listProject(projectId: String? = null): List<ProjectInfo> {
        return if (projectId == null) {
            projectClient.list().data!!
        } else {
            listOf(projectClient.query(projectId).data!!)
        }
    }

    fun listRepository(projectId: String, repoName: String? = null): List<RepositoryInfo> {
        return if (repoName == null) {
            repositoryClient.list(projectId).data!!
        } else {
            listOf(repositoryClient.detail(projectId, repoName).data!!)
        }
    }

    fun getRepositoryDetail(projectId: String, repoName: String): RepositoryInfo? {
        return repositoryClient.detail(projectId, repoName).data
    }

    fun countFileNode(repositoryInfo: RepositoryInfo): Long {
        return nodeClient.countFileNode(repositoryInfo.projectId, repositoryInfo.name, ROOT).data!!
    }

    fun listFileNode(projectId: String, repoName: String, path: String = ROOT, page: Int = 0, size: Int = 100): List<NodeInfo> {
        return nodeClient.page(projectId, repoName, page, size, path, includeFolder = false, includeMetadata = true, deep = true).data!!.records
    }

    fun getMetadata(nodeInfo: NodeInfo): Map<String, String> {
        return metadataClient.query(nodeInfo.projectId, nodeInfo.repoName, nodeInfo.fullPath).data!!
    }

    fun getFile(sha256: String, length: Long, repoInfo: RepositoryInfo): InputStream {
        return storageService.load(sha256, Range.ofFull(length), repoInfo.storageCredentials)
            ?: throw ReplicationException("File data does not exist")
    }

    fun listRole(projectId: String, repoName: String?): List<Role> {
        val roleType = if (repoName == null) RoleType.PROJECT else RoleType.REPO
        return roleResource.listRole(roleType, projectId, repoName).data!!
    }

    fun listUser(roleIdList: List<String>): List<User> {
        return userResource.listUser(roleIdList).data!!
    }

    fun listPermission(resourceType: ResourceType, projectId: String, repoName: String?): List<Permission> {
        return permissionResource.listPermission(resourceType, projectId, repoName).data!!
    }

    fun getUserDetail(uid: String): User? {
        return userResource.detail(uid).data
    }
}
