param(
    [int]$DishId = 1,
    [int]$Requests = 50,
    [string]$BaseUrl = "http://localhost:8080"
)

Add-Type -AssemblyName System.Net.Http

$client = New-Object System.Net.Http.HttpClient
$client.Timeout = [TimeSpan]::FromSeconds(120)
$body = '{"dishId": ' + $DishId + ', "quantity": 1}'

$tasks = 1..$Requests | ForEach-Object {
    $content = New-Object System.Net.Http.StringContent($body, [System.Text.Encoding]::UTF8, "application/json")
    $client.PostAsync("$BaseUrl/orders", $content)
}

[System.Threading.Tasks.Task]::WaitAll([System.Threading.Tasks.Task[]]$tasks)

Write-Host "HTTP status distribution for $Requests simultaneous requests:"
$tasks | ForEach-Object { [int]$_.Result.StatusCode } | Group-Object | Sort-Object Name | ForEach-Object {
    Write-Host ("  {0} -> {1}" -f $_.Name, $_.Count)
}

$dish = Invoke-RestMethod "$BaseUrl/dishes/$DishId"
Write-Host ("Final stock of '{0}': {1}" -f $dish.name, $dish.stock)
