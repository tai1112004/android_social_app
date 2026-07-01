$ErrorActionPreference = 'Stop'

$adb = Get-Command adb -ErrorAction SilentlyContinue
if (-not $adb) {
    Write-Host 'adb not found in PATH.' -ForegroundColor Red
    exit 1
}

$lines = & adb devices | Select-Object -Skip 1
$serials = @()
foreach ($line in $lines) {
    if ([string]::IsNullOrWhiteSpace($line)) { continue }
    $parts = $line -split '\s+'
    if ($parts.Length -ge 2 -and $parts[1] -eq 'device') {
        $serials += $parts[0]
    }
}

if ($serials.Count -eq 0) {
    Write-Host 'No connected devices in device state.' -ForegroundColor Yellow
    exit 0
}

foreach ($serial in $serials) {
    Write-Host "Applying adb reverse for $serial ..."
    & adb -s $serial reverse tcp:8082 tcp:8082
}

Write-Host 'Done. Re-run this script any time a device is unplugged/replugged.' -ForegroundColor Green
