# ProGuard rules for 8 Ball Pool Mod Menu
# Anti-cheat bypass için obfuscation

-keep class com.poolmod.menu.** { *; }

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Obfuscation is enabled by default in ProGuard
# No need for -obfuscate flag (it's not a valid ProGuard option)

# Keep application class
-keep class com.poolmod.menu.PoolModApplication { *; }

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Keep game detection classes
-keep class com.poolmod.menu.GameDetector { *; }
-keep class com.poolmod.menu.GameLauncher { *; }

# Anti-cheat bypass classes
-keep class com.poolmod.menu.AntiCheatBypass { *; }

