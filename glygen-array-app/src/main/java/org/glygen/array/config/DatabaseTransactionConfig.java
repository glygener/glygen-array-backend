package org.glygen.array.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class DatabaseTransactionConfig {
	
	@Bean
	@Qualifier(value="jpaTransactionManager")
	public PlatformTransactionManager transactionManager() {
		return new JpaTransactionManager();
	}

}
