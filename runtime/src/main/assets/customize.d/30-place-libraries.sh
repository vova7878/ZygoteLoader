# Place Zygisk libraries

if [ "$ARCH" = "arm" ]; then
  ui_print "- Placing arm32 libraries"

  rm -rf "$MODPATH/zygisk/x86.so" "$MODPATH/zygisk/x86_64.so" "$MODPATH/zygisk/arm64-v8a.so"
  rm -rf "$MODPATH/lib/x86" "$MODPATH/lib/x86_64" "$MODPATH/lib/arm64-v8a"
fi

if [ "$ARCH" = "arm64" ]; then
  ui_print "- Placing arm64 libraries"

  rm -rf "$MODPATH/zygisk/x86.so" "$MODPATH/zygisk/x86_64.so"
  rm -rf "$MODPATH/lib/x86" "$MODPATH/lib/x86_64"
fi

if [ "$ARCH" = "x86" ]; then
  ui_print "- Placing x86 libraries"

  rm -rf "$MODPATH/zygisk/armeabi-v7a.so" "$MODPATH/zygisk/arm64-v8a.so" "$MODPATH/zygisk/x86_64.so"
  rm -rf "$MODPATH/lib/armeabi-v7a" "$MODPATH/lib/arm64-v8a" "$MODPATH/lib/x86_64"
fi

if [ "$ARCH" = "x64" ]; then
  ui_print "- Placing x86_64 libraries"

  rm -rf "$MODPATH/zygisk/armeabi-v7a.so" "$MODPATH/zygisk/arm64-v8a.so"
  rm -rf "$MODPATH/lib/armeabi-v7a" "$MODPATH/lib/arm64-v8a"
fi
