# Package Reorganization Script for SocializerAI Backend

$basePath = "src\main\java\com\SocializerAI"

Write-Host "Starting package reorganization..." -ForegroundColor Green

# Create new package structure
$packages = @("model", "dto", "service", "controller", "repository")
foreach ($pkg in $packages) {
    $path = Join-Path $basePath $pkg
    if (-not (Test-Path $path)) {
        New-Item -ItemType Directory -Path $path -Force | Out-Null
        Write-Host "Created: $pkg/" -ForegroundColor Yellow
    }
}

# Function to move and update imports
function Move-JavaFile {
    param($source, $dest, $type)
    
    if (Test-Path $source) {
        $fileName = Split-Path $source -Leaf
        $destPath = Join-Path $dest $fileName
        
        # Copy file (not move to preserve original)
        Copy-Item $source $destPath -Force
        
        # Read content
        $content = Get-Content $destPath -Raw
        
        # Update package declaration
        $newPackage = "package com.SocializerAI.$type;"
        $content = $content -replace "package com\.SocializerAI\.[^;]+;", $newPackage
        
        # Write updated content
        Set-Content $destPath $content -NoNewline
        
        Write-Host "  Moved: $fileName -> $type/" -ForegroundColor Cyan
        return $true
    }
    return $false
}

# Move Model files
Write-Host "`nMoving Model files..." -ForegroundColor Green
$modelFiles = @(
    "user\User.java",
    "user\UserPreferences.java",
    "user\UserEmotionalProfile.java",
    "admin\AdminUser.java",
    "chat\Conversation.java",
    "chat\Message.java",
    "emotion\model\EmotionalHistory.java",
    "emotion\model\EmotionalPattern.java",
    "recommend\model\WellbeingActivity.java",
    "report\model\Report.java",
    "report\model\ReportInsight.java",
    "chat\flow\model\FlowResponse.java",
    "chat\flow\model\MonitoringContext.java"
)

foreach ($file in $modelFiles) {
    Move-JavaFile (Join-Path $basePath $file) (Join-Path $basePath "model") "model"
}

# Move DTO files
Write-Host "`nMoving DTO files..." -ForegroundColor Green
$dtoFiles = @(
    "auth\dto\LoginRequest.java",
    "auth\dto\RegisterRequest.java",
    "auth\dto\JwtResponse.java",
    "chat\dto\MessageRequest.java",
    "report\dto\ReportRequest.java",
    "report\dto\ReportResponse.java",
    "emotion\dto\EmotionRequest.java",
    "emotion\dto\PatternDTO.java",
    "chat\flow\dto\ChatbotFlowRequest.java",
    "chat\flow\dto\ChatbotFlowResponseDTO.java"
)

foreach ($file in $dtoFiles) {
    Move-JavaFile (Join-Path $basePath $file) (Join-Path $basePath "dto") "dto"
}

# Move Service files
Write-Host "`nMoving Service files..." -ForegroundColor Green
$serviceFiles = @(
    "user\UserService.java",
    "user\UserPreferencesService.java",
    "user\UserEmotionalProfileService.java",
    "admin\service\AdminService.java",
    "auth\service\AuthService.java",
    "chat\service\ConversationService.java",
    "chat\service\MessageService.java",
    "emotion\service\EmotionService.java",
    "emotion\service\PatternService.java",
    "recommend\service\WellbeingService.java",
    "report\service\ReportService.java",
    "chat\flow\service\ChatbotFlowEngine.java",
    "chat\flow\service\GreetingService.java",
    "chat\flow\service\MonitoringAndScreeningService.java",
    "chat\flow\service\ResponseTemplateService.java",
    "chat\flow\service\LoopManager.java",
    "chat\flow\service\FlowWithAIService.java",
    "chat\flow\service\HuggingFaceResponseEnhancer.java",
    "chat\flow\service\EmotionAwarePersonalization.java"
)

foreach ($file in $serviceFiles) {
    Move-JavaFile (Join-Path $basePath $file) (Join-Path $basePath "service") "service"
}

# Move Controller files
Write-Host "`nMoving Controller files..." -ForegroundColor Green
$controllerFiles = @(
    "user\UserController.java",
    "user\UserPreferencesController.java",
    "user\UserEmotionalProfileController.java",
    "admin\controller\AdminController.java",
    "auth\controller\AuthController.java",
    "chat\controller\MessageController.java",
    "emotion\controller\EmotionController.java",
    "emotion\controller\PatternController.java",
    "recommend\controller\WellbeingController.java",
    "report\controller\ReportController.java",
    "chat\flow\controller\ChatbotFlowController.java",
    "chat\flow\controller\ChatbotFlowEnhancedController.java"
)

foreach ($file in $controllerFiles) {
    Move-JavaFile (Join-Path $basePath $file) (Join-Path $basePath "controller") "controller"
}

# Move Repository files
Write-Host "`nMoving Repository files..." -ForegroundColor Green
$repositoryFiles = @(
    "user\UserRepository.java",
    "user\UserPreferencesRepository.java",
    "user\UserEmotionalProfileRepository.java",
    "admin\repo\AdminRepository.java",
    "chat\ConversationRepository.java",
    "chat\MessageRepository.java",
    "emotion\repo\EmotionalHistoryRepository.java",
    "emotion\repo\EmotionalPatternRepository.java",
    "recommend\repo\WellbeingActivityRepository.java",
    "report\repo\ReportRepository.java",
    "report\repo\ReportInsightRepository.java"
)

foreach ($file in $repositoryFiles) {
    Move-JavaFile (Join-Path $basePath $file) (Join-Path $basePath "repository") "repository"
}

Write-Host "`n✅ Reorganization complete!" -ForegroundColor Green
Write-Host "`nNew structure:" -ForegroundColor Yellow
Write-Host "  com.SocializerAI.model       - All entity/model classes"
Write-Host "  com.SocializerAI.dto         - All DTOs"
Write-Host "  com.SocializerAI.service     - All service classes"
Write-Host "  com.SocializerAI.controller  - All controllers"
Write-Host "  com.SocializerAI.repository  - All repositories"
Write-Host "  com.SocializerAI.config      - Configuration (unchanged)"
Write-Host "  com.SocializerAI.common      - Common/Utility (unchanged)"

Write-Host "`n⚠️  Next steps:" -ForegroundColor Yellow
Write-Host "1. Update all import statements in files"
Write-Host "2. Remove old empty directories"
Write-Host "3. Run: mvn clean compile"
Write-Host "4. Fix any remaining import errors"
