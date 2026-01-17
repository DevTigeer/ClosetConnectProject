#!/bin/bash

# U2NET API 재배포 스크립트 (디버깅용 강화 설정)
# Enhanced settings for troubleshooting 503 errors

set -e

# 색상 정의
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

PROJECT_ID="your-project-id"  # TODO: 실제 프로젝트 ID로 변경
REGION="asia-northeast3"
SERVICE_NAME="closetconnect-u2net"

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}U2NET API 재배포 (디버깅 강화)${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# 프로젝트 ID 확인
if [ "$PROJECT_ID" = "your-project-id" ]; then
    echo -e "${YELLOW}⚠️  PROJECT_ID를 설정해주세요${NC}"
    read -p "GCP Project ID: " PROJECT_ID
fi

echo -e "${YELLOW}📋 배포 설정${NC}"
echo "  - Memory: 4Gi (모델 로드를 위해 증가)"
echo "  - CPU: 2"
echo "  - Timeout: 300s"
echo "  - Startup CPU Boost: Enabled (Cold Start 개선)"
echo "  - Min Instances: 1 (Cold Start 제거)"
echo ""

# CloudRun에 배포
echo -e "${GREEN}🚀 CloudRun에 배포 중...${NC}"
gcloud run deploy ${SERVICE_NAME} \
  --source . \
  --platform managed \
  --region ${REGION} \
  --allow-unauthenticated \
  --memory 4Gi \
  --cpu 2 \
  --timeout 300 \
  --startup-cpu-boost \
  --min-instances 1 \
  --max-instances 10 \
  --port 8004 \
  --dockerfile Dockerfile.u2net-api \
  --project ${PROJECT_ID}

if [ $? -eq 0 ]; then
    echo ""
    echo -e "${GREEN}✅ 재배포 완료!${NC}"
    echo ""

    # 서비스 URL 확인
    SERVICE_URL=$(gcloud run services describe ${SERVICE_NAME} \
        --platform managed \
        --region ${REGION} \
        --project ${PROJECT_ID} \
        --format 'value(status.url)')

    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}배포 정보${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo "서비스 URL: ${SERVICE_URL}"
    echo "리전: ${REGION}"
    echo ""

    echo -e "${YELLOW}📊 로그 확인 방법:${NC}"
    echo "gcloud run services logs read ${SERVICE_NAME} --region ${REGION} --project ${PROJECT_ID} --limit 100"
    echo ""

    echo -e "${YELLOW}🧪 테스트 방법:${NC}"
    echo "1. Health Check:"
    echo "   curl ${SERVICE_URL}/health"
    echo ""
    echo "2. 이미지 세그멘테이션 테스트:"
    echo "   curl -X POST ${SERVICE_URL}/segment -F 'file=@test_image.jpg'"
    echo ""

    echo -e "${YELLOW}🔍 예상되는 로그:${NC}"
    echo "  ✓ 🚀 U2NET API Startup Event Started"
    echo "  ✓ 📦 Initializing U2NetSegmentationModel..."
    echo "  ✓ ✓ Device: cpu"
    echo "  ✓ ✓ Checkpoint path: /app/model/cloth_segm.pth"
    echo "  ✓ ✓ Path exists: True"
    echo "  ✓ ✓ File size: 176.XX MB"
    echo "  ✓ ⏳ Loading U2NET model... (this may take 10-30 seconds)"
    echo "  ✓ ✅ U2NET model loaded successfully"
    echo ""
else
    echo -e "${RED}❌ 배포 실패${NC}"
    exit 1
fi

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}다음 단계${NC}"
echo -e "${GREEN}========================================${NC}"
echo "1. 로그를 확인하여 모델 로드 성공 여부 확인"
echo "2. Health Check 엔드포인트 테스트"
echo "3. Worker의 U2NET_API_URL 환경 변수 업데이트"
echo "4. 전체 플로우 테스트 (Vercel → Spring Boot → Worker → U2NET API)"
echo ""
