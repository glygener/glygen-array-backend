package org.glygen.array.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Entity(name="permissions")
@XmlRootElement (name="graph-permission")
@JsonSerialize
public class GraphPermissionEntity {
	@Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="token_seq")
    @SequenceGenerator(name="token_seq", sequenceName="TOKEN_SEQ")
    private Long id;
	
	@OneToOne(targetEntity = UserEntity.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "userid", foreignKey = @ForeignKey(name = "FK_VERIFY_USER"))
    private UserEntity user;
	
	@Column(name="graphuri", unique = false, nullable = true)
	private String graphIRI;
	
	@Column(name="resourceuri", unique = false, nullable = true)
	private String resourceIRI;
	
	@Column(name="permissiontype", unique = false, nullable = false)
	private Integer permission = 0;   // 0 - read only, 1 - read/update/delete
	
	public String getGraphIRI() {
		return graphIRI;
	}
	
	public void setGraphIRI(String graphIRI) {
		this.graphIRI = graphIRI;
	}
	
	public UserEntity getUser() {
		return user;
	}
	
	public void setUser(UserEntity user) {
		this.user = user;
	}
	
	public String getResourceIRI() {
		return resourceIRI;
	}
	
	public void setResourceIRI(String resourceIRI) {
		this.resourceIRI = resourceIRI;
	}
	
	public Integer getPermission() {
		return permission;
	}
	
	public void setPermission(Integer permission) {
		this.permission = permission;
	}
}
