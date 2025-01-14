package kr.hhplus.be.server.user.domain.repository;

import kr.hhplus.be.server.user.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

}
