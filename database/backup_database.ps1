# ==============================================================================
# KIZUNATUTOR - SCRIPT SAO LƯU TOÀN DIỆN CƠ SỞ DỮ LIỆU (DATABASE BACKUP AUTOMATION)
# ==============================================================================
# Mục đích:
# 1. Tự động xuất (pg_dump) toàn bộ schema + data của KizunaTutor ra file .sql nén.
# 2. Đảm bảo 100% tự chủ dữ liệu, độc lập hoàn toàn với các kho lưu trữ bên ngoài.
# 3. Tương thích với Supabase PostgreSQL / Local PostgreSQL.
# ==============================================================================

param (
    [string]$DbHost = $env:DB_HOST,
    [string]$DbPort = $env:DB_PORT,
    [string]$DbName = $env:DB_NAME,
    [string]$DbUser = $env:DB_USER,
    [string]$OutputDir = "$PSScriptRoot\backups"
)

# Thiết lập giá trị mặc định nếu chưa cấu hình biến môi trường
if (-not $DbHost) { $DbHost = "localhost" }
if (-not $DbPort) { $DbPort = "5432" }
if (-not $DbName) { $DbName = "kizuna_tutor" }
if (-not $DbUser) { $DbUser = "postgres" }

# Tạo thư mục lưu backup nếu chưa tồn tại
if (-not (Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
}

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$backupFile = Join-Path $OutputDir "kizunatutor_backup_$timestamp.sql"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "🚀 ĐANG KHỞI CHẠY SAO LƯU CƠ SỞ DỮ LIỆU KIZUNATUTOR..." -ForegroundColor Yellow
Write-Host "Host: $DbHost | Port: $DbPort | Database: $DbName | User: $DbUser" -ForegroundColor Gray
Write-Host "File đích: $backupFile" -ForegroundColor Gray
Write-Host "==========================================================" -ForegroundColor Cyan

# Kiểm tra công cụ pg_dump
$pgDumpCmd = Get-Command pg_dump -ErrorAction SilentlyContinue

if ($pgDumpCmd) {
    try {
        & pg_dump -h $DbHost -p $DbPort -U $DbUser -d $DbName -F p -b -v -f $backupFile
        Write-Host "✅ SAO LƯU THÀNH CÔNG!" -ForegroundColor Green
        Write-Host "Đã xuất cơ sở dữ liệu an toàn tại: $backupFile" -ForegroundColor White
    } catch {
        Write-Host "❌ Lỗi trong quá trình chạy pg_dump: $_" -ForegroundColor Red
    }
} else {
    Write-Host "⚠️ Không tìm thấy lệnh 'pg_dump' trong biến môi trường PATH." -ForegroundColor Yellow
    Write-Host "💡 Hướng dẫn cấu hình công cụ xuất dữ liệu thủ công:" -ForegroundColor Cyan
    Write-Host "1. Sử dụng pgAdmin 4: Chuột phải vào database '$DbName' -> Chọn 'Backup...' -> Chọn Plain/Custom format." -ForegroundColor White
    Write-Host "2. Sử dụng DBeaver: Chuột phải vào Connection -> Tools -> Dump Database." -ForegroundColor White
    Write-Host "3. Hoặc đảm bảo tất cả file seed tại 'database/seeds/*.sql' đã được lưu trữ vào Git repo." -ForegroundColor White
}
