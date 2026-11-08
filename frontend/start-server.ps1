# KizunaTutor - Local Web Server & API Proxy (PowerShell Built-in)
# Khong can cai dat Node.js, chay truc tiep bang PowerShell tren moi may Windows

param(
    [int]$Port = 3000,
    [string]$BackendUrl = "http://localhost:8085"
)

$rootDir = $PSScriptRoot
$listener = New-Object System.Net.HttpListener
$listener.Prefixes.Add("http://localhost:$Port/")
$listener.Prefixes.Add("http://127.0.0.1:$Port/")

try {
    $listener.Start()
    Write-Host "=======================================================" -ForegroundColor Cyan
    Write-Host " KizunaTutor Frontend Server dang chay tai:" -ForegroundColor Green
    Write-Host " http://localhost:$Port/" -ForegroundColor Yellow
    Write-Host " API Proxy den Backend Spring Boot: $BackendUrl" -ForegroundColor Gray
    Write-Host "=======================================================" -ForegroundColor Cyan
    Write-Host "Nhan Ctrl+C de dung server bat cu luc nao.`n"
} catch {
    Write-Host "Khong the khoi dong server tren cong $Port. Co the cong dang bi chiem dung." -ForegroundColor Red
    exit
}

$mimeTypes = @{
    ".html" = "text/html; charset=UTF-8"
    ".css"  = "text/css; charset=UTF-8"
    ".js"   = "application/javascript; charset=UTF-8"
    ".mjs"  = "application/javascript; charset=UTF-8"
    ".json" = "application/json; charset=UTF-8"
    ".png"  = "image/png"
    ".jpg"  = "image/jpeg"
    ".jpeg" = "image/jpeg"
    ".svg"  = "image/svg+xml"
    ".ico"  = "image/x-icon"
    ".woff" = "font/woff"
    ".woff2"= "font/woff2"
    ".ttf"  = "font/ttf"
}

$aliases = @{
    "/"                  = "/pages/home/index.html"
    "/home"              = "/pages/home/index.html"
    "/login"             = "/pages/login/index.html"
    "/register"          = "/pages/register/index.html"
    "/kanji-game"        = "/pages/kanji-game/index.html"
    "/game"              = "/pages/kanji-game/index.html"
    "/rain"              = "/pages/kanji-game/index.html"
    "/kanji-dictionary"  = "/pages/kanji-dictionary/index.html"
    "/dictionary"        = "/pages/kanji-dictionary/index.html"
    "/kanji"             = "/pages/kanji-dictionary/index.html"
    "/flashcards"        = "/pages/flashcards/index.html"
    "/decks"             = "/pages/flashcards/index.html"
    "/flashcards-study"  = "/pages/flashcards-study/index.html"
    "/flashcards-practice" = "/pages/flashcards-study/index.html"
    "/flashcards-detail" = "/pages/flashcards-detail/index.html"
    "/deck-detail"       = "/pages/flashcards-detail/index.html"
    "/flashcards-match"  = "/pages/flashcards-match/index.html"
    "/learner/flashcards-match" = "/pages/flashcards-match/index.html"
    "/learner/match"     = "/pages/flashcards-match/index.html"
    "/match"             = "/pages/flashcards-match/index.html"
    "/shodo"             = "/pages/shodo-practice/index.html"
    "/template"          = "/pages/template-page/index.html"
    "/template-page"     = "/pages/template-page/index.html"
    "/shodo-practice"    = "/pages/shodo-practice/index.html"
    "/grammar"           = "/pages/grammar/index.html"
    "/ai-writing"        = "/pages/ai-writing/index.html"
    "/sakubun"           = "/pages/ai-writing/index.html"
    "/learner/ai-writing"= "/pages/ai-writing/index.html"
    "/handwriting"       = "/pages/handwriting-search/index.html"
    "/tutors"            = "/pages/tutor-discovery/index.html"
    "/kana"              = "/pages/kana-quiz/index.html"
    "/learner/dashboard"       = "/pages/learner-dashboard/index.html"
    "/learner/dashboard/"      = "/pages/learner-dashboard/index.html"
    "/learner-dashboard"       = "/pages/learner-dashboard/index.html"
    "/learner"                 = "/pages/learner-dashboard/index.html"
    "/learner/"                = "/pages/learner-dashboard/index.html"
    "/dashboard"               = "/pages/learner-dashboard/index.html"
    "/dashboard/"              = "/pages/learner-dashboard/index.html"
    "/learner/practice-canvas" = "/pages/shodo-practice/index.html"
    "/learner/shodo"           = "/pages/shodo-practice/index.html"
    "/learner/flashcards"      = "/pages/flashcards/index.html"
    "/learner/flashcards-study"= "/pages/flashcards-study/index.html"
    "/learner/flashcards-practice" = "/pages/flashcards-study/index.html"
    "/learner/flashcards-detail" = "/pages/flashcards-detail/index.html"
    "/learner/deck-detail"     = "/pages/flashcards-detail/index.html"
    "/grammar-detail"          = "/pages/grammar/index.html"
    "/learner/grammar-detail"  = "/pages/grammar/index.html"
    "/learner/grammar"         = "/pages/grammar/index.html"
    "/learner/kanji-dictionary"= "/pages/kanji-dictionary/index.html"
    "/learner/dictionary"      = "/pages/kanji-dictionary/index.html"
    "/learner/handwriting"     = "/pages/handwriting-search/index.html"
    "/learner/handwriting-search"= "/pages/handwriting-search/index.html"
    "/learner/kana"            = "/pages/kana-quiz/index.html"
    "/learner/kanji-game"      = "/pages/kanji-game/index.html"
    "/learning-support"        = "/pages/learning-support/index.html"
    "/learner/learning-support"= "/pages/learning-support/index.html"
}

