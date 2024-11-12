# Restore module directory permissions

ui_print "- Restore module permissions"

# set_perm_recursive  <dirname>  <owner> <group> <dirpermission> <filepermission> <contexts> (default: u:object_r:system_file:s0)
set_perm_recursive    "$MODPATH" 0       0       0755            0644
