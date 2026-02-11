######################################################################
# STAGE 1: BUILD
######################################################################
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY . .
# Cấp quyền chạy cho Maven Wrapper
RUN chmod +x mvnw
# Build package (Vẫn chạy test để đảm bảo an toàn code)
RUN ./mvnw clean package

######################################################################
# STAGE 2: RUN (SECURE)
######################################################################
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 1. Update OS để vá lỗi bảo mật (Critical Step)
RUN apk update && apk upgrade --no-cache

# 2. Tạo User & Group (Non-root)
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# 3. Copy file JAR từ builder sang
COPY --from=builder /app/target/*.jar app.jar

# 🔥 [FIX SECURITY CHUẨN]:
# Thay vì cấp quyền lung tung, ta tạo sẵn thư mục 'uploads'
# Sau đó chuyển quyền sở hữu (Owner) của JAR và folder 'uploads' cho appuser
# Các thư mục khác trong /app vẫn thuộc về root (appuser không sửa được -> An toàn hơn)
RUN mkdir uploads && \
    chown -R appuser:appgroup /app/app.jar /app/uploads

# 4. Switch sang user thường (Từ đây về sau chạy với quyền hạn chế)
USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar","app.jar"]