#!/bin/bash

# CloudRun Worker 배포 스크립트
# CloudRun API들을 호출하는 경량 Worker를 배포합니다

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 설정
PROJECT_ID="your-project-id"  # 여기에 실제 프로젝트 ID 입력
REGION="asia-northeast3"
SERVICE_NAME="closetconnect-worker"
IMAGE_NAME="gcr.io/${PROJECT_ID}/${SERVICE_NAME}"

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}CloudRun Worker 배포${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# 프로젝트 ID 확인
if [ "$PROJECT_ID" = "your-project-id" ]; then
    echo -e "${RED}❌ 오류: PROJECT_ID를 설정해주세요${NC}"
    echo "   이 스크립트 파일을 열어 PROJECT_ID 변수를 수정하세요"
    exit 1
fi

# 환경 변수 확인
echo -e "${YELLOW}📋 환경 변수 확인${NC}"
read -p "RabbitMQ Host (Railway RabbitMQ 주소): " RABBITMQ_HOST
read -p "RabbitMQ Port (default: 5672): " RABBITMQ_PORT
RABBITMQ_PORT=${RABBITMQ_PORT:-5672}
read -p "RabbitMQ Username: " RABBITMQ_USERNAME
read -sp "RabbitMQ Password: " RABBITMQ_PASSWORD
echo ""
read -p "Segmentation API URL (https://...): " SEGMENTATION_API_URL
read -p "Inpainting API URL (https://...): " INPAINTING_API_URL
read -p "Google API Key (선택, Imagen 사용 시): " GOOGLE_API_KEY
echo ""

# Docker 이미지 빌드
echo -e "${GREEN}🔨 Docker 이미지 빌드 중...${NC}"
docker build -f Dockerfile.worker-cloudrun -t ${IMAGE_NAME}:latest .

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Docker 이미지 빌드 완료${NC}"
else
    echo -e "${RED}❌ Docker 이미지 빌드 실패${NC}"
    exit 1
fi

# Docker 이미지 푸시
echo -e "${GREEN}📤 Docker 이미지 푸시 중...${NC}"
docker push ${IMAGE_NAME}:latest

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Docker 이미지 푸시 완료${NC}"
else
    echo -e "${RED}❌ Docker 이미지 푸시 실패${NC}"
    exit 1
fi

# CloudRun에 배포
echo -e "${GREEN}🚀 CloudRun에 배포 중...${NC}"
gcloud run deploy ${SERVICE_NAME} \
  --image ${IMAGE_NAME}:latest \
  --region ${REGION} \
  --platform managed \
  --no-allow-unauthenticated \
  --memory 2Gi \
  --cpu 1 \
  --timeout 900 \
  --set-env-vars="RABBITMQ_HOST=${RABBITMQ_HOST},RABBITMQ_PORT=${RABBITMQ_PORT},RABBITMQ_USERNAME=${RABBITMQ_USERNAME},RABBITMQ_PASSWORD=${RABBITMQ_PASSWORD},SEGMENTATION_API_URL=${SEGMENTATION_API_URL},INPAINTING_API_URL=${INPAINTING_API_URL},GOOGLE_API_KEY=${GOOGLE_API_KEY}"

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ CloudRun 배포 완료!${NC}"
    echo ""
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}배포 정보${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo "서비스 이름: ${SERVICE_NAME}"
    echo "리전: ${REGION}"
    echo "이미지: ${IMAGE_NAME}:latest"
    echo ""
    echo -e "${YELLOW}⚠️  주의사항:${NC}"
    echo "1. Worker는 백그라운드에서 실행되므로 HTTP 엔드포인트가 없습니다"
    echo "2. CloudRun은 HTTP 요청이 없으면 자동으로 스케일 다운됩니다"
    echo "3. Worker가 지속적으로 실행되도록 min-instances를 설정하세요:"
    echo "   gcloud run services update ${SERVICE_NAME} --region ${REGION} --min-instances 1"
else
    echo -e "${RED}❌ CloudRun 배포 실패${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}다음 단계${NC}"
echo -e "${GREEN}========================================${NC}"
echo "1. Railway에서 RabbitMQ가 실행 중인지 확인"
echo "2. Railway Spring Boot의 환경 변수에 RabbitMQ 주소 확인"
echo "3. Vercel Frontend에서 옷 업로드 테스트"
echo ""
