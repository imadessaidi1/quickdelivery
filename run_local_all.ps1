# Script de lancement local stabilisé pour QuickDelivery
# Ports : Registry (8080), Config (8889), Auth (18443), Gateway (18081), Users (18082), Packages (18083), Front (8084)

Write-Host "Nettoyage des anciens processus Java..." -ForegroundColor Yellow
taskkill /F /IM java.exe /T 2>$null

Write-Host "`nVérification des ports et démarrage de l'environnement local (Mode Ultra-Léger)..." -ForegroundColor Cyan

function Check-Port($port) {
    return Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue
}

# Optimisation agressive pour machines saturées
$jvmArgs = "-Xmx192m -Xms64m -XX:CICompilerCount=2"

# 1. Registre Eureka
if (-not (Check-Port 8080)) {
    Write-Host "Lancement du Registre Eureka (8080)..."
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "mvn -pl quickdelivery-registy-server spring-boot:run `"-Dspring-boot.run.profiles=local`" `"-Dspring-boot.run.jvmArguments=$jvmArgs`""
    Start-Sleep -Seconds 12
}

# 2. Config Server
if (-not (Check-Port 8889)) {
    Write-Host "Lancement du Config Server (8889)..."
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "mvn -pl quickdemivery-config-server spring-boot:run `"-Dspring-boot.run.profiles=local`" `"-Dspring-boot.run.jvmArguments=$jvmArgs`""
    Start-Sleep -Seconds 10
}

# 3. Auth Server
if (-not (Check-Port 18443)) {
    Write-Host "Lancement de l'Auth Server (18443)..."
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "mvn -pl oauth-authorization-server spring-boot:run `"-Dspring-boot.run.profiles=local`" `"-Dspring-boot.run.jvmArguments=$jvmArgs`""
    Start-Sleep -Seconds 15
}

# 4. API Gateway
if (-not (Check-Port 18081)) {
    Write-Host "Lancement de l'API Gateway (18081)..."
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "mvn -pl quickdelivery-api-gateway spring-boot:run `"-Dspring-boot.run.profiles=local`" `"-Dspring-boot.run.jvmArguments=$jvmArgs`""
    Start-Sleep -Seconds 8
}

# 5. Business Services
if (-not (Check-Port 18082)) {
    Write-Host "Lancement de Users Service (18082)..."
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "mvn -pl quickdelivery-users spring-boot:run `"-Dspring-boot.run.profiles=local`" `"-Dspring-boot.run.jvmArguments=$jvmArgs`""
}

if (-not (Check-Port 18083)) {
    Write-Host "Lancement de Packages Service (18083)..."
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "mvn -pl quickdelivery-packages spring-boot:run `"-Dspring-boot.run.profiles=local`" `"-Dspring-boot.run.jvmArguments=$jvmArgs`""
}

# 6. Frontend
if (-not (Check-Port 8084)) {
    Write-Host "Lancement du Frontend (8084)..."
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd quickdelivery-googlemaps-front; npm run serve"
}

Write-Host "`nL'environnement est en cours de démarrage." -ForegroundColor Green
Write-Host "Eureka Dashboard : http://localhost:8080"
Write-Host "Frontend : https://localhost:8084"
