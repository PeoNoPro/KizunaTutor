param([ValidateSet('Cs','Manager','Learner','Tutor','Admin')][string]$AccountRole='Cs')
$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
$backendDirectory=[IO.Path]::GetFullPath((Join-Path $projectRoot 'backend'))
$listener=Get-NetTCPConnection -State Listen -LocalPort 8085 -ErrorAction SilentlyContinue
if($listener){
    $backendProcess=Get-CimInstance Win32_Process -Filter "ProcessId=$($listener[0].OwningProcess)"
    if($backendProcess.Name -ne 'java.exe' -or $backendProcess.CommandLine -notlike '*com.kizuna.KizunaBackendApplication*'){throw 'Cong 8085 khong thuoc backend KizunaTutor. Script khong dung tien trinh khac.'}
    $jcmd=Join-Path (Split-Path -Parent $backendProcess.ExecutablePath) 'jcmd.exe'
    if(!(Test-Path -LiteralPath $jcmd)){throw 'Khong xac minh duoc backend dang chay. Vui long dung backend cu trong cua so da mo no.'}
    $properties=& $jcmd $backendProcess.ProcessId VM.system_properties 2>$null
    $directoryLine=$properties | Where-Object {$_ -like 'user.dir=*'} | Select-Object -First 1
    if(!$directoryLine){throw 'Khong xac minh duoc thu muc backend. Khong dung tien trinh.'}
    $actualDirectory=$directoryLine.Substring(9).Replace('\:',':').Replace('\\','\')
    if([IO.Path]::GetFullPath($actualDirectory) -ne $backendDirectory){throw 'Backend tren cong 8085 thuoc workspace khac. Khong dung tien trinh.'}
    Stop-Process -Id $backendProcess.ProcessId
    for($attempt=0;$attempt -lt 30;$attempt++){if(!(Get-NetTCPConnection -State Listen -LocalPort 8085 -ErrorAction SilentlyContinue)){break};Start-Sleep -Milliseconds 100}
}
if(!(Get-NetTCPConnection -State Listen -LocalPort 3000 -ErrorAction SilentlyContinue)){
    Start-Process -FilePath node.exe -WindowStyle Hidden -ArgumentList @(('"'+(Join-Path $projectRoot 'frontend/server.js')+'"')) -WorkingDirectory (Join-Path $projectRoot 'frontend')
}
$view=switch($AccountRole){'Cs' {'cs'} 'Manager' {'manager'} 'Tutor' {'tutor'} default {'learner'}}
$pagePath=if($AccountRole -eq 'Admin'){'/admin/dashboard'}elseif($AccountRole -eq 'Cs'){'/cs/dashboard'}else{"/$view/support/tickets"}
Write-Host "Mo trang sau khi backend khoi dong: http://localhost:3000$pagePath"
Write-Host 'Phien nay dung tai khoan that trong DB phat trien cuc bo; khong can dang nhap. Ctrl+C de dung backend.'
& (Join-Path $PSScriptRoot 'start-admin-dev.ps1') -AccountRole $AccountRole
