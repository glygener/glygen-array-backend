package sample.web.secure.jdbc.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.JdbcUserDetailsManager;

import sample.web.secure.jdbc.model.GlygenUser;

public class GlygenUserDetailsService extends JdbcUserDetailsManager {
	
	public static final String GLYGEN_DEF_USERS_BY_USERNAME_QUERY = "select username,password,enabled,firstname,lastname,email,affiliation,affiliationwebsite,publicflag "
			+ "from users " + "where username = ?";
	
	public GlygenUserDetailsService() {
		super();
		setUsersByUsernameQuery(GLYGEN_DEF_USERS_BY_USERNAME_QUERY);
	}
	
	/**
	 * Executes the SQL <tt>usersByUsernameQuery</tt> and returns a list of UserDetails
	 * objects. There should normally only be one matching user.
	 */
	protected List<UserDetails> loadUsersByUsername(String username) {
		return getJdbcTemplate().query(getUsersByUsernameQuery(),
				new String[] { username }, new RowMapper<UserDetails>() {
					@Override
					public UserDetails mapRow(ResultSet rs, int rowNum)
							throws SQLException {
						String username = rs.getString(1);
						String password = rs.getString(2);
						boolean enabled = rs.getBoolean(3);
						String firstname = rs.getString(4);
						String lastname = rs.getString(5);
						String email = rs.getString(6);
						String affiliation = rs.getString(7);
						String affiliationWebsite = rs.getString(8);
						boolean publicFlag = rs.getBoolean(9);
						return new GlygenUser(username, password, enabled, true, true, true,
								AuthorityUtils.NO_AUTHORITIES, firstname, lastname, email, affiliation, affiliationWebsite, publicFlag);
					}

				});
	}
	
	@Override
	protected UserDetails createUserDetails(String username, UserDetails userFromUserQuery,
			List<GrantedAuthority> combinedAuthorities) {
		String returnUsername = userFromUserQuery.getUsername();

		if (!this.isUsernameBasedPrimaryKey()) {
			returnUsername = username;
		}

		return new GlygenUser(returnUsername, userFromUserQuery.getPassword(),
				userFromUserQuery.isEnabled(), true, true, true, combinedAuthorities, 
				((GlygenUser)userFromUserQuery).getFirstName(), 
				((GlygenUser)userFromUserQuery).getLastName(), 
				((GlygenUser)userFromUserQuery).getEmail(),
				((GlygenUser)userFromUserQuery).getAffiliation(), 
				((GlygenUser)userFromUserQuery).getAffiliationWebsite(), 
				((GlygenUser)userFromUserQuery).getPublicFlag());
	}
}
