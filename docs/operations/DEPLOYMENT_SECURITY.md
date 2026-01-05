# Deployment, Security & Environment Management

> MCP Reference Document for ClosetConnect deployment operations, security practices, and environment management

## 1. Environment Strategy

### 1.1 Environment Separation

ClosetConnect operates across three environments:

| Environment | Purpose | Data | Access |
|------------|---------|------|--------|
| **DEV** | Development & feature testing | Synthetic/anonymized | All developers |
| **STAGING** | Pre-production validation | Production-like synthetic | QA + Senior devs |
| **PRODUCTION** | Live user traffic | Real user data | Ops team + Emergency access |

### 1.2 Environment Configuration

**Environment Variables by Environment:**

```bash
# DEV Environment
SPRING_PROFILES_ACTIVE=dev
DATABASE_URL=jdbc:mariadb://localhost:3306/closetconnect_dev
JWT_SECRET=dev-secret-key-change-me
RABBITMQ_HOST=localhost
GOOGLE_AI_API_KEY=<dev-api-key>
TOSS_PAYMENTS_SECRET_KEY=<test-secret-key>
REDIS_HOST=localhost
LOG_LEVEL=DEBUG

# STAGING Environment
SPRING_PROFILES_ACTIVE=staging
DATABASE_URL=jdbc:mariadb://staging-db.internal:3306/closetconnect_staging
JWT_SECRET=<from-secrets-manager>
RABBITMQ_HOST=staging-rabbitmq.internal
GOOGLE_AI_API_KEY=<staging-api-key>
TOSS_PAYMENTS_SECRET_KEY=<test-secret-key>
REDIS_HOST=staging-redis.internal
LOG_LEVEL=INFO

# PRODUCTION Environment
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=jdbc:mariadb://prod-db-writer.internal:3306/closetconnect
JWT_SECRET=<from-secrets-manager>
RABBITMQ_HOST=prod-rabbitmq.internal
GOOGLE_AI_API_KEY=<prod-api-key>
TOSS_PAYMENTS_SECRET_KEY=<prod-secret-key>
REDIS_HOST=prod-redis.internal
LOG_LEVEL=WARN
```

### 1.3 Configuration Management

**Precedence Order (highest to lowest):**
1. Environment variables (runtime)
2. Secrets Manager (AWS Secrets Manager, HashiCorp Vault)
3. application-{profile}.properties
4. application.properties

**Sensitive Configuration:**
- Never commit secrets to git
- Use `.env.example` for local development templates
- Store production secrets in AWS Secrets Manager or HashiCorp Vault
- Rotate secrets quarterly (critical secrets monthly)

## 2. Secret Management

### 2.1 Secret Categories

| Category | Examples | Rotation Frequency | Storage |
|----------|----------|-------------------|---------|
| **Database Credentials** | MariaDB password | 90 days | Secrets Manager |
| **API Keys** | Google AI, Toss Payments | 180 days | Secrets Manager |
| **JWT Signing Keys** | jwt.secret | 90 days | Secrets Manager |
| **Service Account Keys** | RabbitMQ admin | 90 days | Secrets Manager |
| **TLS Certificates** | HTTPS certs | 365 days (auto-renew) | Certificate Manager |

### 2.2 AWS Secrets Manager Integration

**Secret Naming Convention:**
```
/{environment}/{service}/{secret-name}

Examples:
/prod/closetconnect/database-password
/prod/closetconnect/jwt-secret
/prod/closetconnect/google-ai-api-key
/staging/closetconnect/toss-payments-secret
```

**Access Pattern (Java):**
```java
// Use AWS SDK to fetch secrets at application startup
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

public class SecretManagerConfig {
    private final SecretsManagerClient secretsClient;

    public String getSecret(String secretName) {
        GetSecretValueRequest request = GetSecretValueRequest.builder()
            .secretId(secretName)
            .build();
        return secretsClient.getSecretValue(request).secretString();
    }
}

// In application startup:
String dbPassword = secretManager.getSecret("/prod/closetconnect/database-password");
String jwtSecret = secretManager.getSecret("/prod/closetconnect/jwt-secret");
```

