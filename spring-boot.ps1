# spring-boot.ps1

Write-Host "Starting Meta-Broker Services..." -ForegroundColor Green

# Function to check if a port is in use
function Test-PortInUse {
    param($port)
    
    $connection = New-Object System.Net.Sockets.TcpClient
    try {
        $connection.Connect("127.0.0.1", $port)
        $connection.Close()
        return $true
    }
    catch {
        return $false
    }
}

# Kill existing Java processes on port 8080 if any
if (Test-PortInUse 8080) {
    Write-Host "Port 8080 in use. Stopping existing process..." -ForegroundColor Yellow
    Stop-Process -Name "java" -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 2
}

# Start Hardhat node in a new window
Start-Process powershell -ArgumentList "-NoExit -Command cd './Contracts'; npx hardhat node" -WindowStyle Normal

# Wait for Hardhat to start
Write-Host "Waiting for Hardhat node to start..." -ForegroundColor Yellow
Start-Sleep -Seconds 5

# Deploy contracts
Write-Host "Deploying contracts..." -ForegroundColor Yellow
Set-Location -Path "./Contracts"
npx hardhat run scripts/deployAll.js --network localhost
Set-Location -Path ".."

# Start Spring Boot application
Write-Host "Starting Spring Boot application..." -ForegroundColor Green
$env:SPRING_PROFILES_ACTIVE = "dev"
./mvnw spring-boot:run

# Handle script termination
$OnExit = {
    Write-Host "`nShutting down services..." -ForegroundColor Yellow
    Stop-Process -Name "java" -ErrorAction SilentlyContinue
    Stop-Process -Name "node" -ErrorAction SilentlyContinue
}

Register-EngineEvent -SourceIdentifier PowerShell.Exiting -Action $OnExit 