param([string]$SampleDirectory)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$testRoot = Join-Path ([IO.Path]::GetTempPath()) ('shimeji-import-checks-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $testRoot | Out-Null
$oldSampleDirectory = $env:SHIMEJI_SAMPLE_DIR
Push-Location -LiteralPath $projectRoot
try {
    if ($SampleDirectory) { $env:SHIMEJI_SAMPLE_DIR = $SampleDirectory }
    python -B -m unittest discover -s tests -p 'test_*.py' -v
    if ($LASTEXITCODE -ne 0) { throw 'Python tests failed.' }
    $compiler = Join-Path $env:WINDIR 'Microsoft.NET\Framework64\v4.0.30319\csc.exe'
    $sources = @('CharacterImport.cs', 'CharacterFormats.cs', 'CharacterWebp.cs') | ForEach-Object { Join-Path $projectRoot $_ }
    $references = @('/r:System.IO.Compression.dll', '/r:System.IO.Compression.FileSystem.dll', '/r:System.Web.Extensions.dll', '/r:System.Xml.Linq.dll')
    $resources = @(('/resource:' + (Join-Path $projectRoot 'character_frames.json') + ',Shimeji.CharacterFrames.json'),
                   ('/resource:' + (Join-Path $projectRoot 'lib\webp\dwebp.exe') + ',Shimeji.dwebp.exe'))
    $importExe = Join-Path $testRoot 'CharacterImportTest.exe'
    & $compiler /nologo /codepage:65001 /target:exe ('/out:' + $importExe) @references @resources @sources (Join-Path $projectRoot 'tests\CharacterImportTest.cs')
    if ($LASTEXITCODE -ne 0) { throw 'Windows import test compilation failed.' }
    $examples = @()
    if ($SampleDirectory) { $examples = @('0fxxyxhc.zip', '7rkbhgkk.zip') | ForEach-Object { Join-Path $SampleDirectory $_ } }
    & $importExe @examples
    if ($LASTEXITCODE -ne 0) { throw 'Windows import tests failed.' }
    $settingsExe = Join-Path $testRoot 'SettingsScalingTest.exe'
    & $compiler /nologo /codepage:65001 /target:exe /main:SettingsScalingTest ('/out:' + $settingsExe) /r:System.Windows.Forms.dll @references @resources @sources (Join-Path $projectRoot 'ShimejiSettingsLauncher.cs') (Join-Path $projectRoot 'tests\SettingsScalingTest.cs')
    if ($LASTEXITCODE -ne 0) { throw 'Windows settings test compilation failed.' }
    & $settingsExe
    if ($LASTEXITCODE -ne 0) { throw 'Windows settings tests failed.' }
}
finally {
    Pop-Location
    $env:SHIMEJI_SAMPLE_DIR = $oldSampleDirectory
    $resolvedTestRoot = [IO.Path]::GetFullPath($testRoot)
    $expectedPrefix = [IO.Path]::GetFullPath([IO.Path]::GetTempPath()).TrimEnd('\') + '\shimeji-import-checks-'
    if (-not $resolvedTestRoot.StartsWith($expectedPrefix, [StringComparison]::OrdinalIgnoreCase)) { throw 'Unexpected test cleanup path.' }
    Remove-Item -LiteralPath $resolvedTestRoot -Recurse -Force
}
