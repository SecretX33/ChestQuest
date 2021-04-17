-injars  build/libs/Chestquest.jar
-outjars build/libs/Chestquest-minified.jar

-libraryjar "C:\Program Files\Java\jdk1.8.0_281\jre\lib\rt.jar"
-libraryjar "D:\Local Disk\Users\User\Documents\GitHub\Chestquest\libs\spigot-1.12.2.jar"

-dontwarn net.minecraft.**
-dontwarn org.bukkit.**
-dontwarn com.google.**
-dontwarn com.comphenix.**
-dontwarn android.**
-dontwarn org.hibernate.**
-dontwarn com.sk89q.worldedit**
-dontwarn com.sk89q.worldguard**
-dontwarn net.milkbowl.vault.economy**
-keep class com.github.secretx33.dependencies.chestquest.hikari.metrics.**
-dontwarn com.codahale.metrics.**
-keep class com.codahale.metrics.**
-dontwarn **hikari.metrics**
-dontwarn javax.crypto.**
-dontwarn javassist.**
-dontwarn **slf4j**
-dontwarn io.micrometer.core.instrument.MeterRegistry
-dontwarn org.codehaus.mojo.**
-dontwarn **prometheus**
-keepnames class com.github.secretx33.dependencies.chestquest.kotlin.coroutines.**

# Don't rename SQLite classes
-keepnames class com.github.secretx33.dependencies.chestquest.sqlite.**

#-dontshrink
#-dontobfuscate
#-dontoptimize

# Keep your main class
-keep,allowobfuscation,allowoptimization class * extends org.bukkit.plugin.java.JavaPlugin { *; }

# Keep event handlers
-keep,allowobfuscation,allowoptimization class * extends org.bukkit.event.Listener {
    @org.bukkit.event.EventHandler <methods>;
}

# Keep serialization adapter classes from moshi
-keep,allowobfuscation class com.github.secretx33.chestquest.** {
    @com.github.secretx33.dependencies.chestquest.moshi.FromJson <methods>;
    @com.github.secretx33.dependencies.chestquest.moshi.ToJson <methods>;
}

# Keep main package name (spigot forum rule)
-keeppackagenames "com.github.secretx33.chestquest"

# Keep public enum names
-keepclassmembers,allowoptimization public enum com.github.secretx33.chestquest.** {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep all ProtocolLib packet listeners (this was rough to get working, don't turn on optimization, it ALWAYS breaks the sensible ProtocolLib)
-keepclassmembers class com.github.secretx33.chestquest.**  {
    void onPacketSending(com.comphenix.protocol.events.PacketEvent);
    void onPacketReceiving(com.comphenix.protocol.events.PacketEvent);
}

# Keep static fields in custom Events
-keepclassmembers,allowoptimization class com.github.secretx33.chestquest.** extends org.bukkit.event.Event {
    @com.github.secretx33.dependencies.chestquest.kotlin.jvm.JvmStatic <fields>;
    public static final <fields>;
    @com.github.secretx33.dependencies.chestquest.kotlin.jvm.JvmStatic <methods>;
    public static <methods>;
}

# Remove dependencies obsfuscation to remove bugs factor
#-keep,allowshrinking class com.github.secretx33.dependencies.** { *; }

# If your goal is obfuscating and making things harder to read, repackage your classes with this rule
-repackageclasses com.github.secretx33.chestquest
-allowaccessmodification
-mergeinterfacesaggressively
-adaptresourcefilecontents **.yml,META-INF/MANIFEST.MF

# Some attributes that you'll need to keep (if I remove *Annotation* Koin dies)
-keepattributes Exceptions,Signature,SourceFile,LineNumberTable,*Annotation*,EnclosingMethod
#-keepattributes Exceptions,Signature,Deprecated,LineNumberTable,*Annotation*,EnclosingMethod
#-keepattributes LocalVariableTable,LocalVariableTypeTable,Exceptions,InnerClasses,Signature,Deprecated,LineNumberTable,*Annotation*,EnclosingMethod
