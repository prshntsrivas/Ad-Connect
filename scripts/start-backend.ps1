<#
.SYNOPSIS
  Starts Ad-Connect Spring Boot services locally as background java processes.
.EXAMPLE
  ./scripts/start-backend.ps1                     # all services
  ./scripts/start-backend.ps1 -Services auth-service,campaign-service
  ./scripts/start-backend.ps1 -Build              # mvn package first
#>
param(
    [string[]]$Services = @(
        "discovery-server", "api-gateway", "auth-service", "campaign-service",
        "search-service", "chat-service", "payment-service", "notification-service"),
    [switch]$Build,
    [string]$Heap = "256m"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$backend = Join-Path $root "backend"
$runDir = Join-Path $root ".run"
$logDir = Join-Path $runDir "logs"
New-Item -ItemType Directory -Force -Path $logDir | Out-Null

$envFile = Join-Path $root ".env"
if (Test-Path $envFile) {
    Get-Content $envFile | Where-Object { $_ -match '^\s*[^#].*=' } | ForEach-Object {
        $name, $value = $_ -split '=', 2
        [Environment]::SetEnvironmentVariable($name.Trim(), $value.Trim(), "Process")
    }
}

if ($Build) {
    $mvn = if (Get-Command mvn -ErrorAction SilentlyContinue) { "mvn" } else { "$env:USERPROFILE\tools\apache-maven-3.9.9\bin\mvn.cmd" }
    & $mvn -q -f (Join-Path $backend "pom.xml") package -DskipTests
    if ($LASTEXITCODE -ne 0) { throw "Maven build failed" }
}

function Wait-Http($url, $seconds) {
    $deadline = (Get-Date).AddSeconds($seconds)
    while ((Get-Date) -lt $deadline) {
        try { Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 2 | Out-Null; return $true } catch { Start-Sleep 2 }
    }
    return $false
}

foreach ($svc in $Services) {
    $pidFile = Join-Path $runDir "$svc.pid"
    if (Test-Path $pidFile) {
        $old = Get-Content $pidFile
        if (Get-Process -Id $old -ErrorAction SilentlyContinue) { Write-Host "$svc already running (pid $old)"; continue }
    }
    $jar = Get-ChildItem (Join-Path $backend "$svc\target") -Filter "$svc-*.jar" -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notlike "*.original" } | Select-Object -First 1
    if (-not $jar) { throw "No jar for $svc; run with -Build" }
    $log = Join-Path $logDir "$svc.log"
    $proc = Start-Process java -ArgumentList "-Xms64m", "-Xmx$Heap", "-XX:+UseSerialGC", "-XX:TieredStopAtLevel=1",
        "-jar", $jar.FullName -RedirectStandardOutput $log -RedirectStandardError "$log.err" -WindowStyle Hidden -PassThru
    Set-Content $pidFile $proc.Id
    Write-Host "Started $svc (pid $($proc.Id)) -> $log"
    if ($svc -eq "discovery-server") {
        if (-not (Wait-Http "http://localhost:8761/actuator/health" 90)) { throw "Eureka did not start" }
    }
}
