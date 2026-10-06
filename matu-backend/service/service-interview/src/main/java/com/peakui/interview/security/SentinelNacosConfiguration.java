package com.peakui.interview.security;

import com.alibaba.csp.sentinel.datasource.Converter;
import com.alibaba.csp.sentinel.datasource.ReadableDataSource;
import com.alibaba.csp.sentinel.datasource.nacos.NacosDataSource;
import com.alibaba.csp.sentinel.property.DynamicSentinelProperty;
import com.alibaba.csp.sentinel.property.PropertyListener;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleUtil;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Properties;
import java.util.function.Predicate;

/**
 * 独立于 Spring YAML 配置加载的 Sentinel JSON 数组数据源。
 * dataId 保留 .yml 后缀以兼容部署发布脚本，文件内容必须是 JSON。
 * 缺失、空白、JSON null、格式/语义错误均保留上次有效规则；首次使用安全默认规则。
 * 显式 [] 允许管理员关闭对应类型的保护，属于有意操作，而非缺失配置。
 */
@Configuration
public class SentinelNacosConfiguration {
    private static final Logger log = LoggerFactory.getLogger(SentinelNacosConfiguration.class);
    private final ObjectMapper objectMapper;
    private ReadableDataSource<String, List<FlowRule>> flowDataSource;
    private ReadableDataSource<String, List<DegradeRule>> degradeDataSource;

    @Value("${spring.cloud.nacos.config.server-addr:127.0.0.1:8848}")
    private String serverAddr;
    @Value("${spring.cloud.nacos.config.namespace:${spring.cloud.nacos.namespace:}}")
    private String namespace;
    @Value("${spring.cloud.nacos.config.username:${spring.cloud.nacos.username:}}")
    private String username;
    @Value("${spring.cloud.nacos.config.password:${spring.cloud.nacos.password:}}")
    private String password;
    @Value("${spring.cloud.nacos.config.group:DEFAULT_GROUP}")
    private String group;
    @Value("${security.sentinel.flow-data-id:service-interview-sentinel-flow.yml}")
    private String flowDataId;
    @Value("${security.sentinel.degrade-data-id:service-interview-sentinel-degrade.yml}")
    private String degradeDataId;

