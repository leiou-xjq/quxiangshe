package com.quxiangshe.backend.config;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Milvus向量数据库配置
 *
 * 核心职责：初始化Milvus客户端连接，创建Collection（如果不存在）
 * 业务模块：审核模块（RAG向量检索层）
 *
 * 配置项：
 *   - host: Milvus服务地址（docker-compose中为milvus）
 *   - port: Milvus服务端口（默认19530）
 *   - collection-name: 向量集合名称（违规案例库）
 *   - dimension: 向量维度（768维，对应豆包embedding）
 *
 * @author 趣享社技术团队
 */
@Slf4j
@Data
@Configuration
public class MilvusConfig {

    @Value("${milvus.host:localhost}")
    private String host;

    @Value("${milvus.port:19530}")
    private int port;

    @Value("${milvus.collection-name:violation_cases}")
    private String collectionName;

    @Value("${milvus.dimension:768}")
    private int dimension;

    @Value("${milvus.timeout:10}")
    private int timeout;

    /**
     * 创建Milvus客户端Bean
     *
     * 使用连接池模式，默认超时时间10秒
     * 在docker-compose中，backend服务通过服务名"milvus"访问
     *
     * @return MilvusServiceClient实例
     */
    @Bean
    public MilvusServiceClient milvusClient() {
        log.info("初始化Milvus客户端: host={}, port={}, collection={}, dimension={}",
            host, port, collectionName, dimension);

        try {
            ConnectParam connectParam = ConnectParam.newBuilder()
                .withHost(host)
                .withPort(port)
                .build();

            MilvusServiceClient client = new MilvusServiceClient(connectParam);
            log.info("Milvus客户端初始化成功");
            return client;
        } catch (Exception e) {
            log.error("Milvus客户端初始化失败: {}", e.getMessage(), e);
            throw new RuntimeException("Milvus客户端初始化失败", e);
        }
    }
}