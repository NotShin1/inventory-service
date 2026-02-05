######################################################################

# Dùng JDK để biên dịch, tuy nhiên sẽ dùng JRE để chạy app
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY . .
# Cấp quyền chạy cho Maven Wrapper
RUN chmod +x mvnw
# Không skip test nữa để đảm bảo code an toàn
RUN ./mvnw clean package


######################################################################

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# 👇 [QUAN TRỌNG] UPDATE HỆ ĐIỀU HÀNH ĐỂ VÁ LỖI BẢO MẬT (libexpat...) 👇
## Phải chạy lúc đang là root, trước khi tạo user
RUN apk update && apk upgrade --no-cache

# Phân quyền User; Không chạy mặc định dưới quyền root nữa

# Tạo user thường tên là 'appuser', add appser vào group appgroup
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=builder /app/target/*.jar app.jar
# Chuyển quyền sở hữu app sang cho appuser trong nhóm appgroup
RUN chown appuser:appgroup /app/app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar","app.jar"]

######################################################################