    public SentinelNacosConfiguration(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT);
    }

    @PostConstruct
    public void init() {
        var flowProperty = new DynamicSentinelProperty<List<FlowRule>>();
        var degradeProperty = new DynamicSentinelProperty<List<DegradeRule>>();
        // 必须先挂载默认规则；Nacos 初始化失败时，其 property 可能从未赋值。
        flowProperty.updateValue(defaultFlowRules());
        degradeProperty.updateValue(defaultDegradeRules());
        FlowRuleManager.register2Property(flowProperty);
        DegradeRuleManager.register2Property(degradeProperty);
        Properties properties = nacosProperties();
        try {
            flowDataSource = createDataSource(properties, group, normalizeDataId(flowDataId), flowConverter());
            forwardNonNullUpdates(flowDataSource, flowProperty);
        } catch (RuntimeException failure) {
            log.warn("Sentinel flow data source initialization failed; retaining safe rules", failure);
        }
        try {
            degradeDataSource = createDataSource(properties, group, normalizeDataId(degradeDataId), degradeConverter());
            forwardNonNullUpdates(degradeDataSource, degradeProperty);
        } catch (RuntimeException failure) {
            log.warn("Sentinel degrade data source initialization failed; retaining safe rules", failure);
        }
    }

    private static String normalizeDataId(String dataId) {
        return dataId.endsWith(".yml") ? dataId : dataId + ".yml";
    }

    Properties nacosProperties() {
        Properties properties = new Properties();
        properties.setProperty(PropertyKeyConst.SERVER_ADDR, serverAddr);
        putNonBlank(properties, PropertyKeyConst.NAMESPACE, namespace);
        putNonBlank(properties, PropertyKeyConst.USERNAME, username);
        putNonBlank(properties, PropertyKeyConst.PASSWORD, password);
        return properties;
    }

    private static void putNonBlank(Properties properties, String key, String value) {
        if (value != null && !value.isBlank()) {
            properties.setProperty(key, value);
        }
    }

    <T> ReadableDataSource<String, List<T>> createDataSource(Properties properties, String groupId,
            String dataId, Converter<String, List<T>> converter) {
        // Sentinel 1.8.8 API 顺序是 Properties, groupId, dataId, parser。
        return new NacosDataSource<>(properties, groupId, dataId, converter);
    }

    private static <T> void forwardNonNullUpdates(ReadableDataSource<String, List<T>> source,
                                                  DynamicSentinelProperty<List<T>> target) {
        source.getProperty().addListener(new PropertyListener<>() {
            @Override
            public void configUpdate(List<T> value) {
                if (value != null) {
                    target.updateValue(value);
                }
            }

            @Override
            public void configLoad(List<T> value) {
                configUpdate(value);
            }
        });
    }

    @PreDestroy
    public void close() {
        closeDataSource(flowDataSource);
        closeDataSource(degradeDataSource);
    }

    private static void closeDataSource(ReadableDataSource<?, ?> source) {
        if (source != null) {
            try {
                source.close();
            } catch (Exception failure) {
                log.warn("Failed to close Sentinel Nacos data source", failure);
            }
        }
    }

    Converter<String, List<FlowRule>> flowConverter() {
        return new RetainingConverter<>(objectMapper, new TypeReference<>() {}, defaultFlowRules(),
                rule -> FlowRuleUtil.isValidRule(rule)
                        && SentinelProtection.READ_RESOURCE.equals(rule.getResource())
                        && Double.isFinite(rule.getCount())
                        && rule.getStrategy() <= RuleConstant.STRATEGY_CHAIN
                        && rule.getControlBehavior() <= RuleConstant.CONTROL_BEHAVIOR_WARM_UP_RATE_LIMITER);
    }

    Converter<String, List<DegradeRule>> degradeConverter() {
        return new RetainingConverter<>(objectMapper, new TypeReference<>() {}, defaultDegradeRules(),
                rule -> DegradeRuleManager.isValidRule(rule)
                        && SentinelProtection.READ_RESOURCE.equals(rule.getResource())
                        && Double.isFinite(rule.getCount()));
    }

    static List<FlowRule> defaultFlowRules() {
        FlowRule rule = new FlowRule(SentinelProtection.READ_RESOURCE);
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule.setCount(60);
        return List.of(rule);
    }

    static List<DegradeRule> defaultDegradeRules() {
        DegradeRule rule = new DegradeRule(SentinelProtection.READ_RESOURCE);
        rule.setGrade(RuleConstant.DEGRADE_GRADE_RT);
        rule.setCount(1000); // Sentinel RT count 的单位是毫秒，不是比例。
        rule.setSlowRatioThreshold(0.5);
        rule.setTimeWindow(10);
        rule.setMinRequestAmount(5);
        rule.setStatIntervalMs(10000);
        return List.of(rule);
    }

    private static final class RetainingConverter<T> implements Converter<String, List<T>> {
        private final ObjectMapper mapper;
        private final TypeReference<List<T>> type;
        private final Predicate<T> validator;
        private List<T> lastGood;

        private RetainingConverter(ObjectMapper mapper, TypeReference<List<T>> type, List<T> defaults,
                                   Predicate<T> validator) {
            this.mapper = mapper;
            this.type = type;
            this.lastGood = defaults;
            this.validator = validator;
        }

        @Override
        public synchronized List<T> convert(String source) {
            try {
                if (source == null || source.isBlank()) {
                    throw new IllegalArgumentException("Missing or blank rule content");
                }
                List<T> rules = mapper.readValue(source, type);
                if (rules == null || rules.stream().anyMatch(rule -> rule == null || !validator.test(rule))) {
                    throw new IllegalArgumentException("Invalid Sentinel rule array");
                }
                lastGood = List.copyOf(rules);
            } catch (Exception failure) {
                // 不记录原始配置，避免意外输出 Nacos 凭证或其他敏感配置。
                log.warn("Ignoring missing/invalid Sentinel rules; retaining {} last valid rules ({})",
                        lastGood.size(), failure.getClass().getSimpleName());
            }
            return lastGood;
        }
    }
}
