# Agent APK

1. app/src/main/java/com/sys/service/Cfg.kt me HOST + TOKEN daalo
2. echo "sdk.dir=$HOME/sdk" > local.properties
3. ./build.sh
4. adb install -r out/agent.apk
5. adb shell dpm set-device-owner com.sys.service/.AdminReceiver

App dobara kholna: dialer me *#*#7788#*#*
