$job = Start-Job -ScriptBlock {
    cd "C:\Users\User\Desktop\RLIFE-JAVA-main"
    .\mvnw.cmd javafx:run 2>&1
}

Start-Sleep -Seconds 15
Stop-Job $job
$output = Receive-Job $job
$output | Out-File -FilePath "test_output.txt"
Write-Host "Application test completed. Output saved to test_output.txt"
Get-Content "test_output.txt" -Tail 50

