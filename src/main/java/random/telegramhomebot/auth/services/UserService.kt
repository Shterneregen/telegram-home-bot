package random.telegramhomebot.auth.services;

import random.telegramhomebot.auth.db.entities.User;

import java.util.Optional;

public interface UserService {
    Optional<User> getUserByID(long id);

    void changeUserPassword(User user, String password);

    boolean checkIfValidOldPassword(User user, String oldPassword);
}
