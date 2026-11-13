# ============================================================================
#  R8 / ProGuard rules for the release build.
# ============================================================================
# Firebase Auth uses reflection on its model classes internally; the Firebase
# artifacts ship their own -keep rules, so nothing extra is needed here today.
# Add -keep rules here if a release build ever crashes where debug works.
