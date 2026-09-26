package com.fenji.scorcetrace.data.local

import androidx.room.TypeConverter
import java.util.Date

/** Room 类型转换：实体统一用 java.util.Date，落库为 epoch 毫秒（Long）。 */
class Converters {

    @TypeConverter
    fun fromDate(value: Date?): Long? = value?.time

    @TypeConverter
    fun toDate(value: Long?): Date? = value?.let { Date(it) }
}