### 2.3 Secret Rotation Procedures

**Quarterly Rotation Process:**

1. **Week 1: Generate New Secret**
   ```bash
   # Generate new JWT secret
   openssl rand -base64 32

   # Update in Secrets Manager
   aws secretsmanager update-secret \
     --secret-id /prod/closetconnect/jwt-secret \
     --secret-string "new-secret-value"
   ```

2. **Week 2: Deploy with Dual-Key Support**
   - Application validates tokens with both old and new keys
   - New tokens issued with new key

3. **Week 3: Monitor & Validate**
   - Verify all services using new key
   - Check error rates for authentication failures

4. **Week 4: Remove Old Key**
   - Deploy version removing old key validation
   - Archive old secret with 30-day retention

**Emergency Rotation (Security Breach):**
- Immediate rotation within 1 hour
- Force re-authentication of all users
- Audit logs for compromise period

### 2.4 Local Development Secrets

**`.env` File (gitignored):**
```bash
# Database
DB_PASSWORD=root1234

# JWT
JWT_SECRET=local-dev-secret-not-for-production

# External APIs
GOOGLE_AI_API_KEY=your-dev-api-key-here
TOSS_PAYMENTS_SECRET_KEY=test_sk_xxxxxxxxxxxx

# RabbitMQ
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest
```

**`.env.example` (committed to git):**
```bash
# Database
DB_PASSWORD=your-database-password

# JWT (generate with: openssl rand -base64 32)
JWT_SECRET=your-jwt-secret-key

# External APIs (get from Google AI Studio)
GOOGLE_AI_API_KEY=your-google-api-key

# Toss Payments (get from Toss Developers)
TOSS_PAYMENTS_SECRET_KEY=test_sk_your_key_here

# RabbitMQ
RABBITMQ_USERNAME=your-rabbitmq-username
RABBITMQ_PASSWORD=your-rabbitmq-password
```

## 3. CI/CD Pipeline

### 3.1 Pipeline Architecture

```
┌─────────────┐     ┌──────────────┐     ┌─────────────┐     ┌────────────┐
│   Commit    │────▶│  Build & Test│────▶│   Deploy    │────▶│  Smoke     │
│   to main   │     │   (CI)       │     │  (Staging)  │     │   Tests    │
└─────────────┘     └──────────────┘     └─────────────┘     └────────────┘
                           │                     │                   │
                           │ Pass                │ Pass              │ Pass
                           ▼                     ▼                   ▼
                    ┌──────────────┐     ┌─────────────┐     ┌────────────┐
                    │ Quality Gates│     │   Deploy    │     │  Verify    │
                    │ (Coverage,   │     │ (Production)│     │ Production │
                    │  Security)   │     │             │     │            │
                    └──────────────┘     └─────────────┘     └────────────┘
```

### 3.2 GitHub Actions CI Pipeline

