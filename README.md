# JAVA-unversity
SSM（Spring + SpringMVC + MyBatis）+ Tomcat + MySQL + Maven
## 部署步骤
1. 环境准备：JDK 1.8、Tomcat 8.5、MySQL 8.0、Maven 3.6+
2. 导入项目里的 sql 脚本，新建数据库，修改配置文件里数据库账号密码
3. Maven 打包：执行 `mvn clean package` 生成 war 包
4. 将 war 包放到 `tomcat/webapps`，启动 Tomcat
5. 访问地址：`http://localhost:8080/项目名称`