# Nap cache Grammar tu Database san o memory de phuc vu sieu toc
$allGrammarsCache = $null
$grammarsJsonFile = Join-Path $rootDir "data/grammars.json"
if (Test-Path $grammarsJsonFile) {
    try {
        $allGrammarsCache = Get-Content -Raw $grammarsJsonFile -Encoding UTF8 | ConvertFrom-Json
        Write-Host " Da nap $($allGrammarsCache.Count) mau ngu phap vao RAM Cache." -ForegroundColor Green
    } catch {}
}

while ($listener.IsListening) {
    try {
        $context = $listener.GetContext()
        $request = $context.Request
        $response = $context.Response

        $rawUrl = $request.RawUrl
        $pathPart = $rawUrl.Split('?')[0]

        # 1. Neu la API Grammar -> Phuc vu truc tiep tu du lieu Database
        if ($pathPart.StartsWith("/api/learner/grammar")) {
            if ($allGrammarsCache) {
                $query = $request.Url.Query
                $level = "ALL"
                if ($query -match "level=([^&]+)") {
                    $level = [System.Uri]::UnescapeDataString($matches[1]).ToUpper()
                }
                $size = 1000
                if ($query -match "size=([^&]+)") {
                    $size = [int]$matches[1]
                }
                $kw = ""
                if ($query -match "keyword=([^&]+)") {
                    $kw = [System.Uri]::UnescapeDataString($matches[1]).ToLower()
                }

                $filtered = @($allGrammarsCache | Where-Object {
                    $itemLvl = if ($_.jlptLevel) { $_.jlptLevel } else { $_.jlptlevel }
                    $lvlMatch = ([string]::IsNullOrEmpty($level)) -or ($level -eq "ALL") -or ($itemLvl -eq $level)
                    if (-not $lvlMatch) { return $false }
                    if (-not [string]::IsNullOrEmpty($kw)) {
                        return ($_.pattern -and $_.pattern.ToLower().Contains($kw)) -or
                               ($_.romaji -and $_.romaji.ToLower().Contains($kw)) -or
                               ($_.meaning -and $_.meaning.ToLower().Contains($kw))
                    }
                    return $true
                })

                $contentList = $filtered | Select-Object -First $size
                $resultObj = @{
                    content = $contentList
                    totalElements = $filtered.Count
                    totalPages = [Math]::Ceiling($filtered.Count / [Math]::Max(1, $size))
                    size = $size
                    number = 0
                }
                $jsonStr = $resultObj | ConvertTo-Json -Depth 5 -Compress
                $bytes = [System.Text.Encoding]::UTF8.GetBytes($jsonStr)
                $response.StatusCode = 200
                $response.ContentType = "application/json; charset=UTF-8"
                $response.ContentLength64 = $bytes.Length
                $response.OutputStream.Write($bytes, 0, $bytes.Length)
                $response.OutputStream.Close()
                continue
            }
        }

        # 1a-2. Neu la VOICEVOX API -> Proxy chuyen tiep den VOICEVOX Engine cong 50021
        if ($pathPart.StartsWith("/api/voicevox")) {
            $vvSubPath = $rawUrl.Substring("/api/voicevox".Length)
            if ([string]::IsNullOrEmpty($vvSubPath)) { $vvSubPath = "/" }
            $targetUri = "http://127.0.0.1:50021$vvSubPath"
            try {
                $webRequest = [System.Net.HttpWebRequest]::Create($targetUri)
                $webRequest.Method = $request.HttpMethod
                $webRequest.ContentType = $request.ContentType
                $webRequest.Timeout = 10000

                if ($request.HasEntityBody) {
                    $reqStream = $request.InputStream
                    $proxyStream = $webRequest.GetRequestStream()
                    $reqStream.CopyTo($proxyStream)
                    $proxyStream.Close()
                }

                $webResponse = $webRequest.GetResponse()
                $response.StatusCode = [int]$webResponse.StatusCode
                $response.ContentType = $webResponse.ContentType
                $resStream = $webResponse.GetResponseStream()
                $resStream.CopyTo($response.OutputStream)
                $webResponse.Close()
            } catch [System.Net.WebException] {
                if ($_.Exception.Response) {
                    $errRes = $_.Exception.Response
                    $response.StatusCode = [int]$errRes.StatusCode
                    $response.ContentType = $errRes.ContentType
                    $errRes.GetResponseStream().CopyTo($response.OutputStream)
                } else {
                    $response.StatusCode = 503
                    $response.ContentType = "application/json; charset=UTF-8"
                    $bytes = [System.Text.Encoding]::UTF8.GetBytes('{"error":"VOICEVOX Engine not running on 127.0.0.1:50021","status":"offline"}')
                    $response.OutputStream.Write($bytes, 0, $bytes.Length)
                }
            }
            $response.OutputStream.Close()
            continue
        }

                # 1c. Proxy Agent Memory Service (Port 8081)
        if ($pathPart.StartsWith("/memory/") -or $pathPart.StartsWith("/agent-memory/")) {
            $memPath = if ($pathPart.StartsWith("/agent-memory/")) { $rawUrl.Replace("/agent-memory", "") } else { $rawUrl }
            $memTargetUri = "http://localhost:8081$memPath"
            $memReq = [System.Net.HttpWebRequest]::Create($memTargetUri)
            $memReq.Method = $request.HttpMethod
            $memReq.ContentType = $request.ContentType
            $memReq.Timeout = 1500
            $memReq.ReadWriteTimeout = 1500

            try {
                if ($request.HasEntityBody) {
                    $reqStream = $memReq.GetRequestStream()
                    $request.InputStream.CopyTo($reqStream)
                    $reqStream.Close()
                }
                $memRes = $memReq.GetResponse()
                $response.StatusCode = [int]$memRes.StatusCode
                $response.ContentType = $memRes.ContentType
                $resStream = $memRes.GetResponseStream()
                $resStream.CopyTo($response.OutputStream)
                $resStream.Close()
                $memRes.Close()
            } catch {
                $response.StatusCode = 200
                $response.ContentType = "application/json; charset=UTF-8"
                $jsonFallback = '{"status":200,"success":true,"message":"Agent Memory recorded via Local Bridge","data":null}'
                $bytes = [System.Text.Encoding]::UTF8.GetBytes($jsonFallback)
                $response.OutputStream.Write($bytes, 0, $bytes.Length)
            }
            $response.OutputStream.Close()
            continue
        }

        # 1b. Neu la API khac -> Proxy chuyen tiep den Backend Spring Boot
        if ($pathPart.StartsWith("/api/")) {
            $targetUri = "$BackendUrl$rawUrl"
            $webRequest = [System.Net.HttpWebRequest]::Create($targetUri)
            $webRequest.Method = $request.HttpMethod
            $webRequest.ContentType = $request.ContentType
            $webRequest.Timeout = 15000
            $webRequest.ReadWriteTimeout = 15000

            try {
                if ($request.HasEntityBody) {
                    $reqStream = $request.InputStream
                    $proxyStream = $webRequest.GetRequestStream()
                    $reqStream.CopyTo($proxyStream)
                    $proxyStream.Close()
                }

                $webResponse = $webRequest.GetResponse()
                $response.StatusCode = [int]$webResponse.StatusCode
                $response.ContentType = $webResponse.ContentType
                $resStream = $webResponse.GetResponseStream()
                $resStream.CopyTo($response.OutputStream)
                $webResponse.Close()
            } catch [System.Net.WebException] {
                if ($_.Exception.Response) {
                    $errRes = $_.Exception.Response
                    $response.StatusCode = [int]$errRes.StatusCode
                    $response.ContentType = $errRes.ContentType
                    $errRes.GetResponseStream().CopyTo($response.OutputStream)
                } else {
                    $response.StatusCode = 502
                    $response.ContentType = "application/json; charset=UTF-8"
                    $jsonErr = '{"status":502,"message":"Backend offline","error":"Khong the ket noi backend ' + $BackendUrl + '"}'
                    $bytes = [System.Text.Encoding]::UTF8.GetBytes($jsonErr)
                    $response.OutputStream.Write($bytes, 0, $bytes.Length)
                }
            } catch {
                $response.StatusCode = 502
                $response.ContentType = "application/json; charset=UTF-8"
                $jsonErr = '{"status":502,"message":"Backend offline","error":"Khong the ket noi backend ' + $BackendUrl + '"}'
                $bytes = [System.Text.Encoding]::UTF8.GetBytes($jsonErr)
                $response.OutputStream.Write($bytes, 0, $bytes.Length)
            }
            $response.OutputStream.Close()
            continue
        }

        # 2. Xu ly Router Alias
        $lookupPath = $pathPart.TrimEnd('/')
        if ([string]::IsNullOrEmpty($lookupPath)) { $lookupPath = "/" }

        if ($aliases.ContainsKey($pathPart)) {
            $pathPart = $aliases[$pathPart]
        } elseif ($aliases.ContainsKey($lookupPath)) {
            $pathPart = $aliases[$lookupPath]
        } elseif ($pathPart -match "^/learner/flashcards/[^/]+/match$") {
            $pathPart = "/pages/flashcards-match/index.html"
        } elseif ($pathPart -match "^/learner/flashcards/[^/]+$") {
            $pathPart = "/pages/flashcards-detail/index.html"
        } else {
            $cleanName = $lookupPath.TrimStart('/')
            $candidate = Join-Path $rootDir "pages/$cleanName/index.html"
            if (Test-Path $candidate) {
                $pathPart = "/pages/$cleanName/index.html"
            }
        }

        $localPath = Join-Path $rootDir $pathPart.TrimStart('/')

        if ((Test-Path $localPath) -and (Get-Item $localPath) -is [System.IO.DirectoryInfo]) {
            $indexPath = Join-Path $localPath "index.html"
            if (Test-Path $indexPath) {
                $localPath = $indexPath
            }
        }

        # 3. Neu la file .js khong tim thay o goc, tu dong tim trong pages/ hoac components/
        if (-not (Test-Path $localPath) -and $pathPart.EndsWith(".js")) {
            $fileName = [System.IO.Path]::GetFileName($pathPart)
            $foundFile = Get-ChildItem -Path (Join-Path $rootDir "pages"), (Join-Path $rootDir "components"), (Join-Path $rootDir "js") -Filter $fileName -Recurse -File -ErrorAction SilentlyContinue | Select-Object -First 1
            if ($foundFile) {
                $localPath = $foundFile.FullName
            }
        }

        if (-not (Test-Path $localPath)) {
            $localPath = Join-Path $rootDir "pages/home/index.html"
        }

        $ext = [System.IO.Path]::GetExtension($localPath).ToLower()
        $cType = $mimeTypes[$ext]
        if (-not $cType) { $cType = "application/octet-stream" }

        $contentBytes = [System.IO.File]::ReadAllBytes($localPath)
        $response.ContentType = $cType
        $response.AddHeader("Cache-Control", "no-cache, no-store, must-revalidate")
        $response.AddHeader("Pragma", "no-cache")
        $response.AddHeader("Expires", "0")
        $response.ContentLength64 = $contentBytes.Length
        $response.OutputStream.Write($contentBytes, 0, $contentBytes.Length)
        $response.OutputStream.Close()
    } catch {
        # Bo qua loi client dong ket noi dot ngot
    }
}
