# Retrofit + Gson 模型类需保留泛型与字段名
-keepattributes Signature
-keepattributes *Annotation*

# Retrofit 接口方法上的注解需要保留
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# 保留 dto 包下的数据模型（Gson 反射依赖字段名）
-keep class com.fenji.scoretrace.data.remote.dto.** { *; }

# OkHttp 平台相关可选项缺失时的警告忽略
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
