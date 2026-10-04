package com.fenji.scoretrace.data.remote.dto

import com.fenji.scoretrace.data.local.entity.Subject
import com.google.gson.annotations.SerializedName

data class SubjectDto(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("color") val color: Long,
)

fun SubjectDto.toEntity(): Subject = Subject(id = id, name = name, color = color)
