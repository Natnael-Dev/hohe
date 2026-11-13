# desh-hash-scan.ps1 — ensures no proprietary files from original Desh APK enter repo or APK
# Windows PowerShell 5.1 compatible (no &&, no ternary, no ??)
param(
    [Parameter(Mandatory = $true)][string]$DeshApkDir,
    [string]$RepoDir = ".",
    [string]$Apk = "app\build\outputs\apk\debug\app-debug.apk"
)

$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.IO.Compression.FileSystem

Write-Host "============================================================"
Write-Host "=== Desh Proprietary File Hash Scan (desh-hash-scan.ps1)"
Write-Host "============================================================"
Write-Host "Desh APK Directory: $DeshApkDir"
Write-Host "Repository Directory: $RepoDir"
Write-Host "Output APK: $Apk"

if (-not (Test-Path $DeshApkDir)) {
    Write-Host "ERROR: Desh APK directory not found: $DeshApkDir"
    exit 1
}

$resolvedApk = Join-Path $RepoDir $Apk
if (-not (Test-Path $resolvedApk)) {
    if (Test-Path $Apk) {
        $resolvedApk = $Apk
    } else {
        Write-Host "ERROR: Output APK not found: $Apk"
        exit 1
    }
}

# Documented allow-list of trivially common upstream entries
# Reason for each category:
# 1. Zero-length entries: empty directories or zero-byte placeholders contain no copyrighted data.
# 2. META-INF/MANIFEST.MF and signing blocks (*.SF, *.RSA, *.DSA, *.EC): standard JVM/Android packaging metadata.
# 3. META-INF/*.version: standard Google/AndroidX library version markers (e.g. androidx.core_core.version) packaged by upstream AARs.
# 4. META-INF/*LICENSE* and *NOTICE*: standard Apache 2.0 / open-source license text files bundled inside upstream dependencies.
# 5. META-INF/*.kotlin_module: standard Kotlin compiler module metadata.
# 6. DebugProbesKt.bin: standard Kotlin coroutines debug metadata packaged by org.jetbrains.kotlinx:kotlinx-coroutines-core.
# 7. lib/*/libandroidx.*.so: prebuilt upstream shared libraries distributed by Google in AndroidX AARs (e.g. graphics-path).
function Test-AllowList([string]$entryName, [long]$length) {
    if ($length -eq 0) {
        return $true
    }
    $norm = $entryName.Replace('\', '/')
    if ($norm -match '(?i)^META-INF/(MANIFEST\.MF|[^/]+\.(SF|RSA|DSA|EC))$') {
        return $true
    }
    if ($norm -match '(?i)^META-INF/.*\.version$') {
        return $true
    }
    if ($norm -match '(?i)^META-INF/.*(LICENSE|NOTICE).*$') {
        return $true
    }
    if ($norm -match '(?i)^META-INF/.*\.kotlin_module$') {
        return $true
    }
    if ($norm -ieq "DebugProbesKt.bin") {
        return $true
    }
    if ($norm -match '(?i)^lib/[^/]+/libandroidx\..*\.so$') {
        return $true
    }
    return $false
}

function Get-StreamSha256($stream) {
    $sha256 = [System.Security.Cryptography.SHA256]::Create()
    try {
        $hashBytes = $sha256.ComputeHash($stream)
        return [System.BitConverter]::ToString($hashBytes).Replace('-', '').ToLower()
    }
    finally {
        $sha256.Dispose()
    }
}

function Get-FileSha256($filePath) {
    $fileStream = [System.IO.File]::OpenRead($filePath)
    try {
        return Get-StreamSha256($fileStream)
    }
    finally {
        $fileStream.Dispose()
    }
}

# Step 1: Compute hashes of all entries inside Desh APKs in memory (never extracting to disk)
Write-Host ""
Write-Host "--- Scanning Desh APKs in memory ---"
$deshApkFiles = Get-ChildItem -Path $DeshApkDir -Filter "*.apk"
if ($deshApkFiles.Count -eq 0) {
    Write-Host "ERROR: No APK files found in $DeshApkDir"
    exit 1
}

$deshHashes = @{} # hash -> "apkName:entryName"
$deshTotalEntries = 0
$deshIgnoredEntries = 0

foreach ($apkFile in $deshApkFiles) {
    $zip = [System.IO.Compression.ZipFile]::OpenRead($apkFile.FullName)
    try {
        foreach ($entry in $zip.Entries) {
            $deshTotalEntries++
            if (Test-AllowList $entry.FullName $entry.Length) {
                $deshIgnoredEntries++
                continue
            }
            $entryStream = $entry.Open()
            try {
                $hash = Get-StreamSha256($entryStream)
                if (-not $deshHashes.ContainsKey($hash)) {
                    $deshHashes[$hash] = ($apkFile.Name + ":" + $entry.FullName)
                }
            }
            finally {
                $entryStream.Dispose()
            }
        }
    }
    finally {
        $zip.Dispose()
    }
}

Write-Host "Desh APKs examined: $($deshApkFiles.Count)"
Write-Host "Total Desh entries inspected: $deshTotalEntries"
Write-Host "Allow-listed/ignored Desh entries: $deshIgnoredEntries"
Write-Host "Unique Desh entry hashes indexed: $($deshHashes.Count)"

# Step 2: Scan tracked repo files
Write-Host ""
Write-Host "--- Scanning tracked files in repository ---"
$trackedFiles = & git -C $RepoDir ls-files
$repoTotalFiles = 0
$repoMatches = New-Object System.Collections.Generic.List[string]

foreach ($relPath in $trackedFiles) {
    $fullPath = Join-Path $RepoDir $relPath
    if (-not (Test-Path $fullPath -PathType Leaf)) {
        continue
    }
    $fileInfo = Get-Item $fullPath
    $repoTotalFiles++
    if (Test-AllowList $relPath $fileInfo.Length) {
        continue
    }
    $hash = Get-FileSha256($fullPath)
    if ($deshHashes.ContainsKey($hash)) {
        $matchDesc = "REPO FILE '$relPath' matches Desh entry '$($deshHashes[$hash])' (SHA256: $hash)"
        $repoMatches.Add($matchDesc)
    }
}
Write-Host "Repo tracked files scanned: $repoTotalFiles"

# Step 3: Scan output APK entries in memory
Write-Host ""
Write-Host "--- Scanning output APK entries in memory ---"
$apkTotalEntries = 0
$apkMatches = New-Object System.Collections.Generic.List[string]

$apkZip = [System.IO.Compression.ZipFile]::OpenRead($resolvedApk)
try {
    foreach ($entry in $apkZip.Entries) {
        $apkTotalEntries++
        if (Test-AllowList $entry.FullName $entry.Length) {
            continue
        }
        $entryStream = $entry.Open()
        try {
            $hash = Get-StreamSha256($entryStream)
            if ($deshHashes.ContainsKey($hash)) {
                $matchDesc = "APK ENTRY '$($entry.FullName)' matches Desh entry '$($deshHashes[$hash])' (SHA256: $hash)"
                $apkMatches.Add($matchDesc)
            }
        }
        finally {
            $entryStream.Dispose()
        }
    }
}
finally {
    $apkZip.Dispose()
}
Write-Host "Output APK entries scanned: $apkTotalEntries"

# Step 4: Report verdict
Write-Host ""
Write-Host "--- Hash Scan Summary ---"
$totalMatches = $repoMatches.Count + $apkMatches.Count
Write-Host "Desh entries indexed: $($deshHashes.Count)"
Write-Host "Repo tracked files scanned: $repoTotalFiles"
Write-Host "APK entries scanned: $apkTotalEntries"

if ($totalMatches -gt 0) {
    Write-Host ""
    Write-Host "PROPRIETARY MATCHES DETECTED ($totalMatches):"
    foreach ($m in $repoMatches) {
        Write-Host "  [REPO MATCH] $m"
    }
    foreach ($m in $apkMatches) {
        Write-Host "  [APK MATCH]  $m"
    }
    Write-Host ""
    Write-Host "MATCHES: $totalMatches"
    exit 1
}

Write-Host "MATCHES: 0"
exit 0
