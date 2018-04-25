package org.glygen.array.service;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.provisioning.GroupManager;
import org.springframework.security.provisioning.UserDetailsManager;

/**
 * 
 * GlygenArrayRestUserDetailsService extends the the {@link UserDetailsManager} which provides the ability to create new users.
 * This is integrated into spring security, so this implementation will access the backend service to create and query user details.
 * 
 * The {@link GroupManager} interface provides interaction to the user's authorities.
 * 
 * All functionality is a mapping of the Backend REST API.
 * 
 * @author aoki
 *
 */
public class GlygenArrayRestUserDetailsService implements UserDetailsService, UserDetailsManager, GroupManager {

	Log logger = LogFactory.getLog(getClass());
	
	public GlygenArrayRestUserDetailsService() {
		super();
	}

	protected List<UserDetails> loadUsersByUsername(String username) {
		logger.debug("loadUsersByUsername(String " + username + ")");
		// rest client to access users
		List<UserDetails> listUserDetails = null;
		return listUserDetails;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		logger.debug("loadUserByUsername(String " + username + ")");
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> findAllGroups() {
		logger.debug("findAllGroups");
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> findUsersInGroup(String groupName) {
		// TODO Auto-generated method stub
		logger.debug("findAllGroups");
		return null;
	}

	@Override
	public void createGroup(String groupName, List<GrantedAuthority> authorities) {
		// TODO Auto-generated method stub
		logger.debug("findAllGroups");

	}

	@Override
	public void deleteGroup(String groupName) {
		// TODO Auto-generated method stub
		logger.debug("findAllGroups");

	}

	@Override
	public void renameGroup(String oldName, String newName) {
		// TODO Auto-generated method stub
		logger.debug("findAllGroups");

	}

	@Override
	public void addUserToGroup(String username, String group) {
		// TODO Auto-generated method stub
		logger.debug("findAllGroups");

	}

	@Override
	public void removeUserFromGroup(String username, String groupName) {
		// TODO Auto-generated method stub
		logger.debug("findAllGroups");

	}

	@Override
	public List<GrantedAuthority> findGroupAuthorities(String groupName) {
		// TODO Auto-generated method stub
		logger.debug("findAllGroups");
		return null;
	}

	@Override
	public void addGroupAuthority(String groupName, GrantedAuthority authority) {
		// TODO Auto-generated method stub
		logger.debug("findAllGroups");

	}

	@Override
	public void removeGroupAuthority(String groupName, GrantedAuthority authority) {
		// TODO Auto-generated method stub
		logger.debug("findAllGroups");

	}

	@Override
	public void createUser(UserDetails user) {
		// TODO Auto-generated method stub
		logger.debug("findAllGroups");

	}

	@Override
	public void updateUser(UserDetails user) {
		// TODO Auto-generated method stub
		logger.debug("findAllGroups");

	}

	@Override
	public void deleteUser(String username) {
		// TODO Auto-generated method stub
		logger.debug("findAllGroups");

	}

	@Override
	public void changePassword(String oldPassword, String newPassword) {
		// TODO Auto-generated method stub
		logger.debug("findAllGroups");

	}

	@Override
	public boolean userExists(String username) {
		// TODO Auto-generated method stub
		logger.debug("findAllGroups");
		return false;
	}
}
