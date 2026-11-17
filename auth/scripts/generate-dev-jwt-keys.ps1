param(
    [switch]$Force
)

$ErrorActionPreference = "Stop"

$secretsPath = Join-Path $PSScriptRoot "..\secrets"
$privateKeyPath = Join-Path $secretsPath "jwt-private.pem"
$publicKeyPath = Join-Path $secretsPath "jwt-public.pem"

if (((Test-Path -LiteralPath $privateKeyPath) -or (Test-Path -LiteralPath $publicKeyPath)) -and -not $Force) {
    throw "JWT key files already exist. Use -Force only if you intend to replace the local development key pair."
}

New-Item -ItemType Directory -Path $secretsPath -Force | Out-Null

$rsa = [System.Security.Cryptography.RSA]::Create(3072)
try {
    [System.IO.File]::WriteAllText(
        $privateKeyPath,
        $rsa.ExportPkcs8PrivateKeyPem(),
        [System.Text.Encoding]::ASCII
    )
    [System.IO.File]::WriteAllText(
        $publicKeyPath,
        $rsa.ExportSubjectPublicKeyInfoPem(),
        [System.Text.Encoding]::ASCII
    )
}
finally {
    $rsa.Dispose()
}

Write-Output "Created local JWT key files in $secretsPath. They are ignored by Git."
