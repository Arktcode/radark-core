$ErrorActionPreference = "Stop"

Write-Host "Downloading Mindustry assets (from master)..."

if (-not (Test-Path "assets/sprites")) {
    New-Item -ItemType Directory -Force -Path "assets/sprites" | Out-Null
}

$baseUrl = "https://raw.githubusercontent.com/Anuken/Mindustry/master/core/assets/sprites"

$files = @(
    "sprites.aatls",
    "sprites.png" 
)

foreach ($file in $files) {
    if (Test-Path "assets/sprites/$file") {
        Write-Host "$file already exists. Skipping."
        continue
    }

    $url = "$baseUrl/$file"
    $output = "assets/sprites/$file"
    
    Write-Host "Downloading $file..."
    try {
        Invoke-WebRequest -Uri $url -OutFile $output -UserAgent "Mozilla/5.0"
        Write-Host "  -> OK"
    }
    catch {
        Write-Host "  -> Failed to download $file. Error: $_"
    }
}


if (-not (Test-Path "assets/sprites/block_colors.png")) {
    Write-Host "Downloading block_colors.png..."
    try {
        Invoke-WebRequest -Uri "$baseUrl/block_colors.png" -OutFile "assets/sprites/block_colors.png" -UserAgent "Mozilla/5.0"
        Write-Host "  -> OK"
    }
    catch {
        Write-Host "  -> Failed (Optional)"
    }
}

Write-Host "Done!"
