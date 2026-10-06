package com.peakui.auth.mini;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.peakui.auth.mapper.*;
import com.peakui.auth.util.PasswordUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real SQL/transaction tests on isolated H2; production MySQL migration still needs staging verification. */
class MiniIdentityTransactionTest {
    private MiniIdentityService service;
    private JdbcTemplate jdbc;
    private final MiniModels.WechatIdentity identity = new MiniModels.WechatIdentity("app", "open-id", null);
    @BeforeEach void setup() throws Exception {
        var ds = new DriverManagerDataSource("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(ds);
        jdbc.execute("CREATE TABLE users(id BIGINT PRIMARY KEY,username VARCHAR(50) UNIQUE,email VARCHAR(100) UNIQUE,nickname VARCHAR(50),phone VARCHAR(20),password_hash VARCHAR(255),avatar_url VARCHAR(500),gender INT,birthday DATE,signature VARCHAR(200),status INT,last_login_time TIMESTAMP,last_login_ip VARCHAR(45),created_at TIMESTAMP,updated_at TIMESTAMP,deleted_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE user_profiles(id BIGINT PRIMARY KEY,user_id BIGINT UNIQUE,activity_level BIGINT,school_verified INT,company_verified INT,title_verified INT,is_vip INT,vip_level INT,vip_days_remaining INT,follower_count INT,following_count INT,view_count INT)");
        jdbc.execute("CREATE TABLE roles(id BIGINT PRIMARY KEY,role_code VARCHAR(30),role_name VARCHAR(50),role_desc VARCHAR(200),status INT,created_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE user_roles(id BIGINT PRIMARY KEY,user_id BIGINT,role_id BIGINT,created_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE user_external_identities(id BIGINT PRIMARY KEY,user_id BIGINT,provider VARCHAR(32),app_id VARCHAR(64),open_id VARCHAR(128),union_id VARCHAR(128),created_at TIMESTAMP,UNIQUE(provider,app_id,open_id),UNIQUE(provider,app_id,user_id))");
        jdbc.update("INSERT INTO roles(id,role_code,status) VALUES(1,'USER',1)");
        var config = new MybatisConfiguration(); config.setMapUnderscoreToCamelCase(true);
        for (var mapper : List.of(UserMapper.class,RoleMapper.class,UserRoleMapper.class,UserProfileMapper.class,ExternalIdentityMapper.class)) config.addMapper(mapper);
        var factory = new MybatisSqlSessionFactoryBean(); factory.setDataSource(ds); factory.setConfiguration(config);
        var sql = new SqlSessionTemplate(factory.getObject());
        service = new MiniIdentityService(sql.getMapper(ExternalIdentityMapper.class),sql.getMapper(UserMapper.class),
                sql.getMapper(UserProfileMapper.class),sql.getMapper(UserRoleMapper.class),sql.getMapper(RoleMapper.class),
                new TransactionTemplate(new DataSourceTransactionManager(ds)));
    }
    @Test void createsOneUserWithNoDefaultPasswordAndRole() {
        Long id = service.complete(identity,null);
        assertNotNull(id); assertFalse(MiniIdentityService.passwordEnabled(service.active(id)));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM user_roles WHERE user_id=?",Integer.class,id));
        assertTrue(service.bound(id,"app")); assertFalse(service.bound(id,"other-app"));
    }
    @Test void duplicateCreationRollsBackUserProfileAndRole() {
        service.complete(identity,null);
        assertThrows(RuntimeException.class,()->service.complete(identity,null));
        for(String table:List.of("users","user_profiles","user_roles","user_external_identities"))
            assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM "+table,Integer.class));
    }
    @Test void concurrentFirstLoginCannotCreateTwoAccounts() throws Exception {
        var barrier = new CyclicBarrier(2); var pool = Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> attempt = () -> { barrier.await(); try {service.complete(identity,null);return true;}catch(RuntimeException e){return false;} };
            var futures = pool.invokeAll(List.of(attempt,attempt)); int successes=0;
            for(var future:futures) if(future.get())successes++;
            assertEquals(1,successes); assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM users",Integer.class));
        } finally {pool.shutdownNow();}
    }
    @Test void existingAccountBindingIsIdempotentButCannotBeStolen() {
        jdbc.update("INSERT INTO users(id,username,email,password_hash,status) VALUES(10,'old','old@example.test','!disabled!',1),(11,'other','other@example.test','!disabled!',1)");
        assertEquals(10L,service.complete(identity,10L)); assertEquals(10L,service.complete(identity,10L));
        assertThrows(RuntimeException.class,()->service.complete(identity,11L));
        assertEquals(10L,service.find(identity).getUserId());
    }
    @Test void refusesDisabledAccountsAndOnlyLoginUnlink() {
        Long id=service.complete(identity,null);
        assertThrows(RuntimeException.class,()->service.unlink(id,"app","anything"));
        jdbc.update("UPDATE users SET status=2 WHERE id=?",id);
        assertThrows(RuntimeException.class,()->service.active(id));
    }
    @Test void unlinkRequiresPasswordAndPreservesOtherApps() {
        Long id=service.complete(identity,null);
        jdbc.update("UPDATE users SET password_hash=? WHERE id=?",PasswordUtil.encode("Known-test-password"),id);
        service.complete(new MiniModels.WechatIdentity("other-app","other-open",null),id);
        assertThrows(RuntimeException.class,()->service.unlink(id,"app","wrong"));
        assertTrue(service.bound(id,"app"));
        service.unlink(id,"app","Known-test-password");
        assertFalse(service.bound(id,"app"));assertTrue(service.bound(id,"other-app"));
    }
}
