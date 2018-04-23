package org.glygen.array.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.glygen.array.model.SettingEntity;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.JdbcDaoSupport;

public class SettingsDAOImpl extends JdbcDaoSupport {
	public static final String DEF_SETTINGS_BY_NAME_QUERY = "select name, value "
			+ "from settings " + "where name = ?";
	
	String settingsByNameQuery;
	
	public SettingsDAOImpl() {
		this.settingsByNameQuery = DEF_SETTINGS_BY_NAME_QUERY;
	}

	/**
	 * Executes the SQL <tt>usersByUsernameQuery</tt> and returns a list of UserDetails
	 * objects. There should normally only be one matching user.
	 */
	protected List<SettingEntity> getSetting(String name) {
		return getJdbcTemplate().query(this.settingsByNameQuery,
				new String[] { name }, new RowMapper<SettingEntity>() {
					@Override
					public SettingEntity mapRow(ResultSet rs, int rowNum)
							throws SQLException {
						String name = rs.getString(1);
						String value = rs.getString(2);
						return new SettingEntity(name, value);
					}

				});
	}
	
	/**
	 * retrieve the setting with the given name
	 * @param name name of the setting
	 * @return SettingEntity matching the name
	 */
	public SettingEntity getSettingByName (String name) {
		List<SettingEntity> settings = this.getSetting(name);
		if (settings.size() == 0) {
			this.logger.debug("Query returned no results for setting '" + name + "'");
			throw new RuntimeException ("Setting " + name + " not found!");
		}
		
		SettingEntity setting = settings.get(0);
		return setting;
	}
}
