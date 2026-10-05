# Retrofit + Gson 模型类需保留泛型与字段名
-keepattributes Signature
-keepattributes *Annotation*

# Retrofit 接口方法上的注解需要保留
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# 保留 dto 包下的数据模型（Gson 反射依赖字段名）
# 注意：DeepSeek/GLM 的 DTO 在 data.remote.deepseek.dto / data.remote.glm.dto 下，
# 因此整体保留 data.remote 及其所有子包的类与字段名。
-keep class com.fenji.scoretrace.data.remote.** { *; }

# asset JSON（schools.json / majors_v2.json）的反序列化模型（Gson 反射依赖字段名）。
# 这些类没有 @SerializedName，一旦被 R8 混淆字段名，Gson 就填不进值，
# 会导致 TargetSchoolScreen 读 school.level/schools 时 NPE 闪退（release 专属）。
-keep class com.fenji.scoretrace.data.model.** { *; }

# OkHttp 平台相关可选项缺失时的警告忽略
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
