package com.fenji.scoretrace.data.remote

import com.fenji.scoretrace.data.remote.dto.BaseResponse
import com.fenji.scoretrace.data.remote.dto.IpResponse
import com.fenji.scoretrace.data.remote.dto.SubjectDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Url

/**
 * 服务端接口占位定义，后续按实际后端补全。
 * baseUrl 在 NetworkModule 中配置。
 */
interface ApiService {

    @GET("subjects")
    suspend fun getSubjects(): BaseResponse<List<SubjectDto>>

    /**
     * 获取本机公网 IP。
     * 目标地址与 baseUrl 不同域，因此用 [Url] 传完整 URL（@GET 只接受相对路径）。
     */
    @GET
    suspend fun getUserIp(@Url url: String): IpResponse

    @GET("subjects/{id}")
    suspend fun getSubject(@Path("id") id: Long): BaseResponse<SubjectDto>
}
