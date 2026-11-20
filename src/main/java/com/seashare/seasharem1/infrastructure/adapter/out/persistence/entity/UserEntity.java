package com.seashare.seasharem1.infrastructure.adapter.out.persistence.entity;

import com.seashare.seasharem1.domain.model.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Entidad JPA de la tabla users. */
@Entity
@Table(name = "users")
public class UserEntity {

  @Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "id", length = 36, nullable = false, updatable = false)
  private UUID id;

  @Column(name = "full_name", length = 100, nullable = false)
  private String fullName;

  @Column(name = "document_number", nullable = false)
  private long documentNumber;

  @Column(name = "email", length = 150, nullable = false)
  private String email;

  @Column(name = "phone", length = 20, nullable = false)
  private String phone;

  @Column(name = "password", nullable = false)
  private String password;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false)
  private UserRole role;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public long getDocumentNumber() {
    return documentNumber;
  }

  public void setDocumentNumber(long documentNumber) {
    this.documentNumber = documentNumber;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public UserRole getRole() {
    return role;
  }

  public void setRole(UserRole role) {
    this.role = role;
  }
}
