package org.teiid.spring.autoconfigure;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.embedded.ConnectionProperties;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseConfigurer;

final class TeiidDatabaseConfigurer implements EmbeddedDatabaseConfigurer {

	private TeiidServer server;

	TeiidDatabaseConfigurer(TeiidServer server) {
		this.server = server;
	}

	@Override
	public void configureConnectionProperties(ConnectionProperties properties, String databaseName) {
	}

	@Override
	public void shutdown(DataSource dataSource, String databaseName) {
		server.stop();
	}
}