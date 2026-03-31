package com.baeldung.auth.config;

import java.util.LinkedHashSet;
import java.util.NoSuchElementException;
import java.util.Set;

import org.keycloak.Config;
import org.keycloak.exportimport.ExportImportManager;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.ApplianceBootstrap;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.KeycloakApplication;
import org.keycloak.services.util.JsonConfigProviderFactory;
import org.keycloak.util.JsonSerialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import com.baeldung.auth.config.KeycloakServerProperties.AdminUser;

public class EmbeddedKeycloakApplication extends KeycloakApplication {

	private static final Logger LOG = LoggerFactory.getLogger(EmbeddedKeycloakApplication.class);

	static KeycloakServerProperties keycloakServerProperties;
	
	protected void loadConfig() {
        JsonConfigProviderFactory factory = new RegularJsonConfigProviderFactory();
        Config.init(factory.create()
            .orElseThrow(() -> new NoSuchElementException("No value present")));
    }

	@Override
	protected ExportImportManager bootstrap() {
		final ExportImportManager exportImportManager = super.bootstrap();
		createMasterRealmAdminUser();
		createBaeldungRealm();
		return exportImportManager;
	}

	private void createMasterRealmAdminUser() {

		KeycloakSession session = getSessionFactory().create();

		ApplianceBootstrap applianceBootstrap = new ApplianceBootstrap(session);

		AdminUser admin = keycloakServerProperties.getAdminUser();

		try {
			session.getTransactionManager().begin();
			applianceBootstrap.createMasterRealmUser(admin.getUsername(), admin.getPassword());
			session.getTransactionManager().commit();
		} catch (Exception ex) {
			LOG.warn("Couldn't create keycloak master admin user: {}", ex.getMessage());
			session.getTransactionManager().rollback();
		}

		session.close();
	}

	private void createBaeldungRealm() {
		KeycloakSession session = getSessionFactory().create();

		try {
			session.getTransactionManager().begin();
			RealmModel existingRealm = session.realms().getRealmByName("quickdelivery");
			if (existingRealm != null) {
				synchronizeFrontClient(session, existingRealm.getName());
				session.getTransactionManager().commit();
				return;
			}

			RealmManager manager = new RealmManager(session);
			Resource lessonRealmImportFile = new ClassPathResource(keycloakServerProperties.getRealmImportFile());
			RealmRepresentation realmRepresentation =
					JsonSerialization.readValue(lessonRealmImportFile.getInputStream(), RealmRepresentation.class);

			manager.importRealm(realmRepresentation);
			synchronizeFrontClient(session, realmRepresentation.getRealm());

			session.getTransactionManager().commit();
		} catch (Exception ex) {
			LOG.warn("Failed to import Realm json file: {}. Trying to synchronize existing realm.", ex.getMessage());
			session.getTransactionManager().rollback();
			synchronizeExistingRealm();
		}

		session.close();
	}

	private void synchronizeExistingRealm() {
		KeycloakSession session = getSessionFactory().create();

		try {
			session.getTransactionManager().begin();
			synchronizeFrontClient(session, "quickdelivery");
			session.getTransactionManager().commit();
		} catch (Exception ex) {
			LOG.warn("Failed to synchronize existing realm client: {}", ex.getMessage());
			session.getTransactionManager().rollback();
		} finally {
			session.close();
		}
	}

	private void synchronizeFrontClient(KeycloakSession session, String realmName) {
		RealmModel realm = session.realms().getRealmByName(realmName);
		if (realm == null) {
			LOG.warn("Realm {} not found for front client synchronization.", realmName);
			return;
		}

		ClientModel frontClient = realm.getClientByClientId("quickdelivery-front");
		if (frontClient == null) {
			LOG.warn("Client quickdelivery-front not found in realm {}.", realmName);
			return;
		}

		Set<String> redirectUris = new LinkedHashSet<>(frontClient.getRedirectUris());
		redirectUris.add("quickdelivery://auth/callback");
		for (String baseUrl : PublicEndpointResolver.resolveFrontendBaseUrls(System.getProperty("quickdelivery.frontend.base-urls"))) {
			redirectUris.add(baseUrl + "/");
			redirectUris.add(baseUrl + "/app");
			redirectUris.add(baseUrl + "/dashboard/admin");
			redirectUris.add(baseUrl + "/dashboard/courier");
			redirectUris.add(baseUrl + "/dashboard/client");
			redirectUris.add(baseUrl + "/dashboard/metrics");
			redirectUris.add(baseUrl + "/dashboard/finance");
			redirectUris.add(baseUrl + "/createPackage");
			redirectUris.add(baseUrl + "/myPackages");
			redirectUris.add(baseUrl + "/userAccount");
			redirectUris.add(baseUrl + "/notifications");
			redirectUris.add(baseUrl + "/usersAccountValidation");
		}
		frontClient.setRedirectUris(redirectUris);

		Set<String> webOrigins = new LinkedHashSet<>(frontClient.getWebOrigins());
		webOrigins.addAll(PublicEndpointResolver.resolveFrontendBaseUrls(System.getProperty("quickdelivery.frontend.base-urls")));
		webOrigins.add("quickdelivery://auth");
		frontClient.setWebOrigins(webOrigins);
	}
}
