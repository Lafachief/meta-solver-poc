# Set working directory to Meta-Solver root
$rootDir = $PSScriptRoot
Set-Location $rootDir

Write-Host "Starting Exchange System from $rootDir"

# Define config paths
$userConfigPath = ".\Users\config.json"
$configDestClient = ".\clientSideScripts\config.json"

# Clean up old config files
Write-Host "`nCleaning up old config files..."
if (Test-Path $userConfigPath) {
    Remove-Item $userConfigPath -Force
    Write-Host "Deleted $userConfigPath"
}
if (Test-Path $configDestClient) {
    Remove-Item $configDestClient -Force
    Write-Host "Deleted $configDestClient"
}

# 1. Start Hardhat Node
Write-Host "`nStarting Hardhat Node..."
$hardhat = Start-Process -FilePath "cmd.exe" -ArgumentList "/c npx hardhat node" -WorkingDirectory ".\Contracts" -PassThru -NoNewWindow

# Wait for node to start
Write-Host "Waiting for Hardhat node to initialize..."
Start-Sleep -Seconds 5

# 2. Deploy Contracts and create configs
Write-Host "`nDeploying Contracts..."
$deploy = Start-Process -FilePath "cmd.exe" -ArgumentList "/c npx hardhat run scripts/deployAll.js --network localhost" -WorkingDirectory ".\Contracts" -Wait -NoNewWindow

# Wait for config files to be created
Write-Host "Waiting for contract deployment and config generation..."
Start-Sleep -Seconds 5

# Verify user config exists before copying
if (!(Test-Path $userConfigPath)) {
    Write-Host "Error: User config not found at $userConfigPath after deployment"
    exit 1
}

# Copy user config to client scripts
Write-Host "`nCopying config files..."
New-Item -ItemType Directory -Force -Path (Split-Path $configDestClient)
Copy-Item -Path $userConfigPath -Destination $configDestClient -Force
Write-Host "Config files copied successfully"

# Start the rest of the services...
