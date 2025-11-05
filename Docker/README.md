# Qoder 全栈项目 Docker 部署指南
本指南将指导您如何使用 Docker 部署 Qoder 全栈项目。请按照以下步骤操作：
### 步骤1
在此项目的上一级目录下创建一个名为 `qoder-all` 的文件夹。
### 步骤2
将此项目中的 `Docker` 文件夹下所有文件，复制到 `qoder-all` 文件夹根目录下。
### 步骤3
将前端项目 `Qoder` 整个文件放在 `qoder-all` 文件夹 下
### 步骤4
将 `qoder-all` 文件夹拷贝到服务器上任意目录下，例如 `/home/user/` 目录下。
最后目录结构如下所示：
```qoder-all
├── Dockerfile
├── entrypoint.sh
├── .dockerignore
├── nginx.conf
├── Qoder(前端项目文件夹)
├── Qoder-Server(后端项目文件夹)
``` 
### 步骤5
进入服务器终端，切换到 `qoder-all` 文件夹目录下，例如：
```bash
cd /home/user/qoder-all
```
### 步骤6
运行以下命令构建并启动 Docker 容器：
```bash
docker buid -t qoder-all .
```
### 步骤7
docker run 命令运行项目:
```bash
docker run --name=qoder -d -p 8085:80  \
  -e SPRING_DATASOURCE_URL="jdbc:postgresql://xxx.xxx.xxx.xxx:5432/xxx"  \
  -e SPRING_DATASOURCE_USERNAME="postgres"  \
  -e SPRING_DATASOURCE_PASSWORD="xxx"  \
  -e JAVA_OPTS="-Xms256m -Xmx512m"  \
  qoder-all
```
请根据实际情况替换上述命令中的数据库连接信息。
### 步骤8
访问 `http://服务器IP:8085` 即可使用 Qoder 全栈项目
### 注意事项
- 确保服务器上已安装 Docker，并且 Docker 服务正在运行。
- 根据实际需求调整环境变量和端口映射。
- 确保数据库服务器可以从运行 Qoder 的服务器访问。
- 根据需要调整 Java 内存设置（`JAVA_OPTS`）。
- 如果需要持久化数据，请考虑将数据库数据目录映射到主机上的一个目录。
- 确保防火墙允许访问所使用的端口（例如 8085）。
- 定期备份数据库以防止数据丢失。
- 监控 Docker 容器的运行状态，确保服务正常运行。
- 根据实际使用情况调整 Docker 容器的资源限制（CPU、内存等）。
- 定期更新 Qoder 项目和相关依赖，以获取最新的功能和安全修复。
- 如果遇到问题，可以查看 Docker 容器的日志以进行排查：
```bash
docker logs qoder
```
- 如需停止或重启容器，可以使用以下命令：
```bash
docker stop qoder
docker start qoder
```
### 常见问题排查
1. **无法访问应用程序**：
   - 确认 Docker 容器是否正在运行：
   ```bash
   docker ps
   ```
   - 检查防火墙设置，确保端口 8085 已开放。
2. **数据库连接失败**：
   - 确认数据库服务器地址、用户名和密码是否正确。
   - 确保数据库服务器允许来自 Qoder 服务器的连接。
3. **内存不足错误**：
   - 调整 `JAVA_OPTS` 环境变量中的内存设置，例如增加 `-Xmx` 的值。
4. **日志查看**：
   - 使用以下命令查看容器日志，排查错误信息：
   ```bash
   docker logs qoder
   ```
5. **容器重启问题**：
   - 如果容器频繁重启，检查日志以确定是否有异常导致容器崩溃。
   - 确保服务器资源充足，避免因内存或 CPU 不足导致容器崩溃。
   - 根据需要调整 Docker 容器的资源限制。
6. **端口冲突**：
7. - 如果端口 8085 已被其他服务占用，可以更改 `docker run` 命令中的端口映射，例如使用 `-p 8090:80` 将容器的 80 端口映射到主机的 8090 端口。
8. **数据持久化问题**:
   - 如果需要持久化数据库数据，建议将数据库的数据目录映射到主机上的一个目录，确保数据不会因容器重启或删除而丢失。
   - 例如，在运行数据库容器时使用 `-v /path/on/host:/var/lib/postgresql/data` 进行数据卷映射。
9. **更新和维护**：
   - 定期检查 Qoder 项目的更新，获取最新的功能和安全修复。
   - 在更新项目时，建议先备份数据库和重要数据，以防止数据丢失。
10. **性能监控**：
   - 使用 Docker 的监控工具或第三方监控解决方案，监控容器的资源使用情况，确保服务稳定运行。
     - 根据实际使用情况调整容器的资源限制（CPU、内存等），以优化性能。
通过以上步骤和注意事项，您应该能够顺利部署和运行 Qoder 全栈项目。如有其他问题，请联系SchrodingersFish@outlook获取帮助。