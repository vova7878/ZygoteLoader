# Enforce device abi
# Supported abis: arm64-v8a armeabi-v7a x86_64 x86

[ "$ARCH" != "arm" ] && [ "$ARCH" != "arm64" ] && [ "$ARCH" != "x86" ] && [ "$ARCH" != "x64" ] && abort "! Unsupported abi: $ARCH"

ui_print "- Device abi: $ARCH"
