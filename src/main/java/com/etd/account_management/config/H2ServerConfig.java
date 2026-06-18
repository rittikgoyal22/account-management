package com.etd.account_management.config;

import org.h2.tools.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.sql.SQLException;

// Starts an H2 TCP server on port 9092 so auth-service, travel-planner and
// any other ETD service can connect to the shared database without file-locking conflicts.
// account-management itself connects to the file directly (embedded); other services
// connect via jdbc:h2:tcp://localhost:9092/~/data/account_management
@Configuration
public class H2ServerConfig {

    @Bean(initMethod = "start", destroyMethod = "stop")
    public Server h2TcpServer() throws SQLException {
        return Server.createTcpServer("-tcp", "-tcpAllowOthers", "-tcpPort", "9092");
    }

}
