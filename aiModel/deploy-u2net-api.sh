#!/bin/bash

# U2NET API CloudRun 배포 스크립트
# SINGLE_ITEM 이미지 세그멘테이션을 위한 U2NET API를 배포합니다

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 설정
PROJECT_ID="your-project-id"  # TODO: 여기에 실제 GCP 프로젝트 ID 입력
REGION="asia-northeast3"      # 서울 리전
SERVICE_NAME="closetconnect-u2net-api"
PORT="8004"
DOCKERFILE="Dockerfile.u2net-api"

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}U2NET API CloudRun 배포${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# 프로젝트 ID 확인
if [ "$PROJECT_ID" = "your-project-id" ]; then
    echo -e "${RED}❌ 오류: PROJECT_ID를 설정해주세요${NC}"
    echo "   이 스크립트 파일을 열어 PROJECT_ID 변수를 수정하세요"
    exit 1
fi

# gcloud 설치 확인
if ! command -v gcloud &> /dev/null; then
    echo -e "${RED}❌ 오류: gcloud CLI가 설치되어 있지 않습니다${NC}"
    echo "   https://cloud.google.com/sdk/docs/install 에서 설치하세요"
    exit 1
fi

# 프로젝트 설정
echo -e "${YELLOW}📋 GCP 프로젝트 설정 중...${NC}"
gcloud config set project ${PROJECT_ID}

# API 활성화
echo -e "${YELLOW}🔌 필요한 API 활성화 중...${NC}"
gcloud services enable cloudbuild.googleapis.com
gcloud services enable run.googleapis.com
gcloud services enable artifactregistry.googleapis.com

# 배포
echo -e "\n${GREEN}🚀 U2NET API 배포 중...${NC}"
echo "   서비스명: ${SERVICE_NAME}"
echo "   리전: ${REGION}"
echo "   포트: ${PORT}"
echo "   Dockerfile: ${DOCKERFILE}"
echo ""

gcloud run deploy ${SERVICE_NAME} \
    --source . \
    --platform managed \
    --region ${REGION} \
    --allow-unauthenticated \
    --memory 2Gi \
    --cpu 2 \
    --timeout 180 \
    --max-instances 5 \
    --min-instances 0 \
    --port ${PORT} \
    --dockerfile ${DOCKERFILE}

# 배포 확인
if [ $? -eq 0 ]; then
    echo -e "\n${GREEN}✅ U2NET API 배포 완료!${NC}"

    # 서비스 URL 가져오기
    SERVICE_URL=$(gcloud run services describe ${SERVICE_NAME} \
        --platform managed \
        --region ${REGION} \
        --format 'value(status.url)')

    echo ""
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}배포 정보${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo "서비스 이름: ${SERVICE_NAME}"
    echo "리전: ${REGION}"
    echo "포트: ${PORT}"
    echo "URL: ${SERVICE_URL}"
    echo ""

    # 헬스체크
    echo -e "${YELLOW}🏥 헬스체크 중...${NC}"
    HEALTH_CHECK=$(curl -s "${SERVICE_URL}/health" | jq -r '.status' 2>/dev/null || echo "error")

    if [ "$HEALTH_CHECK" = "healthy" ]; then
        echo -e "${GREEN}✅ 헬스체크 성공!${NC}"
    else
        echo -e "${YELLOW}⚠️  헬스체크 실패 (모델 로딩 중일 수 있습니다)${NC}"
        echo "   1-2분 후에 다시 시도하세요: curl ${SERVICE_URL}/health"
    fi

    echo ""
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}다음 단계${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo ""
    echo "1. Worker에 U2NET API URL 환경변수 추가:"
    echo "   gcloud run services update closetconnect-worker \\"
    echo "     --region ${REGION} \\"
    echo "     --set-env-vars U2NET_API_URL=${SERVICE_URL}"
    echo ""
    echo "2. Worker 재배포 (환경변수 적용)"
    echo ""
    echo "3. 테스트 요청 보내기:"
    echo "   curl -X POST ${SERVICE_URL}/segment \\"
    echo "     -F 'file=@test_image.png'"
    echo ""

else
    echo -e "\n${RED}❌ 배포 실패${NC}"
    exit 1
fi
