# Enforce magisk version
# Supported version: 24000+

if [ "$BOOTMODE" ] && [ "$KSU" ]; then
  ui_print "- Installing from KernelSU app"
  ui_print "- KernelSU version: $KSU_KERNEL_VER_CODE (kernel) + $KSU_VER_CODE (ksud)"
elif [ "$BOOTMODE" ] && [ "$APATCH" ]; then
  ui_print "- Installing from APatch app"
  ui_print "- APatch version: $APATCH_VER_CODE"
elif [ "$BOOTMODE" ] && [ "$MAGISK_VER_CODE" ]; then
  ui_print "- Installing from Magisk app"
  ui_print "- Magisk version: $MAGISK_VER_CODE"

  [ "$MAGISK_VER_CODE" -lt 24000 ] && abort "! Unsupported magisk version: $MAGISK_VER_CODE (24000+ required)"
else
  ui_print "*********************************************************"
  ui_print "! Install from recovery is not supported"
  ui_print "! Please install from KernelSU, APatch, or Magisk app"
  abort    "*********************************************************"
fi
