package com.indigo.synapse.iam.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * IAM 本地开发初始化配置。
 *
 * <p>该配置只用于受控地创建本地联调管理员。默认关闭；启用时必须由外部环境显式提供
 * username 与 password，禁止使用固定默认密码。</p>
 */
@ConfigurationProperties(prefix = "synapse.iam.bootstrap")
public class IamBootstrapProperties {

    /**
     * 是否启用本地开发管理员初始化。
     */
    private boolean enabled;

    /**
     * 本地开发管理员用户名。
     */
    private String username;

    /**
     * 本地开发管理员原始密码，仅用于启动期编码，不写入日志或数据库明文。
     */
    private String password;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
