# verify.ps1 — repo guard rails
# Windows PowerShell 5.1 compatible (no &&, no ternary, no ??)
param(
    [string]$DeshApkDir = ""
)

$ErrorActionPreference = "Continue"

Set-Location -LiteralPath $PSScriptRoot

try {
    Write-Host "============================================================"
    Write-Host "=== Hohe Repo Guard Verification (verify.ps1)"
    Write-Host "============================================================"

    # Step a: assembleDebug test ktlintCheck lint
    Write-Host ""
    Write-Host "=== Step a: assembleDebug assembleRelease test ktlintCheck lint ==="
    & .\gradlew.bat assembleDebug assembleRelease test ktlintCheck lint --console=plain
    $stepAExit = $LASTEXITCODE
    Write-Host "exit=$stepAExit"
    if ($stepAExit -ne 0) {
        Write-Host "VERIFY: FAIL (Gradle build/test/lint failed with exit code $stepAExit)"
        exit 1
    }

    # Step b: Permission allow-list
    Write-Host ""
    Write-Host "=== Step b: Permission allow-list ==="
    # AGP 8.x / 9.x merged manifest path: app\build\intermediates\merged_manifests\debug\processDebugManifest\AndroidManifest.xml
    $manifestCandidates = @(
        "app\build\intermediates\merged_manifests\debug\processDebugManifest\AndroidManifest.xml",
        "app\build\intermediates\merged_manifest\debug\processDebugMainManifest\AndroidManifest.xml",
        "app\build\intermediates\packaged_manifests\debug\processDebugManifestForPackage\AndroidManifest.xml"
    )
    $manifestPath = $null
    foreach ($candidate in $manifestCandidates) {
        if (Test-Path $candidate) {
            $manifestPath = $candidate
            break
        }
    }

    if (-not $manifestPath) {
        Write-Host "Merged AndroidManifest.xml not found in expected locations."
        Write-Host "exit=1"
        Write-Host "VERIFY: FAIL (merged manifest path changed; update verify.ps1)"
        exit 1
    }

    Write-Host "Reading manifest: $manifestPath"
    [xml]$manifestXml = Get-Content $manifestPath
    $nodes = $manifestXml.SelectNodes("//*[local-name()='uses-permission' or local-name()='uses-permission-sdk-23' or local-name()='uses-permission-sdk-m']")
    $permissions = New-Object System.Collections.Generic.List[string]
if ($nodes) {
    foreach ($node in $nodes) {
        $pName = $node.GetAttribute("android:name")
        if (-not $pName) {
            $pName = $node.GetAttribute("name")
        }
        if ($pName) {
            $permissions.Add($pName)
        }
    }
}

Write-Host "Permissions in merged manifest:"
foreach ($p in $permissions) {
    Write-Host "  - $p"
}

$allowedPermissions = @(
    "android.permission.VIBRATE",
    "android.permission.RECORD_AUDIO",
    "android.permission.INTERNET",
    "android.permission.ACCESS_NETWORK_STATE"
)

$offendingPermissions = New-Object System.Collections.Generic.List[string]
foreach ($p in $permissions) {
    $isAllowed = $false
    if ($allowedPermissions -contains $p) {
        $isAllowed = $true
    } elseif ($p -like "*.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION") {
        $isAllowed = $true
    }

    if (-not $isAllowed) {
        $offendingPermissions.Add($p)
    }
}

if ($offendingPermissions.Count -gt 0) {
    Write-Host "Offending permissions found: $($offendingPermissions -join ', ')"
    Write-Host "exit=1"
    Write-Host "VERIFY: FAIL (Offending permissions: $($offendingPermissions -join ', '))"
    exit 1
}
Write-Host "Permission allow-list check passed."
Write-Host "exit=0"

# Step c: Forbidden dependency scan
Write-Host ""
Write-Host "=== Step c: Forbidden dependency scan ==="
$depOutput = & .\gradlew.bat :app:dependencies --configuration debugRuntimeClasspath --console=plain 2>&1
$depExit = $LASTEXITCODE
Write-Host ($depOutput | Out-String)
Write-Host "exit=$depExit"
if ($depExit -ne 0) {
    Write-Host "VERIFY: FAIL (gradle dependencies failed with exit code $depExit)"
    exit 1
}

$forbiddenPattern = '(?i)(firebase|play-services-ads|play-services-measurement|facebook|airbridge|crashlytics|appsflyer|adjust|branch\.io|onesignal)'
$offendingDeps = New-Object System.Collections.Generic.List[string]
foreach ($line in $depOutput) {
    if ($line -match $forbiddenPattern) {
        $offendingDeps.Add($line.Trim())
    }
}

if ($offendingDeps.Count -gt 0) {
    Write-Host "Offending dependencies detected:"
    foreach ($dep in $offendingDeps) {
        Write-Host "  $dep"
    }
    Write-Host "exit=1"
    Write-Host "VERIFY: FAIL (Forbidden dependencies: $($offendingDeps -join '; '))"
    exit 1
}
Write-Host "Forbidden dependency scan passed."
Write-Host "exit=0"

# Step d: APK size check
Write-Host ""
Write-Host "=== Step d: APK size check ==="
# S1 (performance-benchmarks.md) budgets the shipped release download, so measure the minified
# release APK; the debug APK is only a fallback when no release build exists.
$apkPath = "app\build\outputs\apk\release\app-release-unsigned.apk"
if (-not (Test-Path $apkPath)) { $apkPath = "app\build\outputs\apk\debug\app-debug.apk" }
if (-not (Test-Path $apkPath)) {
    Write-Host "APK not found at $apkPath"
    Write-Host "exit=1"
    Write-Host "VERIFY: FAIL (APK file not found: $apkPath)"
    exit 1
}
$apkFile = Get-Item $apkPath
$apkSize = $apkFile.Length
$maxApkSize = 15728640
Write-Host ("APK size: {0} bytes ({1:N2} MB), limit: {2} bytes (15 MB)" -f $apkSize, ($apkSize / 1MB), $maxApkSize)
if ($apkSize -gt $maxApkSize) {
    Write-Host "exit=1"
    Write-Host "VERIFY: FAIL (APK size $apkSize bytes exceeds 15 MB limit)"
    exit 1
}
Write-Host "APK size check passed."
Write-Host "exit=0"

# Step e: Desh hash scan
Write-Host ""
Write-Host "=== Step e: Desh hash scan ==="
if ($DeshApkDir -and $DeshApkDir.Trim() -ne "") {
    $hashScanScript = "tools\desh-hash-scan.ps1"
    if (Test-Path $hashScanScript) {
        & powershell -NoProfile -ExecutionPolicy Bypass -File $hashScanScript -DeshApkDir $DeshApkDir -RepoDir . -Apk $apkPath
        $hsExit = $LASTEXITCODE
        Write-Host "exit=$hsExit"
        if ($hsExit -ne 0) {
            Write-Host "VERIFY: FAIL (desh-hash-scan failed with exit code $hsExit)"
            exit 1
        }
    } else {
        Write-Host "SKIPPED (TASK-004)"
        Write-Host "exit=0"
    }
} else {
    Write-Host "SKIPPED (no -DeshApkDir provided)"
    Write-Host "exit=0"
}

    # Step f: Final verdict
    Write-Host ""
    Write-Host "VERIFY: PASS"
    exit 0
} catch {
    Write-Host "exit=1"
    Write-Host "VERIFY: FAIL ($($_.Exception.Message))"
    exit 1
}
