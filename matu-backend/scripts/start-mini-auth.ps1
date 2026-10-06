param([string]$EnvFile = (Join-Path $PSScriptRoot '../../.env'))
$ErrorActionPreference = 'Stop'
$allowed = @('WECHAT_MINI_ENABLED','WECHAT_MINI_APP_ID','WECHAT_MINI_APP_SECRET','MINI_LOGIN_CONCURRENT')
if (Test-Path -LiteralPath $EnvFile) {
    foreach ($line in Get-Content -LiteralPath $EnvFile -Encoding UTF8) {
        if ($line -match '^\s*([A-Z][A-Z0-9_]*)\s*=(.*)$' -and $allowed -contains $Matches[1]) {
            $key = $Matches[1]; $value = $Matches[2].Trim()
            if (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'"))) { $value = $value.Substring(1,$value.Length-2) }
            [Environment]::SetEnvironmentVariable($key,$value,'Process')
        }
    }
}
$authJar = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../service/service-auth/target/service-auth-0.0.1-SNAPSHOT.jar'))
if (!(Test-Path -LiteralPath $authJar)) { throw '请先构建 service-auth 可执行 JAR，参见小程序 README。' }
if ($env:JAVA_HOME) { & (Join-Path $env:JAVA_HOME 'bin/java.exe') -jar $authJar } else { & java -jar $authJar }
exit $LASTEXITCODE
