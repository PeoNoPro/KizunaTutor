param(
    [string]$PostgresBin = 'C:\Program Files\PostgreSQL\18\bin',
    [ValidateRange(1024,65535)][int]$Port = 8085,
    [ValidatePattern('^[a-z][a-z0-9_]*_admin_dev$')][string]$Database = 'kizuna_admin_dev',
    [ValidateSet('Admin','Manager','Cs','Learner','Tutor')][string]$AccountRole = 'Admin',
    [switch]$PrepareOnly
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$workspaceRoot = Split-Path -Parent $projectRoot
$clusterPath = Join-Path $workspaceRoot '.kizuna-local-pg/data'
$pgCtl = Join-Path $PostgresBin 'pg_ctl.exe'
$pgIsReady = Join-Path $PostgresBin 'pg_isready.exe'
$psql = Join-Path $PostgresBin 'psql.exe'
$createdb = Join-Path $PostgresBin 'createdb.exe'

if (!(Test-Path -LiteralPath $psql)) { throw 'Khong tim thay PostgreSQL. Truyen -PostgresBin voi duong dan thu muc bin da cai.' }
if (!(Test-Path -LiteralPath $clusterPath)) { throw 'Chua co PostgreSQL cuc bo cua workspace. Xem docs/ADMIN_DEV.md de chuan bi DB rieng.' }

# Khong doc mat khau Supabase hoac tu tao tai khoan vao DB cua nhom.
$saved = @{}
$names = @('SPRING_PROFILES_ACTIVE','SERVER_PORT','ADMIN_DEV_DB_URL','ADMIN_DEV_DB_USERNAME','ADMIN_DEV_DB_PASSWORD','ADMIN_DEV_EMAIL')
foreach ($name in $names) { $saved[$name] = [Environment]::GetEnvironmentVariable($name, 'Process') }
Push-Location (Join-Path $projectRoot 'backend')
try {
    & $pgIsReady -h 127.0.0.1 -p 5433 *> $null
    if ($LASTEXITCODE -ne 0) {
        & $pgCtl start -D $clusterPath -l (Join-Path $workspaceRoot '.kizuna-local-pg/postgres.log') -o '-p 5433 -h 127.0.0.1' -w
        if ($LASTEXITCODE -ne 0) { throw 'Chua khoi dong duoc PostgreSQL cuc bo.' }
    }
    $databaseExists = & $psql -h 127.0.0.1 -p 5433 -U postgres -d postgres -w -Atc "SELECT datname FROM pg_database WHERE datname='$Database'"
    if ($LASTEXITCODE -ne 0) { throw 'Chua ket noi duoc PostgreSQL cuc bo.' }
    if (!$databaseExists) {
        $source = & $psql -h 127.0.0.1 -p 5433 -U postgres -d postgres -w -Atc "SELECT datname FROM pg_database WHERE datname='kizuna_db'"
        if (!$source) { throw 'Thieu DB mau cuc bo kizuna_db. Xem docs/ADMIN_DEV.md; script khong truy cap Supabase.' }
        & $createdb -h 127.0.0.1 -p 5433 -U postgres -w --template=kizuna_db $Database
        if ($LASTEXITCODE -ne 0) { throw 'Chua tao duoc ban sao DB cuc bo. Khong ghi de DB da co.' }
    }
    $desiredRole = switch ($AccountRole) { 'Manager' { 'ROLE_MANAGER' } 'Cs' { 'ROLE_CS_STAFF' } 'Learner' { 'ROLE_LEARNER' } 'Tutor' { 'ROLE_TUTOR' } default { 'ROLE_ADMIN' } }
    $email = & $psql -h 127.0.0.1 -p 5433 -U postgres -d $Database -w -Atc "SELECT u.email FROM users u JOIN roles r ON r.role_id=u.role_id WHERE r.role_name='$desiredRole' AND r.is_active=true AND u.account_status::text='ACTIVE' AND u.deleted_at IS NULL ORDER BY u.created_at LIMIT 1"
    if ($LASTEXITCODE -ne 0 -or !$email) { throw 'DB phat trien can tai khoan dang hoat dong voi vai tro da chon. Xem docs/ADMIN_DEV.md.' }
    $version = & $psql -h 127.0.0.1 -p 5433 -U postgres -d $Database -w -Atc "SELECT version FROM operations_schema_versions WHERE version='fe11_001'"
    if ($LASTEXITCODE -ne 0 -or !$version) { throw 'DB cuc bo chua co schema FE-11. Xem database/migrations/README-fe11.md.' }
    & $psql -h 127.0.0.1 -p 5433 -U postgres -d $Database -w -X -v ON_ERROR_STOP=1 -f (Join-Path $projectRoot 'database/seeds/admin_staff_schema.sql') | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Chua chuan bi duoc bang lich su nhan su trong DB phat trien rieng.' }
    & $psql -h 127.0.0.1 -p 5433 -U postgres -d $Database -w -X -v ON_ERROR_STOP=1 -f (Join-Path $projectRoot 'database/seeds/fe10_support_ticket.sql') | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Migration FE-10 chua dat. Xem thong bao preflight; khong tu sua du lieu cu.' }
    if ($PrepareOnly) { Write-Host 'DB quan tri phat trien da san sang.'; return }
    $probe = New-Object System.Net.Sockets.TcpListener([System.Net.IPAddress]::Loopback, $Port)
    try { $probe.Start() } catch { throw "Cong $Port dang duoc su dung. Dung backend cu trong cua so da chay no truoc khi chay script nay." } finally { $probe.Stop() }
    $env:SPRING_PROFILES_ACTIVE = 'admin-dev'
    $env:SERVER_PORT = [string]$Port
    $env:ADMIN_DEV_DB_URL = "jdbc:postgresql://127.0.0.1:5433/$Database"
    $env:ADMIN_DEV_DB_USERNAME = 'postgres'
    # Cluster workspace chi cho phep ket noi cuc bo; mat khau neu co lay tu bien moi truong rieng.
    $env:ADMIN_DEV_DB_PASSWORD = $saved['ADMIN_DEV_DB_PASSWORD']
    $env:ADMIN_DEV_EMAIL = $email.Trim()
    Write-Host "Backend quan tri: http://localhost:$Port | DB cuc bo rieng | Ctrl+C de dung."
    & .\mvnw.cmd '-Dspring-boot.run.jvmArguments=-Dspring.devtools.restart.enabled=false' spring-boot:run
    if ($LASTEXITCODE -ne 0) { throw 'Backend chua khoi dong duoc. Xem log phia tren.' }
} finally {
    foreach ($name in $names) { [Environment]::SetEnvironmentVariable($name, $saved[$name], 'Process') }
    Pop-Location
}
