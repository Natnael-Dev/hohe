# Proguard rules for app module
# No reflection or serialization is used. Strip debug/verbose logging from release (application-architecture section 6).
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