**`.github/workflows/ci.yml`:**
```yaml
name: CI Pipeline

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

env:
  JAVA_VERSION: '17'
  PYTHON_VERSION: '3.10'

jobs:
  test-backend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Cache Gradle packages
        uses: actions/cache@v3
        with:
          path: ~/.gradle/caches
          key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*') }}

      - name: Run tests with coverage
        run: ./gradlew test jacocoTestReport

      - name: Check test coverage
        run: |
          coverage=$(grep -oP 'Total.*?(\d+)%' build/reports/jacoco/test/html/index.html | grep -oP '\d+')
          if [ "$coverage" -lt 70 ]; then
            echo "Coverage $coverage% is below 70% threshold"
            exit 1
          fi

      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v3

  test-ai-services:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Set up Python
        uses: actions/setup-python@v4
        with:
          python-version: '3.10'

      - name: Install dependencies
        run: |
          cd aiModel
          pip install -r requirements.txt
          pip install pytest pytest-cov

      - name: Run Python tests
        run: |
          cd aiModel
          pytest tests/ --cov=src --cov-report=xml

      - name: Check Python coverage
        run: |
          cd aiModel
          coverage report --fail-under=60

  security-scan:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Run OWASP Dependency Check (Java)
        run: ./gradlew dependencyCheckAnalyze

      - name: Run Snyk security scan
        uses: snyk/actions/gradle@master
        env:
          SNYK_TOKEN: ${{ secrets.SNYK_TOKEN }}

      - name: Python security scan
        run: |
          pip install safety bandit
          cd aiModel
          safety check -r requirements.txt
          bandit -r src/

  build-docker:
    needs: [test-backend, test-ai-services, security-scan]
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    steps:
      - uses: actions/checkout@v3

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v2

      - name: Login to Amazon ECR
        uses: aws-actions/amazon-ecr-login@v1

      - name: Build and push backend image
        run: |
          docker build -t closetconnect-backend:${{ github.sha }} .
          docker tag closetconnect-backend:${{ github.sha }} \
            ${{ secrets.ECR_REGISTRY }}/closetconnect-backend:latest
          docker push ${{ secrets.ECR_REGISTRY }}/closetconnect-backend:latest

      - name: Build and push AI services image
        run: |
          cd aiModel
          docker build -t closetconnect-ai:${{ github.sha }} .
          docker tag closetconnect-ai:${{ github.sha }} \
            ${{ secrets.ECR_REGISTRY }}/closetconnect-ai:latest
          docker push ${{ secrets.ECR_REGISTRY }}/closetconnect-ai:latest
```

### 3.3 CD Pipeline (Staging)

**`.github/workflows/cd-staging.yml`:**
```yaml
name: Deploy to Staging

on:
  workflow_run:
    workflows: ["CI Pipeline"]
    types:
      - completed
    branches: [main]

jobs:
  deploy-staging:
    runs-on: ubuntu-latest
    if: ${{ github.event.workflow_run.conclusion == 'success' }}
    steps:
      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v2
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: ap-northeast-2

      - name: Deploy to ECS (Staging)
        run: |
          aws ecs update-service \
            --cluster closetconnect-staging \
            --service backend \
            --force-new-deployment

          aws ecs update-service \
            --cluster closetconnect-staging \
            --service ai-services \
            --force-new-deployment

      - name: Wait for deployment
        run: |
          aws ecs wait services-stable \
            --cluster closetconnect-staging \
            --services backend ai-services

      - name: Run smoke tests
        run: |
          curl -f https://staging.closetconnect.com/health || exit 1
          curl -f https://staging.closetconnect.com/api/v1/health || exit 1

      - name: Notify Slack
        if: always()
        uses: slackapi/slack-github-action@v1
        with:
          payload: |
            {
              "text": "Staging Deployment ${{ job.status }}",
              "blocks": [
                {
                  "type": "section",
                  "text": {
                    "type": "mrkdwn",
                    "text": "Staging deployment *${{ job.status }}*\nCommit: ${{ github.sha }}"
                  }
                }
              ]
            }
```

### 3.4 CD Pipeline (Production)

**Manual Approval Required:**

```yaml
name: Deploy to Production

on:
  workflow_dispatch:
    inputs:
      version:
        description: 'Version to deploy (git tag or commit SHA)'
        required: true

jobs:
  deploy-production:
    runs-on: ubuntu-latest
    environment:
      name: production
      url: https://closetconnect.com
    steps:
      - name: Checkout specific version
        uses: actions/checkout@v3
        with:
          ref: ${{ github.event.inputs.version }}

      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v2
        with:
          aws-access-key-id: ${{ secrets.PROD_AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.PROD_AWS_SECRET_ACCESS_KEY }}
          aws-region: ap-northeast-2

      - name: Create deployment record
        run: |
          echo "Deploying version ${{ github.event.inputs.version }}" >> deployments.log
          git tag -a "deploy-$(date +%Y%m%d-%H%M%S)" -m "Production deployment"

      - name: Blue-Green Deployment
        run: |
          # Deploy to green environment
          aws ecs update-service \
            --cluster closetconnect-prod \
            --service backend-green \
            --force-new-deployment

          # Wait for green to be healthy
          aws ecs wait services-stable \
            --cluster closetconnect-prod \
            --services backend-green

          # Run health checks on green
          for i in {1..10}; do
            curl -f https://green.closetconnect.com/health && break
            sleep 10
          done

          # Switch load balancer to green
          aws elbv2 modify-rule \
            --rule-arn ${{ secrets.LB_RULE_ARN }} \
            --actions Type=forward,TargetGroupArn=${{ secrets.GREEN_TG_ARN }}

          # Monitor for 10 minutes
          sleep 600

          # If no errors, scale down blue
          aws ecs update-service \
            --cluster closetconnect-prod \
            --service backend-blue \
            --desired-count 1

      - name: Rollback on failure
        if: failure()
        run: |
          # Switch back to blue
          aws elbv2 modify-rule \
            --rule-arn ${{ secrets.LB_RULE_ARN }} \
            --actions Type=forward,TargetGroupArn=${{ secrets.BLUE_TG_ARN }}

          # Notify on-call
          curl -X POST ${{ secrets.PAGERDUTY_WEBHOOK }} \
            -d '{"incident_key":"deploy-failure","description":"Production deployment failed"}'
```

