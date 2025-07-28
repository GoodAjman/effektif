#!/bin/bash

# Effektif Integration Platform 2.0 启动脚本
# 用于快速启动集成平台和相关依赖服务

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_debug() {
    echo -e "${BLUE}[DEBUG]${NC} $1"
}

# 检查命令是否存在
check_command() {
    if ! command -v $1 &> /dev/null; then
        log_error "$1 命令未找到，请先安装 $1"
        exit 1
    fi
}

# 检查端口是否被占用
check_port() {
    if lsof -Pi :$1 -sTCP:LISTEN -t >/dev/null ; then
        log_warn "端口 $1 已被占用"
        return 1
    fi
    return 0
}

# 等待服务启动
wait_for_service() {
    local host=$1
    local port=$2
    local service_name=$3
    local max_attempts=30
    local attempt=1

    log_info "等待 $service_name 服务启动..."
    
    while [ $attempt -le $max_attempts ]; do
        if nc -z $host $port 2>/dev/null; then
            log_info "$service_name 服务已启动"
            return 0
        fi
        
        log_debug "尝试连接 $service_name ($attempt/$max_attempts)..."
        sleep 2
        ((attempt++))
    done
    
    log_error "$service_name 服务启动失败或超时"
    return 1
}

# 启动MySQL数据库
start_mysql() {
    log_info "启动MySQL数据库..."
    
    if ! docker ps | grep -q effektif-mysql; then
        docker run -d \
            --name effektif-mysql \
            -e MYSQL_ROOT_PASSWORD=root123 \
            -e MYSQL_DATABASE=effektif_integration \
            -e MYSQL_USER=effektif \
            -e MYSQL_PASSWORD=effektif123 \
            -p 3306:3306 \
            -v effektif_mysql_data:/var/lib/mysql \
            mysql:8.0
        
        wait_for_service localhost 3306 "MySQL"
        
        # 等待MySQL完全启动
        sleep 10
        
        # 执行初始化脚本
        if [ -f "src/main/resources/db/migration/V1__Create_trigger_tables.sql" ]; then
            log_info "执行数据库初始化脚本..."
            docker exec -i effektif-mysql mysql -ueffektif -peffektif123 effektif_integration < src/main/resources/db/migration/V1__Create_trigger_tables.sql
        fi
    else
        log_info "MySQL容器已存在，跳过启动"
    fi
}

# 启动RabbitMQ
start_rabbitmq() {
    log_info "启动RabbitMQ消息队列..."
    
    if ! docker ps | grep -q effektif-rabbitmq; then
        docker run -d \
            --name effektif-rabbitmq \
            -e RABBITMQ_DEFAULT_USER=effektif \
            -e RABBITMQ_DEFAULT_PASS=effektif123 \
            -p 5672:5672 \
            -p 15672:15672 \
            -v effektif_rabbitmq_data:/var/lib/rabbitmq \
            rabbitmq:3.8-management
        
        wait_for_service localhost 5672 "RabbitMQ"
        log_info "RabbitMQ管理界面: http://localhost:15672 (用户名: effektif, 密码: effektif123)"
    else
        log_info "RabbitMQ容器已存在，跳过启动"
    fi
}

# 启动MongoDB
start_mongodb() {
    log_info "启动MongoDB数据库..."
    
    if ! docker ps | grep -q effektif-mongodb; then
        docker run -d \
            --name effektif-mongodb \
            -e MONGO_INITDB_ROOT_USERNAME=effektif \
            -e MONGO_INITDB_ROOT_PASSWORD=effektif123 \
            -e MONGO_INITDB_DATABASE=effektif \
            -p 27017:27017 \
            -v effektif_mongodb_data:/data/db \
            mongo:4.4
        
        wait_for_service localhost 27017 "MongoDB"
    else
        log_info "MongoDB容器已存在，跳过启动"
    fi
}

# 构建应用程序
build_application() {
    log_info "构建应用程序..."
    
    if [ ! -f "pom.xml" ]; then
        log_error "未找到pom.xml文件，请确保在项目根目录下运行此脚本"
        exit 1
    fi
    
    mvn clean package -DskipTests
    
    if [ $? -ne 0 ]; then
        log_error "应用程序构建失败"
        exit 1
    fi
    
    log_info "应用程序构建成功"
}

