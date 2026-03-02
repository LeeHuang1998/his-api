package com.leehuang.his.api.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * xxl-job 配置：
 *      1.在 docker 中部署 xxl-job-admin，调度中心页面端口为 8080，执行器端口为 9999
 *      2. 部署完成后，在项目中引入 xxl-job-core 依赖，与调度中心版本一致，并且在主库中执行 tables_xxl_job.sql，创建 xxl_job 数据库（里面有 8 张表）
 *      3. 在 application.yml 中配置 xxl-job-admin 地址，以及定时任务参数（app.order）
 *      4. 登录调度中心（账号：admin，密码：123456）
 *          4.1 在执行器管理中，创建执行器，属性值要和 application.yaml 中配置的一致
 *          4.2 在任务管理中，创建任务，属性值要和 OrderCloseJobHandler 中配置的一致，cron 表达式自定义即可
 *      5. 在任务管理中，启动该任务即可
 */
@Configuration
public class XxlJobConfig {
    private static final Logger logger = LoggerFactory.getLogger(XxlJobConfig.class);

    @Value("${xxl.job.admin.addresses}")
    private String adminAddresses;

    @Value("${xxl.job.executor.appname}")
    private String appName;

    @Value("${xxl.job.executor.ip:}")
    private String ip;

    @Value("${xxl.job.executor.port:9999}")
    private int port;

    @Value("${xxl.job.executor.logpath:./logs/xxl-job}")
    private String logPath;

    @Value("${xxl.job.executor.logretentiondays:30}")
    private int logRetentionDays;

    @Value("${xxl.job.accessToken:}")
    private String accessToken;

    @Bean
    public XxlJobSpringExecutor xxlJobExecutor() {
        logger.info(">>>>>>>>>>> XXL-JOB 执行器初始化...");
        XxlJobSpringExecutor xxlJobSpringExecutor = new XxlJobSpringExecutor();
        xxlJobSpringExecutor.setAdminAddresses(adminAddresses);
        xxlJobSpringExecutor.setAppname(appName);
        xxlJobSpringExecutor.setIp(ip);                                     // 留空自动获取
        xxlJobSpringExecutor.setPort(port);
        xxlJobSpringExecutor.setLogPath(logPath);
        xxlJobSpringExecutor.setLogRetentionDays(logRetentionDays);
        xxlJobSpringExecutor.setAccessToken(accessToken);                   // 在 docker 容器中配置后必须设置 accessToken，防止任务调度时被安全拦截
        return xxlJobSpringExecutor;
    }
}