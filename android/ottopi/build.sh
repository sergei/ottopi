# Do clean build
./gradlew clean

# Build debug and test application
./gradlew assembleDebug  || exit 1
./gradlew assembleDebugAndroidTest  || exit 1

VERSION_NAME=`cat app/build.gradle | grep versionName | awk '{print $(NF)}' | sed "s/\"//g"`
GIT_COMMIT=`git rev-parse --short HEAD`
APK_NAME=ottopi-${VERSION_NAME}-${GIT_COMMIT}.apk
cp app/build/outputs/apk/debug/app-debug.apk ${APK_NAME}
echo ${APK_NAME} created
