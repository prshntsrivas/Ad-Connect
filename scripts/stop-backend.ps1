param([string[]]$Services)

$root = Split-Path -Parent $PSScriptRoot
$runDir = Join-Path $root ".run"
Get-ChildItem $runDir -Filter "*.pid" -ErrorAction SilentlyContinue | ForEach-Object {
    $svc = $_.BaseName
    if ($Services -and $Services -notcontains $svc) { return }
    $id = Get-Content $_.FullName
    if (Get-Process -Id $id -ErrorAction SilentlyContinue) {
        Stop-Process -Id $id -Force
        Write-Host "Stopped $svc (pid $id)"
    }
    Remove-Item $_.FullName
}
