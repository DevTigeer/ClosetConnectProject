#!/bin/bash

# Cloud Run Deployment Script for ClosetConnect AI Services
# This script deploys all AI services to Google Cloud Run

set -e  # Exit on error

# Configuration
PROJECT_ID="your-gcp-project-id"  # TODO: Replace with your GCP project ID
REGION="asia-northeast3"  # Seoul region (or use us-central1, asia-northeast1, etc.)
SERVICE_ACCOUNT=""  # Optional: service account email

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}================================${NC}"
echo -e "${GREEN}ClosetConnect AI Services Deployment${NC}"
echo -e "${GREEN}================================${NC}\n"

# Check if gcloud is installed
if ! command -v gcloud &> /dev/null; then
    echo -e "${RED}Error: gcloud CLI is not installed${NC}"
    echo "Please install it from: https://cloud.google.com/sdk/docs/install"
    exit 1
fi

# Check if user is logged in
if ! gcloud auth list --filter=status:ACTIVE --format="value(account)" &> /dev/null; then
    echo -e "${YELLOW}Please login to gcloud:${NC}"
    gcloud auth login
fi

# Set project
echo -e "${YELLOW}Setting GCP project to: ${PROJECT_ID}${NC}"
gcloud config set project ${PROJECT_ID}

# Enable required APIs
echo -e "\n${YELLOW}Enabling required APIs...${NC}"
gcloud services enable cloudbuild.googleapis.com
gcloud services enable run.googleapis.com
gcloud services enable artifactregistry.googleapis.com

# Function to deploy a service
deploy_service() {
    local SERVICE_NAME=$1
    local DOCKERFILE=$2
    local PORT=$3

    echo -e "\n${GREEN}================================${NC}"
    echo -e "${GREEN}Deploying ${SERVICE_NAME}${NC}"
    echo -e "${GREEN}================================${NC}\n"

    # Build and deploy with Cloud Build
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

    echo -e "${GREEN}✓ ${SERVICE_NAME} deployed successfully!${NC}"
    echo -e "${GREEN}  URL: ${SERVICE_URL}${NC}"
}

# Deploy each service
echo -e "\n${YELLOW}Starting deployment of all services...${NC}\n"

# 1. Segmentation API
deploy_service "closetconnect-segmentation" "Dockerfile.segmentation" "8002"

# 2. Inpainting API
deploy_service "closetconnect-inpainting" "Dockerfile.inpainting" "8003"

# 3. Try-on API
deploy_service "closetconnect-tryon" "Dockerfile.tryon" "5001"

# 4. Worker (if you want to deploy it)
# Note: Workers typically don't expose HTTP endpoints, so Cloud Run might not be ideal
# Consider using Cloud Run Jobs or Google Kubernetes Engine (GKE) for workers
# deploy_service "closetconnect-worker" "Dockerfile.worker" "8080"

echo -e "\n${GREEN}================================${NC}"
echo -e "${GREEN}Deployment Summary${NC}"
echo -e "${GREEN}================================${NC}\n"

# List all deployed services
gcloud run services list --platform managed --region ${REGION}

echo -e "\n${GREEN}All services deployed successfully!${NC}"
echo -e "${YELLOW}Note: Make sure to update your backend (Spring Boot) with the new service URLs${NC}"
