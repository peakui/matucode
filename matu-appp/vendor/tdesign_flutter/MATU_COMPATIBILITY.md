# Flutter 3.44 compatibility

Source: tdesign_flutter 0.2.7 from pub.dev (Tencent TDesign, original LICENSE retained).

Flutter 3.44 makes IconData final. This local copy preserves the package's lib, assets and pubspec, except for the generated icon registry. `tool/generate_tdesign_icons.ps1` reads the original 0.2.7 registry and emits equivalent named IconData constants and the same name-to-icon map. It avoids inheritance; names remain available through `TDIcons.all.keys`. No application code relies on the former private icon subtype's `name` property.

Regenerate from the unmodified pub-cache package with the script's `-Source` argument. Do not edit generated constants or the shared pub cache. Remove this override once an upstream release supports the chosen Flutter SDK and passes the application tests.
