package afarin.modules.gateway.model.entity;

import afarin.modules.gateway.enums.AuthenticationType;
import afarin.modules.gateway.enums.UserType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.ToString;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.ColumnDefault;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Collection;

@Entity
@Table(
    name = "tbl_user",
    uniqueConstraints = {
      @UniqueConstraint(name = "uc_user_username", columnNames = "username"),
      @UniqueConstraint(name = "uc_user_ip", columnNames = "ip")})
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Data
@ToString(exclude = "password")
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_user")
  @SequenceGenerator(sequenceName = "seq_user", allocationSize = 1, name = "seq_user")
  private Long id;

  @NotNull
  @Column(name = "username", length = 50, nullable = false)
  private String username;

  @NotNull
  @Column(name = "ip", length = 200, nullable = false)
  private String ip;

  @Column(name = "application_id", length = 50)
  private String applicationId;

  @Column(name = "module_id", length = 50)
  private String moduleId;

  @JsonIgnore
  @NotNull
  @Column(name = "password", length = 100, nullable = false)
  private String password;

  @ColumnDefault("1")
  @Column(name = "is_active")
  private Boolean isActive = true;

  @Column(
      name = "user_type",
      length = 10,
      columnDefinition = "VARCHAR2(10 CHAR) DEFAULT 'USER'",
      insertable = false)
  @Enumerated(value = EnumType.STRING)
  private UserType userType = UserType.USER;

  @Column(
          name = "auth_type",
          length = 10,
          columnDefinition = "VARCHAR2(10 CHAR) DEFAULT 'TOKEN'",
          insertable = false)
  @Enumerated(value = EnumType.STRING)
  private AuthenticationType authType = AuthenticationType.TOKEN;

  @JsonIgnore
  @ManyToMany
  @JoinTable(
      name = "tbl_user_authority",
      joinColumns = {@JoinColumn(name = "user_id", referencedColumnName = "id")},
      inverseJoinColumns = {@JoinColumn(name = "authority_id", referencedColumnName = "id")},
      uniqueConstraints = {@UniqueConstraint(name = "uc_user_authority", columnNames = {"user_id", "authority_id"})})
  @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
  @BatchSize(size = 20)
  private Collection<Authority> authorities;
}
