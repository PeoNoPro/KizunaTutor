@echo off
chcp 65001 >nul
echo ========================================================
echo  KizunaTutor - Khởi Động VOICEVOX Engine (AI TTS)
echo  Docker Image: voicevox/voicevox_engine:cpu-ubuntu20.04-latest
echo  Cổng phục vụ: http://127.0.0.1:50021
echo ========================================================
echo.
echo Đang kiểm tra và chạy container VOICEVOX...
docker run --rm -it -p 127.0.0.1:50021:50021 voicevox/voicevox_engine:cpu-ubuntu20.04-latest
pause