## 4. Containerization

### 4.1 Backend Dockerfile

**`Dockerfile` (Spring Boot):**
```dockerfile
# Multi-stage build for smaller image size
FROM gradle:8.5-jdk17 AS build
WORKDIR /app

# Copy only dependency files first (cache layer)
COPY build.gradle settings.gradle ./
COPY gradle gradle
RUN gradle dependencies --no-daemon

# Copy source and build
COPY src src
RUN gradle bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Add non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy JAR from build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/health || exit 1

# Expose port
EXPOSE 8080

# Run with production-ready JVM options
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+UseG1GC", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
```

### 4.2 AI Services Dockerfile

**`aiModel/Dockerfile`:**
```dockerfile
FROM python:3.10-slim
WORKDIR /app

# Install system dependencies
RUN apt-get update && apt-get install -y \
    gcc \
    g++ \
    libglib2.0-0 \
    libsm6 \
    libxext6 \
    libxrender-dev \
    libgomp1 \
    && rm -rf /var/lib/apt/lists/*

# Create non-root user
RUN useradd -m -u 1000 aiuser && \
    chown -R aiuser:aiuser /app
USER aiuser

# Install Python dependencies
COPY --chown=aiuser:aiuser requirements.txt .
RUN pip install --no-cache-dir --user -r requirements.txt

# Copy application code
COPY --chown=aiuser:aiuser . .

# Download models at build time (cache layer)
RUN python -c "from transformers import AutoImageProcessor, AutoModelForSemanticSegmentation; \
  AutoImageProcessor.from_pretrained('mattmdjaga/segformer_b2_clothes'); \
  AutoModelForSemanticSegmentation.from_pretrained('mattmdjaga/segformer_b2_clothes')"

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD python -c "import requests; requests.get('http://localhost:8002/health')" || exit 1

EXPOSE 8002

# Start all servers
CMD ["python", "start_all_servers.py"]
```

### 4.3 Docker Compose (Local Development)

**`docker-compose.yml`:**
```yaml
version: '3.8'

services:
  mariadb:
    image: mariadb:10.11
    environment:
      MYSQL_ROOT_PASSWORD: root1234
      MYSQL_DATABASE: closetConnectProject
    ports:
      - "3306:3306"
    volumes:
      - mariadb_data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5

  rabbitmq:
    image: rabbitmq:3-management
    environment:
      RABBITMQ_DEFAULT_USER: guest
      RABBITMQ_DEFAULT_PASS: guest
    ports:
      - "5672:5672"
      - "15672:15672"
    healthcheck:
      test: ["CMD", "rabbitmq-diagnostics", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  backend:
    build: .
    depends_on:
      mariadb:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
      redis:
        condition: service_healthy
    environment:
      SPRING_PROFILES_ACTIVE: docker
      DATABASE_URL: jdbc:mariadb://mariadb:3306/closetConnectProject
      RABBITMQ_HOST: rabbitmq
      REDIS_HOST: redis
    env_file:
      - .env
    ports:
      - "8080:8080"
    healthcheck:
      test: ["CMD", "wget", "--spider", "http://localhost:8080/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  ai-services:
    build: ./aiModel
    depends_on:
      rabbitmq:
        condition: service_healthy
    environment:
      RABBITMQ_HOST: rabbitmq
    env_file:
      - aiModel/.env
    ports:
      - "8002:8002"
      - "8003:8003"
      - "5001:5001"
    volumes:
      - ai_outputs:/app/outputs
    deploy:
      resources:
        reservations:
          devices:
            - driver: nvidia
              count: 1
              capabilities: [gpu]

volumes:
  mariadb_data:
  ai_outputs:
```

