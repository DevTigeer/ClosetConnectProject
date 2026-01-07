#!/bin/bash

# Deploy a single service to Cloud Run
# Usage: ./deploy-single-service.sh [segmentation|inpainting|tryon|worker]

set -e

# Configuration
PROJECT_ID="your-gcp-project-id"  # TODO: Replace with your GCP project ID
REGION="asia-northeast3"  # Seoul region

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Check arguments
if [ $# -eq 0 ]; then
    echo -e "${RED}Error: Please specify a service to deploy${NC}"
    echo "Usage: $0 [segmentation|inpainting|tryon|worker]"
    exit 1
fi

SERVICE=$1

# Set service configuration based on argument
case $SERVICE in
    segmentation)
        SERVICE_NAME="closetconnect-segmentation"
        DOCKERFILE="Dockerfile.segmentation"
        PORT="8002"
        ;;
    inpainting)
        SERVICE_NAME="closetconnect-inpainting"
        DOCKERFILE="Dockerfile.inpainting"
        PORT="8003"
        ;;
    tryon)
        SERVICE_NAME="closetconnect-tryon"
        DOCKERFILE="Dockerfile.tryon"
        PORT="5001"
        ;;
    worker)
        SERVICE_NAME="closetconnect-worker"
        DOCKERFILE="Dockerfile.worker"
        PORT="8080"
        echo -e "${YELLOW}Warning: Workers typically don't expose HTTP endpoints${NC}"
        echo -e "${YELLOW}Consider using Cloud Run Jobs instead${NC}"
        ;;
    *)
        echo -e "${RED}Error: Invalid service name${NC}"
        echo "Valid options: segmentation, inpainting, tryon, worker"
        exit 1
        ;;
esac

echo -e "${GREEN}================================${NC}"
echo -e "${GREEN}Deploying ${SERVICE_NAME}${NC}"
echo -e "${GREEN}================================${NC}\n"

# Set project
gcloud config set project ${PROJECT_ID}

# Deploy
gcloud run deploy ${SERVICE_NAME} \
    --source . \
    --platform managed \
    --region ${REGION} \
    --allow-unauthenticated \
    --memory 2Gi \
    --cpu 2 \
    --timeout 300 \
    --max-instances 10 \
    --port ${PORT} \
    --dockerfile ${DOCKERFILE}

# Get service URL
SERVICE_URL=$(gcloud run services describe ${SERVICE_NAME} \
    --platform managed \
    --region ${REGION} \
    --format 'value(status.url)')

echo -e "\n${GREEN}✓ ${SERVICE_NAME} deployed successfully!${NC}"
echo -e "${GREEN}  URL: ${SERVICE_URL}${NC}"
