package org.glygen.array.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.glygen.array.persistence.GlygenUser;
import org.glygen.array.persistence.RoleEntity;
import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.dao.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service("userDetailsService")
public class GlygenUserDetailsService implements UserDetailsService {
	
	@Autowired
	private UserRepository userRepository;
	
	final static Logger logger = LoggerFactory.getLogger("event-logger");
	
	public GlygenUserDetailsService() {
		super();
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		UserEntity user = userRepository.findByUsernameIgnoreCase(username);
		if (user == null) 
			throw new UsernameNotFoundException("User with username " + username + " does not exist!");
			
		return new GlygenUser(username, user.getPassword(), user.getEnabled(), true, true, true,
				getAuthorities(user.getRoles()), user.getFirstName(), user.getLastName(), 
				user.getEmail(), user.getAffiliation(), user.getAffiliationWebsite(), user.getPublicFlag());
	}
	
	public static final Collection<? extends GrantedAuthority> getAuthorities(final Collection<RoleEntity> roles) {
        return getGrantedAuthorities(getRoleNames(roles));
    }

    public static final List<String> getRoleNames(final Collection<RoleEntity> roles) {
    	List<String> roleNames = new ArrayList<>();
        for (final RoleEntity role : roles) {
        	roleNames.add(role.getRoleName());
        }

        return roleNames;
    }

    public static final List<GrantedAuthority> getGrantedAuthorities(final List<String> roleNames) {
        final List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
        for (final String role : roleNames) {
            authorities.add(new SimpleGrantedAuthority(role));
        }
        return authorities;
    }
}
