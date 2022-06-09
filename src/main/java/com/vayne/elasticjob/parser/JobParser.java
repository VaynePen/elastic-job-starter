package com.vayne.elasticjob.parser;

import com.dangdang.ddframe.job.api.ElasticJob;
import com.dangdang.ddframe.job.config.JobCoreConfiguration;
import com.dangdang.ddframe.job.config.JobTypeConfiguration;
import com.dangdang.ddframe.job.config.dataflow.DataflowJobConfiguration;
import com.dangdang.ddframe.job.config.script.ScriptJobConfiguration;
import com.dangdang.ddframe.job.config.simple.SimpleJobConfiguration;
import com.dangdang.ddframe.job.executor.handler.JobProperties;
import com.dangdang.ddframe.job.lite.config.LiteJobConfiguration;
import com.dangdang.ddframe.job.lite.spring.api.SpringJobScheduler;
import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperRegistryCenter;
import com.vayne.elasticjob.annotation.Job;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
public class JobParser implements ApplicationContextAware {

    private Environment environment;
    private final String prefix = "elastic.job.";
    private final List<String> jobTypeNameList = Arrays.asList("SimpleJob", "DataflowJob", "ScriptJob");
    @Autowired
    private ZookeeperRegistryCenter zookeeperRegistryCenter;
    @Value("${elastic.job.enable:true}")
    private Boolean jobEnable;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.environment = applicationContext.getEnvironment();
        loadJob(applicationContext);
    }

    private void loadJob(ApplicationContext context) {
        log.info("start load job");
        if (!jobEnable) {
            log.info("elastic job enable is false");
            return;
        }
        Map<String, Object> map = context.getBeansWithAnnotation(Job.class);
        for (Object jobBean : map.values()) {
            Class<?> clz = jobBean.getClass();
            String jobTypeName = this.getJobType(clz);
            Job conf = this.getJobConf(clz);
            String jobClass = this.getJobClass(clz);
            String jobName = conf.name();
            String cron = this.getEnvironmentStringValue(jobName, "cron", conf.cron());
            String shardingItemParameters = this.getEnvironmentStringValue(jobName, "shardingItemParameters", conf.shardingItemParameters());
            String description = this.getEnvironmentStringValue(jobName, "description", conf.description());
            String jobParameter = this.getEnvironmentStringValue(jobName, "jobParameter", conf.jobParameter());
            String jobExceptionHandler = this.getEnvironmentStringValue(jobName, "jobExceptionHandler", conf.jobExceptionHandler());
            String executorServiceHandler = this.getEnvironmentStringValue(jobName, "executorServiceHandler", conf.executorServiceHandler());
            String jobShardingStrategyClass = this.getEnvironmentStringValue(jobName, "jobShardingStrategyClass", conf.jobShardingStrategyClass());
            String scriptCommandLine = this.getEnvironmentStringValue(jobName, "scriptCommandLine", conf.scriptCommandLine());
            boolean failover = this.getEnvironmentBooleanValue(jobName, "failover", conf.failover());
            boolean misfire = this.getEnvironmentBooleanValue(jobName, "misfire", conf.misfire());
            boolean overwrite = this.getEnvironmentBooleanValue(jobName, "overwrite", conf.overwrite());
            boolean disabled = this.getEnvironmentBooleanValue(jobName, "disabled", conf.disabled());
            boolean monitorExecution = this.getEnvironmentBooleanValue(jobName, "monitorExecution", conf.monitorExecution());
            boolean streamingProcess = this.getEnvironmentBooleanValue(jobName, "streamingProcess", conf.streamingProcess());
            int shardingTotalCount = this.getEnvironmentIntValue(jobName, "shardingTotalCount", conf.shardingTotalCount());
            int monitorPort = this.getEnvironmentIntValue(jobName, "monitorPort", conf.monitorPort());
            int maxTimeDiffSeconds = this.getEnvironmentIntValue(jobName, "maxTimeDiffSeconds", conf.maxTimeDiffSeconds());
            int reconcileIntervalMinutes = this.getEnvironmentIntValue(jobName, "reconcileIntervalMinutes", conf.reconcileIntervalMinutes());
            JobCoreConfiguration coreConfig = JobCoreConfiguration.newBuilder(jobName, cron, shardingTotalCount).shardingItemParameters(shardingItemParameters).description(description).failover(failover).jobParameter(jobParameter).misfire(misfire).jobProperties(JobProperties.JobPropertiesEnum.JOB_EXCEPTION_HANDLER.getKey(), jobExceptionHandler).jobProperties(JobProperties.JobPropertiesEnum.EXECUTOR_SERVICE_HANDLER.getKey(), executorServiceHandler).build();
            JobTypeConfiguration typeConfig = null;
            if (jobTypeName.equals("SimpleJob")) {
                typeConfig = new SimpleJobConfiguration(coreConfig, jobClass);
            }

            if (jobTypeName.equals("DataflowJob")) {
                typeConfig = new DataflowJobConfiguration(coreConfig, jobClass, streamingProcess);
            }

            if (jobTypeName.equals("ScriptJob")) {
                typeConfig = new ScriptJobConfiguration(coreConfig, scriptCommandLine);
            }

            LiteJobConfiguration jobConfig = LiteJobConfiguration.newBuilder(typeConfig).overwrite(overwrite).disabled(disabled).monitorPort(monitorPort).monitorExecution(monitorExecution).maxTimeDiffSeconds(maxTimeDiffSeconds).jobShardingStrategyClass(jobShardingStrategyClass).reconcileIntervalMinutes(reconcileIntervalMinutes).build();


            SpringJobScheduler springJobScheduler = new SpringJobScheduler((ElasticJob) jobBean, zookeeperRegistryCenter, jobConfig);
            springJobScheduler.init();
            log.info("【" + jobName + "】init success");
        }
    }

    private String getJobType(Class<?> clz) {
        for (Class<?> clzz = clz; !clzz.equals(Object.class); clzz = clzz.getSuperclass()) {
            if (clzz.getInterfaces().length != 0) {
                Class<?>[] interfaceList = clzz.getInterfaces();
                for (Class<?> interfaceClass : interfaceList) {
                    String interfaceSimpleName = interfaceClass.getSimpleName();
                    if (this.jobTypeNameList.contains(interfaceSimpleName)) {
                        return interfaceSimpleName;
                    }
                }
            }
        }

        throw new IllegalArgumentException("current job not impl correct interface");
    }

    private Job getJobConf(Class<?> clz) {
        for (Class<?> clzz = clz; !clzz.equals(Object.class); clzz = clzz.getSuperclass()) {
            if (clzz.getAnnotation(Job.class) != null) {
                return clzz.getAnnotation(Job.class);
            }
        }

        throw new IllegalArgumentException("current job not found ElasticJobConf annotation");
    }

    private String getJobClass(Class<?> clz) {
        for (Class<?> clzz = clz; !clzz.equals(Object.class); clzz = clzz.getSuperclass()) {
            if (clzz.getAnnotation(Job.class) != null) {
                return clzz.getName();
            }
        }

        throw new IllegalArgumentException("find current job name error");
    }

    private String getEnvironmentStringValue(String jobName, String fieldName, String defaultValue) {
        String key = this.prefix + jobName + "." + fieldName;
        String value = this.environment.getProperty(key);
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private int getEnvironmentIntValue(String jobName, String fieldName, int defaultValue) {
        String key = this.prefix + jobName + "." + fieldName;
        String value = this.environment.getProperty(key);
        return StringUtils.hasText(value) ? Integer.parseInt(value) : defaultValue;
    }

    private boolean getEnvironmentBooleanValue(String jobName, String fieldName, boolean defaultValue) {
        String key = this.prefix + jobName + "." + fieldName;
        String value = this.environment.getProperty(key);
        return StringUtils.hasText(value) ? Boolean.parseBoolean(value) : defaultValue;
    }
}
