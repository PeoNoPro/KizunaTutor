# KizunaTutor - Chạy VOICEVOX Engine qua PowerShell
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host " KizunaTutor - Khởi Động VOICEVOX Engine (AI TTS)" -ForegroundColor Green
Write-Host " Docker Image: voicevox/voicevox_engine:cpu-ubuntu20.04-latest" -ForegroundColor Yellow
Write-Host " Cổng phục vụ: http://127.0.0.1:50021" -ForegroundColor Yellow
Write-Host "========================================================" -ForegroundColor Cyan

docker run --rm -it -p 127.0.0.1:50021:50021 voicevox/voicevox_engine:cpu-ubuntu20.04-latest
