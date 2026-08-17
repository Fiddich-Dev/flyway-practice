package com.practice.flywaypractice.member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * V1__create_member.sql 의 member 테이블과 1:1 로 대응하는 엔티티.
 *
 * ddl-auto: validate 이므로 이 클래스와 실제 테이블이 어긋나면
 * 앱이 아예 시작되지 않는다. 즉 "엔티티를 고쳤으면 마이그레이션도 같이 써야 한다"를
 * 프레임워크가 강제해준다. 이게 validate + Flyway 조합의 핵심 효용이다.
 */
@Entity
@Getter
@NoArgsConstructor
public class Member {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(nullable = false, length = 50)
	private String nickname;

	@Column(nullable = false)
	private LocalDateTime createdAt;
}
