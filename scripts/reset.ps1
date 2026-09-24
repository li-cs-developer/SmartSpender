# SmartSpender — nuke everything including the database volume
Write-Host "This will DELETE ALL DATA (containers, volumes, images)." -ForegroundColor Red
$confirm = Read-Host "Type 'yes' to continue"
if ($confirm -ne 'yes') {
    Write-Host "Aborted." -ForegroundColor Yellow
    exit
}
docker compose down -v --rmi local
Write-Host "Everything wiped. Run .\scripts\up.ps1 to start fresh." -ForegroundColor Green
