package org.glygen.array.client;

import org.glygen.array.client.model.User;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ch.qos.logback.classic.Logger;

@SpringBootApplication
@Configuration
public class Application implements CommandLineRunner {

	private static final Logger log = (Logger) LoggerFactory.getLogger(Application.class);
	
	@Bean
	@ConfigurationProperties("glygen")
	public GlygenSettings glygen() {
		return new GlygenSettings();
	}

	public static void main(String args[]) {
		new SpringApplicationBuilder(Application.class).run(args);
	}

	@Override
	public void run(String... args) throws Exception {
		UserRestClient userClient = new UserRestClientImpl();
		userClient.login(args[0], args[1]);
		User user = userClient.getUser("user");
		log.info("got user information:" + user.getEmail());
	}
	
	class GlygenSettings {
		String host;
		String scheme;
		String basePath;
		/**
		 * @return the host
		 */
		public String getHost() {
			return host;
		}
		/**
		 * @param host the host to set
		 */
		public void setHost(String host) {
			this.host = host;
		}
		/**
		 * @return the scheme
		 */
		public String getScheme() {
			return scheme;
		}
		/**
		 * @param scheme the scheme to set
		 */
		public void setScheme(String scheme) {
			this.scheme = scheme;
		}
		/**
		 * @return the basePath
		 */
		public String getBasePath() {
			return basePath;
		}
		/**
		 * @param basePath the basePath to set
		 */
		public void setBasePath(String basePath) {
			this.basePath = basePath;
		}
	}
}
