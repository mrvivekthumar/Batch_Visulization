# 🚀 Database Batch Performance Analyzer

A **production-grade** Spring Boot application for analyzing and visualizing database batch operation performance. Compare single vs batch operations with real-time monitoring and beautiful charts.

![Java](https://img.shields.io/badge/Java-24-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.0-brightgreen)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)
![Docker](https://img.shields.io/badge/Docker-Compose-blue)

## 📋 Features

### 🔥 Core Performance Testing
- **Single vs Batch Operations**: Compare insertion/deletion performance
- **Configurable Batch Sizes**: Test with 1, 10, 100, 1000, 5000+ records per batch
- **Real-time Metrics**: CPU, Memory, Execution time monitoring
- **Smart Operations**: Automatically chooses single or batch based on batch size

### 📊 Advanced Monitoring
- **Prometheus Integration**: Production-grade metrics collection
- **Grafana Ready**: Export metrics for advanced visualization
- **JVM Monitoring**: Heap usage, GC metrics, thread analysis
- **Database Metrics**: Connection pool, query performance

### 🎯 Interactive Dashboard
- **Real-time Charts**: Execution time, throughput, memory usage
- **Performance Comparison**: Visual comparison of different batch sizes
- **System Statistics**: Live CPU, memory, database record counts
- **Test Results**: Detailed breakdown of each performance test

## 🏗️ Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Web Dashboard │────│  Spring Boot    │────│   PostgreSQL    │
│   (Chart.js)    │    │   REST API      │    │   Database      │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                               │
                       ┌───────┴───────┐
                       │               │
                ┌─────────────┐ ┌─────────────┐
                │ Prometheus  │ │   Grafana   │
                │  Metrics    │ │Visualization│
                └─────────────┘ └─────────────┘
```

## 🚀 Quick Start

### Prerequisites
- **Java 24** (or Java 17+)
- **Maven 3.6+**
- **Docker & Docker Compose**
- **Git**

### 1. Clone Repository
```bash
git clone <your-repo-url>
cd Batch-Operation-Visulization
```

### 2. Start Infrastructure
```bash
cd docker
docker-compose up -d
```

### 3. Run Application
```bash
mvn clean compile
mvn spring-boot:run
```

### 4. Access Dashboard
```bash
# Main Dashboard
http://localhost:8083

# API Health Check
http://localhost:8083/api/performance/health

# System Stats
http://localhost:8083/api/performance/stats/system
```

## 📊 Usage Guide

### Performance Testing Workflow

1. **Initialize Data**
   - Set total records (e.g., 1000)
   - Choose batch size (1 = one-by-one, 100 = batches of 100)
   - Click "📝 INSERT Test"

2. **Test Deletion Performance**
   - Set records to delete
   - Choose batch size
   - Click "🗑️ DELETE Test"

3. **Analyze Results**
   - View real-time charts
   - Compare different batch sizes
   - Check memory and CPU usage

### Example Test Scenarios

| Test Case | Records | Batch Size | Expected Result |
|-----------|---------|------------|-----------------|
| Single Insert | 1000 | 1 | Slow, high CPU per record |
| Batch Insert | 1000 | 100 | Fast, efficient memory usage |
| Single Delete | 1000 | 1 | Individual DELETE statements |
| Batch Delete | 1000 | 100 | Optimized batch DELETE |

## 🔧 Configuration

### Application Configuration
```yaml
# src/main/resources/application.yml
performance:
  test:
    total-records: 1000000
    batch-sizes: [1, 10, 100, 1000, 10000]

spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/performance_db
```

### Docker Services
- **PostgreSQL**: `localhost:5433`
- **Prometheus**: `localhost:9090`
- **Grafana**: `localhost:3000` (admin/admin)

## 📈 API Endpoints

### Core Operations
```bash
# Insert records with batch performance testing
POST /api/performance/initialize?totalRecords=1000&batchSize=100

# Delete records with batch performance testing  
POST /api/performance/delete?totalRecords=1000&batchSize=100

# Get system statistics
GET /api/performance/stats/system

# Get database statistics
GET /api/performance/stats/database
```

### Monitoring
```bash
# Prometheus metrics
GET /actuator/prometheus

# Application health
GET /actuator/health

# JVM metrics
GET /actuator/metrics
```

## 🏛️ Technology Stack

### Backend
- **Spring Boot 3.5.0** - Main framework
- **Spring Data JPA** - Database operations
- **HikariCP** - Connection pooling
- **Micrometer** - Metrics collection
- **PostgreSQL 16** - Primary database

### Monitoring
- **Prometheus** - Metrics storage
- **Grafana** - Advanced visualization
- **Spring Actuator** - Health checks

### Frontend
- **Chart.js 4.4.0** - Interactive charts
- **Vanilla JavaScript** - Dashboard functionality
- **Responsive CSS** - Modern UI design

### DevOps
- **Docker Compose** - Multi-container setup
- **Maven** - Build automation
- **JMH** - Micro-benchmarking

## 📊 Performance Insights

### Typical Results
- **Single Operations**: ~10-50 records/second
- **Batch Operations (100)**: ~1000-5000 records/second
- **Memory Usage**: 10-50MB per 10K records
- **Performance Improvement**: 90%+ with proper batching

### Key Metrics Tracked
- **Execution Time**: Total operation duration
- **Throughput**: Records processed per second
- **Memory Usage**: Heap consumption during operations
- **CPU Utilization**: Processor usage patterns
- **Database Connections**: Pool utilization

## 🔍 Troubleshooting

### Common Issues

**Port Conflicts**
```bash
# Change application port
server.port=8084
```

**Database Connection**
```bash
# Check PostgreSQL
docker-compose logs postgres

# Test connection
docker-compose exec postgres psql -U postgres -d performance_db
```

**Memory Issues**
```bash
# Increase JVM heap
java -Xmx2g -jar target/application.jar
```

## 🤝 Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push branch (`git push origin feature/amazing-feature`)
5. Open Pull Request

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👨‍💻 Author

**Vivek** - Database Performance Engineering

---

## 🎯 Interview Highlights

This project demonstrates:
- ✅ **Production-grade architecture** with monitoring
- ✅ **Performance optimization** techniques
- ✅ **Real-time data visualization**
- ✅ **Docker containerization**
- ✅ **REST API design**
- ✅ **Database optimization**
- ✅ **Modern web technologies**

**Perfect for showcasing backend development and performance engineering skills!**