## 5. Infrastructure as Code

### 5.1 Terraform Configuration

**`infrastructure/main.tf`:**
```hcl
terraform {
  required_version = ">= 1.0"

  backend "s3" {
    bucket = "closetconnect-terraform-state"
    key    = "prod/terraform.tfstate"
    region = "ap-northeast-2"
    dynamodb_table = "terraform-lock"
    encrypt = true
  }

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = "ClosetConnect"
      Environment = var.environment
      ManagedBy   = "Terraform"
    }
  }
}

# VPC Configuration
module "vpc" {
  source = "./modules/vpc"

  vpc_cidr             = "10.0.0.0/16"
  availability_zones   = ["ap-northeast-2a", "ap-northeast-2c"]
  public_subnet_cidrs  = ["10.0.1.0/24", "10.0.2.0/24"]
  private_subnet_cidrs = ["10.0.11.0/24", "10.0.12.0/24"]

  enable_nat_gateway = true
  enable_vpn_gateway = false
}

# ECS Cluster
resource "aws_ecs_cluster" "main" {
  name = "closetconnect-${var.environment}"

  setting {
    name  = "containerInsights"
    value = "enabled"
  }
}

# RDS (MariaDB)
module "database" {
  source = "./modules/rds"

  identifier     = "closetconnect-${var.environment}"
  engine_version = "10.11"
  instance_class = var.db_instance_class

  allocated_storage     = 100
  max_allocated_storage = 1000

  database_name = "closetConnectProject"
  username      = "admin"
  password      = data.aws_secretsmanager_secret_version.db_password.secret_string

  multi_az               = var.environment == "prod" ? true : false
  backup_retention_period = var.environment == "prod" ? 30 : 7

  vpc_id             = module.vpc.vpc_id
  subnet_ids         = module.vpc.private_subnet_ids
  security_group_ids = [aws_security_group.database.id]
}

# ElastiCache (Redis)
resource "aws_elasticache_cluster" "redis" {
  cluster_id           = "closetconnect-${var.environment}"
  engine               = "redis"
  node_type            = var.redis_node_type
  num_cache_nodes      = 1
  parameter_group_name = "default.redis7"
  port                 = 6379

  subnet_group_name  = aws_elasticache_subnet_group.redis.name
  security_group_ids = [aws_security_group.redis.id]
}

# Application Load Balancer
resource "aws_lb" "main" {
  name               = "closetconnect-${var.environment}"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb.id]
  subnets            = module.vpc.public_subnet_ids

  enable_deletion_protection = var.environment == "prod" ? true : false

  access_logs {
    bucket  = aws_s3_bucket.alb_logs.id
    enabled = true
  }
}

# WAF (Web Application Firewall)
resource "aws_wafv2_web_acl" "main" {
  name  = "closetconnect-${var.environment}"
  scope = "REGIONAL"

  default_action {
    allow {}
  }

  rule {
    name     = "RateLimitRule"
    priority = 1

    action {
      block {}
    }

    statement {
      rate_based_statement {
        limit              = 2000
        aggregate_key_type = "IP"
      }
    }

    visibility_config {
      cloudwatch_metrics_enabled = true
      metric_name                = "RateLimitRule"
      sampled_requests_enabled   = true
    }
  }

  visibility_config {
    cloudwatch_metrics_enabled = true
    metric_name                = "closetconnect-waf"
    sampled_requests_enabled   = true
  }
}
```

### 5.2 Kubernetes Deployment

