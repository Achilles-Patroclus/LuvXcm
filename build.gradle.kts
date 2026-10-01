// 根构建脚本：只声明插件版本，具体在 :app 模块中应用
// 注意：AGP 9 起内置 Kotlin 支持，org.jetbrains.kotlin.android 必须移除（应用会直接报错）
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}