# 启动应用程序
start_application() {
    log_info "启动Effektif集成平台..."
    
    local jar_file="target/effektif-integration-platform-3.0.0-beta15-SNAPSHOT.jar"
    
    if [ ! -f "$jar_file" ]; then
        log_error "未找到JAR文件: $jar_file"
        log_info "正在构建应用程序..."
        build_application
    fi
    
    # 设置JVM参数
    local jvm_opts="-Xms512m -Xmx1g -XX:+UseG1GC -XX:+UseStringDeduplication"
    
    # 启动应用程序
    java $jvm_opts -jar $jar_file \
        --spring.profiles.active=development \
        --logging.level.com.effektif=INFO \
        --server.port=8080 &
    
    local app_pid=$!
    echo $app_pid > effektif-integration-platform.pid
    
    log_info "应用程序正在启动，PID: $app_pid"
    
    # 等待应用程序启动
    wait_for_service localhost 8080 "Effektif集成平台"
    
    log_info "应用程序启动成功！"
    log_info "应用地址: http://localhost:8080/api"
    log_info "API文档: http://localhost:8080/api/swagger-ui.html"
    log_info "健康检查: http://localhost:8080/api/actuator/health"
}

# 停止所有服务
stop_services() {
    log_info "停止所有服务..."
    
    # 停止应用程序
    if [ -f "effektif-integration-platform.pid" ]; then
        local pid=$(cat effektif-integration-platform.pid)
        if kill -0 $pid 2>/dev/null; then
            log_info "停止应用程序 (PID: $pid)..."
            kill $pid
            rm -f effektif-integration-platform.pid
        fi
    fi
    
    # 停止Docker容器
    for container in effektif-mysql effektif-rabbitmq effektif-mongodb; do
        if docker ps | grep -q $container; then
            log_info "停止容器: $container"
            docker stop $container
            docker rm $container
        fi
    done
    
    log_info "所有服务已停止"
}

# 显示服务状态
show_status() {
    log_info "服务状态检查..."
    
    # 检查应用程序
    if [ -f "effektif-integration-platform.pid" ]; then
        local pid=$(cat effektif-integration-platform.pid)
        if kill -0 $pid 2>/dev/null; then
            log_info "✓ Effektif集成平台正在运行 (PID: $pid)"
        else
            log_warn "✗ Effektif集成平台未运行"
        fi
    else
        log_warn "✗ Effektif集成平台未运行"
    fi
    
    # 检查Docker容器
    for container in effektif-mysql effektif-rabbitmq effektif-mongodb; do
        if docker ps | grep -q $container; then
            log_info "✓ $container 容器正在运行"
        else
            log_warn "✗ $container 容器未运行"
        fi
    done
    
    # 检查端口
    for port in 3306 5672 15672 27017 8080; do
        if check_port $port; then
            log_warn "✗ 端口 $port 未被使用"
        else
            log_info "✓ 端口 $port 正在使用中"
        fi
    done
}

# 显示帮助信息
show_help() {
    echo "Effektif Integration Platform 2.0 启动脚本"
    echo ""
    echo "用法: $0 [选项]"
    echo ""
    echo "选项:"
    echo "  start     启动所有服务（默认）"
    echo "  stop      停止所有服务"
    echo "  restart   重启所有服务"
    echo "  status    显示服务状态"
    echo "  build     仅构建应用程序"
    echo "  help      显示此帮助信息"
    echo ""
    echo "示例:"
    echo "  $0 start    # 启动所有服务"
    echo "  $0 stop     # 停止所有服务"
    echo "  $0 status   # 查看服务状态"
}

# 主函数
main() {
    local action=${1:-start}
    
    case $action in
        start)
            log_info "开始启动Effektif集成平台..."
            
            # 检查必要的命令
            check_command docker
            check_command mvn
            check_command java
            check_command nc
            
            # 启动依赖服务
            start_mysql
            start_rabbitmq
            start_mongodb
            
            # 启动应用程序
            start_application
            
            log_info "所有服务启动完成！"
            show_status
            ;;
        stop)
            stop_services
            ;;
        restart)
            stop_services
            sleep 3
            main start
            ;;
        status)
            show_status
            ;;
        build)
            build_application
            ;;
        help|--help|-h)
            show_help
            ;;
        *)
            log_error "未知选项: $action"
            show_help
            exit 1
            ;;
    esac
}

# 捕获中断信号
trap 'log_info "收到中断信号，正在停止服务..."; stop_services; exit 0' INT TERM

# 执行主函数
main "$@"