**`k8s/backend-deployment.yaml`:**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: closetconnect-backend
  namespace: closetconnect
  labels:
    app: backend
    version: v1
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  selector:
    matchLabels:
      app: backend
  template:
    metadata:
      labels:
        app: backend
        version: v1
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "8080"
        prometheus.io/path: "/actuator/prometheus"
    spec:
      serviceAccountName: closetconnect-backend
      containers:
      - name: backend
        image: ${ECR_REGISTRY}/closetconnect-backend:${IMAGE_TAG}
        imagePullPolicy: Always
        ports:
        - containerPort: 8080
          name: http
          protocol: TCP
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: DATABASE_URL
          valueFrom:
            secretKeyRef:
              name: database-credentials
              key: url
        - name: DATABASE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: database-credentials
              key: password
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: jwt-credentials
              key: secret
        - name: RABBITMQ_HOST
          value: "rabbitmq-service"
        - name: REDIS_HOST
          value: "redis-service"
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
          timeoutSeconds: 3
          failureThreshold: 2
        lifecycle:
          preStop:
            exec:
              command: ["/bin/sh", "-c", "sleep 15"]
---
apiVersion: v1
kind: Service
metadata:
  name: backend-service
  namespace: closetconnect
spec:
  type: ClusterIP
  ports:
  - port: 8080
    targetPort: 8080
    protocol: TCP
    name: http
  selector:
    app: backend
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: backend-hpa
  namespace: closetconnect
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: closetconnect-backend
  minReplicas: 3
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
      - type: Percent
        value: 50
        periodSeconds: 60
    scaleUp:
      stabilizationWindowSeconds: 0
      policies:
      - type: Percent
        value: 100
        periodSeconds: 15
      - type: Pods
        value: 2
        periodSeconds: 15
      selectPolicy: Max
```

## 6. Security Practices

### 6.1 Security Scanning

**Dependency Scanning:**
```bash
# Java dependencies (OWASP Dependency Check)
./gradlew dependencyCheckAnalyze

# Python dependencies
pip install safety
safety check -r aiModel/requirements.txt

