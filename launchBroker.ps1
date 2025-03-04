# 3. Start Test Client
Write-Host "`nStarting Test Client..."
$testClient = Start-Process -FilePath "python" -ArgumentList "testClient.py" -WorkingDirectory ".\clientSideScripts" -PassThru -NoNewWindow

# 4. Start Java Exchange
Write-Host "`nStarting Java Exchange..."
$exchange = Start-Process -FilePath "cmd.exe" -ArgumentList "/c mvn clean compile exec:java" -WorkingDirectory ".\Meta-Broker" -PassThru -NoNewWindow

Write-Host "`nAll components started!"
Write-Host "Press Enter to stop all processes..."
Read-Host

# Cleanup
Write-Host "`nStopping all processes..."
$hardhat, $testClient, $exchange | ForEach-Object { 
    if ($_ -ne $null) { 
        Stop-Process -Id $_.Id -Force 
    }
} 