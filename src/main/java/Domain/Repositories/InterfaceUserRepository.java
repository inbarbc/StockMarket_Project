package Domain.Repositories;

import java.util.List;

import Domain.User;

import org.springframework.stereotype.Repository;

@Repository
public interface InterfaceUserRepository {
    boolean doesUserExist(String username);

    User getUserByUsername(String username);

    void addUser(User user);

    List<User> getAllUsers();
}
