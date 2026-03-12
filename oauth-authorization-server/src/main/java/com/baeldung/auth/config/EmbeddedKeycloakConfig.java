package com.baeldung.auth.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.naming.CompositeName;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NamingException;
import javax.naming.spi.NamingManager;
import javax.sql.DataSource;

import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.keycloak.platform.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddedKeycloakConfig {

	private static final Logger LOG = LoggerFactory.getLogger(EmbeddedKeycloakConfig.class);

	@Value("${spring.datasource.url:}")
	private String springDatasourceUrl;

	@Value("${spring.datasource.username:}")
	private String springDatasourceUsername;

	@Value("${spring.datasource.password:}")
	private String springDatasourcePassword;

	@Value("${spring.datasource.driver-class-name:}")
	private String springDatasourceDriverClassName;

	@Value("${quickdelivery.frontend.base-urls:}")
	private String frontendBaseUrls;

	@Bean
	ServletRegistrationBean<HttpServlet30Dispatcher> keycloakJaxRsApplication(
			KeycloakServerProperties keycloakServerProperties, DataSource dataSource) throws Exception {

		propagateDatasourceSettingsToKeycloak();
		mockJndiEnvironment(dataSource);
		EmbeddedKeycloakApplication.keycloakServerProperties = keycloakServerProperties;

		ServletRegistrationBean<HttpServlet30Dispatcher> servlet = new ServletRegistrationBean<>(
				new HttpServlet30Dispatcher());
		servlet.addInitParameter("jakarta.ws.rs.Application", EmbeddedKeycloakApplication.class.getName());
		servlet.addInitParameter(ResteasyContextParameters.RESTEASY_SERVLET_MAPPING_PREFIX,
				keycloakServerProperties.getContextPath());
		servlet.addInitParameter(ResteasyContextParameters.RESTEASY_USE_CONTAINER_FORM_PARAMS, "true");
		servlet.addUrlMappings(keycloakServerProperties.getContextPath() + "/*");
		servlet.setLoadOnStartup(1);
		servlet.setAsyncSupported(true);

		return servlet;
	}

	@Bean
	FilterRegistrationBean<EmbeddedKeycloakRequestFilter> keycloakSessionManagement(KeycloakServerProperties keycloakServerProperties) {

	    FilterRegistrationBean<EmbeddedKeycloakRequestFilter> filter = new FilterRegistrationBean<>();
	    filter.setName("Keycloak Session Management");
	    filter.setFilter(new EmbeddedKeycloakRequestFilter());
	    filter.addUrlPatterns(keycloakServerProperties.getContextPath() + "/*");

	    return filter;
	}

	private void mockJndiEnvironment(DataSource dataSource) throws NamingException {
		NamingManager.setInitialContextFactoryBuilder((env) -> (environment) -> new InitialContext() {

			@Override
			public Object lookup(Name name) {
				return lookup(name.toString());
			}

			@Override
			public Object lookup(String name) {

				if ("spring/datasource".equals(name)) {
					return dataSource;
				} else if (name.startsWith("java:jboss/ee/concurrency/executor/")) {
					return fixedThreadPool();
				}

				return null;
			}

			@Override
			public NameParser getNameParser(String name) {
				return CompositeName::new;
			}

			@Override
			public void close() {
				// NOOP
			}
		});
	}
	
	@Bean("fixedThreadPool")
	public ExecutorService fixedThreadPool() {
		return Executors.newFixedThreadPool(5);
	}

	@Bean
	@ConditionalOnMissingBean(name = "springBootPlatform")
	protected SimplePlatformProvider springBootPlatform() {
		return (SimplePlatformProvider) Platform.getPlatform();
	}

	private void propagateDatasourceSettingsToKeycloak() {
		LOG.info("Spring datasource resolved: url={}, user={}, password={}, driver={}",
				springDatasourceUrl,
				springDatasourceUsername,
				maskSecret(springDatasourcePassword),
				springDatasourceDriverClassName);

		setIfPresent("keycloak.connectionsJpa.url", springDatasourceUrl);
		setIfPresent("keycloak.connectionsJpa.user", springDatasourceUsername);
		setIfPresent("keycloak.connectionsJpa.password", springDatasourcePassword);
		setIfPresent("keycloak.connectionsJpa.driver", springDatasourceDriverClassName);
		setIfPresent("quickdelivery.frontend.base-urls", frontendBaseUrls);

		if ("com.mysql.cj.jdbc.Driver".equals(springDatasourceDriverClassName)) {
			System.setProperty("keycloak.connectionsJpa.driverDialect", "org.hibernate.dialect.MySQLDialect");
		}

		LOG.info("Keycloak datasource bridge: url={}, user={}, password={}, driver={}, dialect={}",
				System.getProperty("keycloak.connectionsJpa.url"),
				System.getProperty("keycloak.connectionsJpa.user"),
				maskSecret(System.getProperty("keycloak.connectionsJpa.password")),
				System.getProperty("keycloak.connectionsJpa.driver"),
				System.getProperty("keycloak.connectionsJpa.driverDialect"));
		LOG.info("Keycloak frontend base URLs: {}", System.getProperty("quickdelivery.frontend.base-urls", "<auto-discovery>"));
	}

	private void setIfPresent(String key, String value) {
		if (value != null && !value.isBlank()) {
			System.setProperty(key, value);
		}
	}

	private String maskSecret(String value) {
		if (value == null || value.isBlank()) {
			return "<empty>";
		}
		return "****";
	}
}
