# temi SDK 는 AIDL/리플렉션을 사용하므로 난독화 시 제외가 필요할 수 있다. (확인 필요)
-keep class com.robotemi.sdk.** { *; }