# Docker image scanning
docker scan closetconnect-backend:latest
trivy image closetconnect-backend:latest
```

### 6.2 Network Security

**Security Group Rules (AWS):**

```hcl
# ALB Security Group
resource "aws_security_group" "alb" {
  name        = "closetconnect-alb-${var.environment}"
  description = "Security group for ALB"
  vpc_id      = module.vpc.vpc_id

  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "HTTPS from internet"
  }

  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "HTTP redirect to HTTPS"
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# Backend Service Security Group
resource "aws_security_group" "backend" {
  name        = "closetconnect-backend-${var.environment}"
  description = "Security group for backend services"
  vpc_id      = module.vpc.vpc_id

  ingress {
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
    description     = "HTTP from ALB"
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# Database Security Group
resource "aws_security_group" "database" {
  name        = "closetconnect-db-${var.environment}"
  description = "Security group for database"
  vpc_id      = module.vpc.vpc_id

  ingress {
    from_port       = 3306
    to_port         = 3306
    protocol        = "tcp"
    security_groups = [aws_security_group.backend.id]
    description     = "MariaDB from backend"
  }
}
```

### 6.3 TLS/SSL Configuration

**Certificate Management (AWS ACM):**
```hcl
resource "aws_acm_certificate" "main" {
  domain_name       = "closetconnect.com"
  validation_method = "DNS"

  subject_alternative_names = [
    "*.closetconnect.com"
  ]

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_lb_listener" "https" {
  load_balancer_arn = aws_lb.main.arn
  port              = "443"
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-TLS-1-2-2017-01"
  certificate_arn   = aws_acm_certificate.main.arn

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.backend.arn
  }
}
```

### 6.4 Audit Logging

**CloudTrail Configuration:**
```hcl
resource "aws_cloudtrail" "main" {
  name                          = "closetconnect-${var.environment}"
  s3_bucket_name                = aws_s3_bucket.cloudtrail.id
  include_global_service_events = true
  is_multi_region_trail         = true
  enable_log_file_validation    = true

  event_selector {
    read_write_type           = "All"
    include_management_events = true

    data_resource {
      type   = "AWS::S3::Object"
      values = ["${aws_s3_bucket.uploads.arn}/"]
    }
  }
}
```

## 7. Disaster Recovery

### 7.1 Backup Strategy

| Component | Frequency | Retention | Recovery Time |
|-----------|-----------|-----------|---------------|
| **Database** | Daily full + hourly incremental | 30 days | 1 hour |
| **User Uploads** | Real-time replication | 90 days | Immediate |
| **Application Config** | On change | Indefinite (git) | Immediate |
| **Secrets** | On rotation | 90 days | Immediate |

### 7.2 Database Backup

**Automated RDS Backups:**
```hcl
resource "aws_db_instance" "main" {
  # ... other config ...

  backup_retention_period = 30
  backup_window           = "03:00-04:00"  # UTC
  maintenance_window      = "mon:04:00-mon:05:00"

  enabled_cloudwatch_logs_exports = ["error", "slowquery"]

  copy_tags_to_snapshot = true

  final_snapshot_identifier = "closetconnect-final-${timestamp()}"
  skip_final_snapshot       = false
}

# Manual snapshot before major changes
resource "null_resource" "manual_snapshot" {
  triggers = {
    deployment_id = var.deployment_id
  }

  provisioner "local-exec" {
    command = <<EOF
      aws rds create-db-snapshot \
        --db-instance-identifier ${aws_db_instance.main.id} \
        --db-snapshot-identifier manual-${var.deployment_id}
    EOF
  }
}
```

### 7.3 Disaster Recovery Procedures

**RTO (Recovery Time Objective): 1 hour**
**RPO (Recovery Point Objective): 15 minutes**

**Recovery Steps:**

1. **Database Failure:**
   ```bash
   # Identify latest snapshot
   aws rds describe-db-snapshots \
     --db-instance-identifier closetconnect-prod \
     --query 'reverse(sort_by(DBSnapshots, &SnapshotCreateTime))[0]'

   # Restore from snapshot
   aws rds restore-db-instance-from-db-snapshot \
     --db-instance-identifier closetconnect-prod-restored \
     --db-snapshot-identifier <snapshot-id>

   # Update DNS/connection string
   ```

2. **Application Failure:**
   ```bash
   # Rollback to previous version
   kubectl rollout undo deployment/closetconnect-backend -n closetconnect

   # Or deploy specific version
   kubectl set image deployment/closetconnect-backend \
     backend=${ECR_REGISTRY}/closetconnect-backend:v1.2.3
   ```

3. **Region Failure (Multi-Region DR):**
   ```bash
   # Failover to DR region
   aws route53 change-resource-record-sets \
     --hosted-zone-id Z1234567890ABC \
     --change-batch file://failover-dns.json

   # Promote read replica to primary
   aws rds promote-read-replica \
     --db-instance-identifier closetconnect-prod-dr
   ```

## 8. Compliance & Data Privacy

### 8.1 GDPR Compliance

**Data Subject Rights:**
- Right to access: `/api/v1/users/me/data-export`
- Right to deletion: `/api/v1/users/me/delete-account`
- Data retention: 2 years after last login
- Cookie consent: Required for non-essential cookies

**Data Processing Locations:**
- Primary: AWS Seoul (ap-northeast-2)
- Backup: AWS Tokyo (ap-northeast-1)
- No data transfer outside Korea/Japan

### 8.2 PII Data Handling

**Encryption at Rest:**
- Database: AES-256 encryption enabled
- S3 Uploads: SSE-S3 encryption
- EBS Volumes: KMS encryption

**Encryption in Transit:**
- TLS 1.2+ for all external communication
- mTLS for internal service communication

**Data Masking in Logs:**
```java
// LoggingFilter.java
private String maskSensitiveData(String data) {
    return data
        .replaceAll("(\"email\":\")[^\"]+", "$1***@***.com")
        .replaceAll("(\"userId\":)(\\d{3})\\d+", "$1***$2")
        .replaceAll("(\"password\":\"[^\"]+)", "\"password\":\"***\"");
}
```

---

**Document Version**: 1.0
**Last Updated**: 2025-12-31
**Next Review**: 2026-03-31
