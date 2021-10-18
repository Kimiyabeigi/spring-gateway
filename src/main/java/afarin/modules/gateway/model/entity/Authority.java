package afarin.modules.gateway.model.entity;

import afarin.modules.gateway.enums.MethodType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Entity
@Table(
    name = "tbl_authority",
        uniqueConstraints = {@UniqueConstraint(name = "uc_uri_method", columnNames = {"uri", "method"})})
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Getter
@Setter
@ToString
public class Authority {
  
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_authority")
  @SequenceGenerator(sequenceName = "seq_authority", allocationSize = 1, name = "seq_authority")
  private Long id;

  @Column(
          name = "method",
          length = 10,
          columnDefinition = "VARCHAR2(10 CHAR) DEFAULT 'ALL'",
          insertable = false)
  @Enumerated(value = EnumType.STRING)
  private MethodType method = MethodType.ALL;

  @NotNull
  @Size(min = 2, max = 100)
  @Column(name = "uri", length = 100, unique = true)
  private String uri;

  @Column(name = "description", length = 500)
  private String description;

}
