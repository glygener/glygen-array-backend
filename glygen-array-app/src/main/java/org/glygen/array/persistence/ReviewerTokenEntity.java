package org.glygen.array.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Entity(name="reviewer_tokens")
@XmlRootElement (name="reviewer-token")
@JsonSerialize
public class ReviewerTokenEntity {
	
	@Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="token_seq")
    @SequenceGenerator(name="token_seq", sequenceName="TOKEN_SEQ", allocationSize=1)
    private Long id;
	
	@Column(name="token", unique = false, nullable = false)
	private String token;
	
	@Column(name="graphuri", unique = false, nullable = false)
	private String graphURI;
	
	@Column(name="resourceURI", unique = false, nullable = false)
	private String resourceURI;
	
	/**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(Long id) {
		this.id = id;
	}
	/**
	 * @return the token
	 */
	public String getToken() {
		return token;
	}
	/**
	 * @param token the token to set
	 */
	public void setToken(String token) {
		this.token = token;
	}
	/**
	 * @return the graph
	 */
	public String getGraphURI() {
		return graphURI;
	}
	/**
	 * @param graph the graph to set
	 */
	public void setGraphURI(String graph) {
		this.graphURI = graph;
	}
	/**
	 * @return the resourceURI
	 */
	public String getResourceURI() {
		return resourceURI;
	}
	/**
	 * @param resourceURI the resourceURI to set
	 */
	public void setResourceURI(String resourceURI) {
		this.resourceURI = resourceURI;
	}

	
	
}
