

package com.bitselect.agent.mcp.config.bit;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 比特严选业务库配置
 * <p>
 * 挂自己的前缀而不占 spring.datasource：同一个进程还托着企业助手那批工具，
 * 单独管理业务库连接和建表顺序
 */
@Data
@ConfigurationProperties(prefix = "ragent.bit")
public class BitProperties {

    private Datasource datasource = new Datasource();

    private PendingOrder pendingOrder = new PendingOrder();

    @Data
    public static class Datasource {

        private String url;

        private String username;

        private String password;

        /**
         * 建库时连的维护库，CREATE DATABASE 不能在还不存在的目标库上执行
         */
        private String maintenanceDb = "postgres";
    }

    /**
     * 待支付订单的超时释放
     * <p>
     * 建单那一刻就扣了库存、核销了券，占了不还就把两样东西永久黑洞掉
     */
    @Data
    public static class PendingOrder {

        /**
         * 演示态取短值，一场演示里能当场看到自动取消；真实电商是 30 分钟
         */
        private Duration timeout = Duration.ofMinutes(5);
    }
}
