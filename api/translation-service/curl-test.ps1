# ===========================================================
# curl-test.ps1  —  Quick test runner for the Translation API
# Usage:  .\curl-test.ps1
#         .\curl-test.ps1 -Text "Good morning" -SourceLang "en" -Targets "ja","ru","de"
# ===========================================================
param(
    [string]$Text       = "Hello world. Please translate this document.",
    [string]$SourceLang = "en",
    [string[]]$Targets  = @("ja", "ru"),
    [string]$BaseUrl    = "http://localhost:8083"
)

$langArray = ($Targets | ForEach-Object { "`"$_`"" }) -join ","

$payload = @"
{
  "textToTranslate": "$Text",
  "sourceLanguage":  "$SourceLang",
  "languageCodes":   [$langArray]
}
"@

Write-Host ""
Write-Host "POST $BaseUrl/api/translate" -ForegroundColor Cyan
Write-Host "Body: $payload" -ForegroundColor DarkGray
Write-Host ""

$bytes = [System.Text.Encoding]::UTF8.GetBytes($payload)

try {
    $response = Invoke-WebRequest `
        -Uri            "$BaseUrl/api/translate" `
        -Method         POST `
        -ContentType    "application/json; charset=utf-8" `
        -Body           $bytes `
        -TimeoutSec     180

    $json = [System.Text.Encoding]::UTF8.GetString($response.Content) | ConvertFrom-Json

    Write-Host "Status: $($response.StatusCode) OK" -ForegroundColor Green
    Write-Host ""
    Write-Host "=== Translation Results ===" -ForegroundColor Yellow

    foreach ($result in $json.translationResults) {
        Write-Host ""
        Write-Host "  Language : $($result.languageCode)" -ForegroundColor Magenta
        Write-Host "  Value    : $($result.value)"
        Write-Host "  Timestamp: $($result.translatedTimestamp)"
    }

    Write-Host ""
    Write-Host "=== Usage ===" -ForegroundColor Yellow
    Write-Host "  Characters billed: $($json.cognitiveServicesUsage.translatorTranslateTextCharacterCount)"
    Write-Host ""

} catch {
    Write-Host "Request failed: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        $errBody = [System.IO.StreamReader]::new(
            $_.Exception.Response.GetResponseStream()).ReadToEnd()
        Write-Host $errBody -ForegroundColor Red
    }
}

