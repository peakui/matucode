# 不声明高级脚本参数，否则 Flutter 的 -d 会被 PowerShell 当作 -Debug 消费。
# 使用原始参数数组完整转发设备、构建模式和 dart-define 等选项。
$FlutterArguments = @($args)
$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$workspaceSdk = [IO.Path]::GetFullPath((Join-Path $projectRoot '../.tools/flutter-3.44.0/bin/flutter.bat'))
$flutterCommand = if (Test-Path -LiteralPath $workspaceSdk) { $workspaceSdk } else { (Get-Command flutter -ErrorAction Stop).Source }
# 移动端任务无需创建 Windows/Linux 桌面插件符号链接，仅影响当前进程。
$savedWindows = $env:FLUTTER_WINDOWS
$savedLinux = $env:FLUTTER_LINUX
try {
  $env:FLUTTER_WINDOWS = 'false'
  $env:FLUTTER_LINUX = 'false'
  Push-Location $projectRoot
  try { & $flutterCommand @FlutterArguments; $commandExitCode = $LASTEXITCODE }
  finally { Pop-Location }
} finally {
  $env:FLUTTER_WINDOWS = $savedWindows
  $env:FLUTTER_LINUX = $savedLinux
}
exit $commandExitCode
