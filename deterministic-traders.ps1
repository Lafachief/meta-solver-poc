Write-Host "Starting Deterministic Traders..." -ForegroundColor Green

# Get the current directory (Meta-Solver root)
$rootDir = $PSScriptRoot
$tradersDir = Join-Path $rootDir "clientSideScripts\Traders"

# Function to create a new PowerShell window and run a script
function Start-TraderWindow {
    param (
        [string]$traderScript,
        [string]$windowTitle
    )
    
    $scriptPath = Join-Path $tradersDir $traderScript
    
    Start-Process powershell -ArgumentList @(
        "-NoExit",
        "-Command",
        "Set-Location '$tradersDir'; Write-Host 'Starting $windowTitle...' -ForegroundColor Green; python '$scriptPath'"
    ) -WindowStyle Normal
}

# Start Trader A
Write-Host "Launching Trader A..." -ForegroundColor Yellow
Start-TraderWindow -traderScript "deterministic_trader_a.py" -windowTitle "Deterministic Trader A"

# Wait a moment before starting Trader B
Start-Sleep -Seconds 2

# Start Trader B
Write-Host "Launching Trader B..." -ForegroundColor Yellow
Start-TraderWindow -traderScript "deterministic_trader_b.py" -windowTitle "Deterministic Trader B"

Write-Host "Both traders have been launched in separate windows." -ForegroundColor Green 