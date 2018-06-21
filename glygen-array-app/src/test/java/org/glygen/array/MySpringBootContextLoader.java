package org.glygen.array;

import org.springframework.boot.test.context.SpringBootContextLoader;
import org.springframework.core.env.ConfigurableEnvironment;

import com.ulisesbocchio.jasyptspringboot.environment.StandardEncryptableEnvironment;

public class MySpringBootContextLoader extends SpringBootContextLoader {
	
	@Override
	protected ConfigurableEnvironment getEnvironment() {
		return new StandardEncryptableEnvironment();
	}

}
