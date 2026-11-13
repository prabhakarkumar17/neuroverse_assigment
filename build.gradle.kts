// ============================================================================
//  ROOT BUILD FILE
//  Declares which plugins the build may use, but applies none of them here.
//  `apply false` = "make this plugin available to sub-modules, don't run it now".
// ============================================================================
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.services) apply false
}
