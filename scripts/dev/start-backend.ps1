. "$PSScriptRoot/common.ps1"

$repoRoot = Get-RepoRoot
$backendDir = Join-Path $repoRoot "backend"
$wrapperCmd = Join-Path $backendDir "mvnw.cmd"
$m2Dir = Join-Path $HOME ".m2"

Set-Location $backendDir

$javaCmd = Get-Command java -ErrorAction SilentlyContinue
$mvnCmd = Get-Command mvn -ErrorAction SilentlyContinue
$dockerCmd = Get-Command docker -ErrorAction SilentlyContinue

if ($javaCmd -and (Test-Path $wrapperCmd)) {
  & $wrapperCmd -pl delivery-app -am -DskipTests package
  if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
  }

  & java -jar "delivery-app/target/delivery-app-1.0.0-SNAPSHOT.jar"
  exit $LASTEXITCODE
}

if ($javaCmd -and $mvnCmd) {
  & mvn -pl delivery-app -am -DskipTests package
  if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
  }

  & java -jar "delivery-app/target/delivery-app-1.0.0-SNAPSHOT.jar"
  exit $LASTEXITCODE
}

if (-not $dockerCmd) {
  throw "Neither Java 21 nor Docker Desktop is available. Install JDK 21 or start Docker Desktop first."
}

& docker run --rm `
  --name delivery-backend-dev `
  --network infra_default `
  -p 8080:8080 `
  -e DELIVERY_DB_URL="jdbc:mysql://mysql:3306/delivery_platform?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8" `
  -e DELIVERY_DB_USERNAME="delivery" `
  -e DELIVERY_DB_PASSWORD="delivery123" `
  -v "${m2Dir}:/root/.m2" `
  -v "${backendDir}:/workspace" `
  -w /workspace `
  maven:3.9-eclipse-temurin-21 `
  bash -lc 'mvn -pl delivery-app -am -DskipTests package && java -jar delivery-app/target/delivery-app-1.0.0-SNAPSHOT.jar'

exit $LASTEXITCODE
