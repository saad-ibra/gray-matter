# Gray Matter Build Script
# Automatically configures JAVA_HOME, local.properties, and runs the build

$ErrorActionPreference = "Stop"

Write-Host "Setting up build environment..." -ForegroundColor Cyan

# 1. Locate Local JDK
$localJdkPath = Join-Path $PSScriptRoot ".gemini\jdk"
if (Test-Path $localJdkPath) {
    $jdkDir = Get-ChildItem $localJdkPath | Select-Object -ExpandProperty Name -First 1
    if ($jdkDir) {
        $fullJdkPath = Join-Path $localJdkPath $jdkDir
        $env:JAVA_HOME = $fullJdkPath
        Write-Host "Found local JDK. Setting JAVA_HOME to: $fullJdkPath" -ForegroundColor Green
        $env:Path = "$fullJdkPath\bin;$env:Path"
    }
}

# 2. Configure Android SDK (local.properties)
$localPropsPath = Join-Path $PSScriptRoot "local.properties"
if (!(Test-Path $localPropsPath)) {
    Write-Host "Searching for Android SDK..." -ForegroundColor Yellow
    $possibleSdkPaths = @(
        "$env:LOCALAPPDATA\Android\Sdk",
        "C:\Android\Sdk",
        "$env:ProgramFiles\Android\Android Studio\sdks"
    )
    
    $sdkPath = $null
    foreach ($path in $possibleSdkPaths) {
        if (Test-Path $path) {
            $sdkPath = $path
            break
        }
    }
    
    if ($sdkPath) {
        Write-Host "Found Android SDK at: $sdkPath" -ForegroundColor Green
        # Escape backslashes for properties file
        $escapedSdkPath = $sdkPath -replace "\\", "\\"
        "sdk.dir=$escapedSdkPath" | Out-File -FilePath $localPropsPath -Encoding ASCII
        Write-Host "Created local.properties"
    } else {
        Write-Error "Could not find Android SDK. Please install Android Studio or create a 'local.properties' file manually with 'sdk.dir=PATH_TO_SDK'."
    }
}

# 3. Check for Fonts (Retry download with curl)
$fontDir = Join-Path $PSScriptRoot "androidApp\src\main\res\font"
if (!(Test-Path $fontDir)) { mkdir $fontDir | Out-Null }

if ((Get-ChildItem $fontDir).Count -eq 0) {
    Write-Host "Fonts missing. Attempting download via curl..." -ForegroundColor Yellow
    
    $fonts = @{
        "inter_regular.ttf" = "https://github.com/google/fonts/raw/main/ofl/inter/static/Inter-Regular.ttf";
        "playfair_display_regular.ttf" = "https://github.com/clauseggers/Playfair-Display/raw/master/fonts/ttf/PlayfairDisplay-Regular.ttf"
    }
    
    foreach ($font in $fonts.GetEnumerator()) {
        try {
            $out = Join-Path $fontDir $font.Key
            # Try Invoke-WebRequest first, then curl
            try {
                Invoke-WebRequest -Uri $font.Value -OutFile $out -UseBasicParsing -TimeoutSec 15
            } catch {
                Write-Host "PowerShell download failed, trying curl..."
                curl.exe -L -o $out $font.Value
            }
        } catch {
            Write-Warning "Failed to download $($font.Key)"
        }
    }
}

# 4. Run Gradle Build
Write-Host "Starting Gradle Build..." -ForegroundColor Cyan
try {
    & .\gradlew.bat :androidApp:assembleDebug
    if ($LASTEXITCODE -eq 0) {
        Write-Host "`nBUILD SUCCESSFUL! ✅" -ForegroundColor Green
        Write-Host "APK location: androidApp\build\outputs\apk\debug\androidApp-debug.apk"
    } else {
        Write-Host "`nBUILD FAILED ❌" -ForegroundColor Red
    }
} catch {
    Write-Error "Failed to execute gradlew: $_"
}
