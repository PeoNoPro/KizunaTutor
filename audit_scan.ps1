$files = Get-ChildItem -Path "C:\Users\Vuh26\Desktop\KizunaTutor\src\main\resources\templates" -Filter "*.html" -Recurse
$report = @()

foreach ($f in $files) {
    $content = Get-Content $f.FullName -Raw
    $badRadius = [regex]::Matches($content, '\b(rounded-lg|rounded-xl|rounded-2xl|rounded-3xl|rounded-md)\b').Count
    $badShadow = [regex]::Matches($content, '\b(shadow-lg|shadow-xl|shadow-2xl)\b').Count
    $uppercase = [regex]::Matches($content, '\buppercase\b').Count
    $roleLeak = [regex]::Matches($content, '\bROLE_[A-Z_]+\b').Count
    
    if ($badRadius -gt 0 -or $badShadow -gt 0 -or $uppercase -gt 0 -or $roleLeak -gt 0) {
        $report += [PSCustomObject]@{
            File = $f.FullName.Replace("C:\Users\Vuh26\Desktop\KizunaTutor\src\main\resources\templates\", "")
            BadRadius = $badRadius
            BadShadow = $badShadow
            Uppercase = $uppercase
            RoleLeak = $roleLeak
            Total = ($badRadius + $badShadow + $uppercase + $roleLeak)
        }
    }
}

$report | Sort-Object -Property Total -Descending | Format-Table -AutoSize
