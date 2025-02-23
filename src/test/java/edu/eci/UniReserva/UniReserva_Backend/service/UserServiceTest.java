package edu.eci.UniReserva.UniReserva_Backend.service;

import edu.eci.UniReserva.UniReserva_Backend.model.User;
import edu.eci.UniReserva.UniReserva_Backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    private User validUser;
    private User duplicateEmail;
    private User invalidePassword;
    private User updateName;
    private User updatePassword;

    @BeforeEach
    void setUp() {
        validUser = new User("1037126548", "Daniel", "email@gmail.com", "Password#123");
        duplicateEmail = new User("1038944351", "Carlos", "email@gmail.com", "Password#456");
        invalidePassword = new User("1038471526", "Vicente", "vicente@gmail.com", "123");
        updateName = new User("1037126548", "Alejandro", "email@gmail.com", "Password#123");
        updatePassword = new User("1037126548", "Daniel", "email@gmail.com", "NewPassword#123");
    }

    @Test
    public void shouldCreateUser() {
        when(userRepository.findByEmail(validUser.getEmail())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(validUser);

        String result = userService.createUser(validUser);

        assertEquals("User created successfully!", result);
        verify(userRepository).save(validUser);
    }

    @Test
    public void shouldNotCreateUserWithDuplicatedEmail() {
        when(userRepository.findByEmail(validUser.getEmail())).thenReturn(Optional.of(validUser));

        Exception exception = assertThrows(IllegalArgumentException.class, () -> userService.createUser(duplicateEmail));

        assertEquals("Email already exists", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    public void shouldNotCreateUserWithInvalidPassword() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> userService.createUser(invalidePassword));

        assertEquals("Invalid password", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    public void shouldUpdateName() {
        when(userRepository.findById(validUser.getId())).thenReturn(Optional.of(validUser));
        when(userRepository.save(any(User.class))).thenReturn(updateName);

        String result = userService.updateUser(validUser.getId(), new User(null, "Alejandro", null, null));

        assertEquals("User updated successfully!", result);
        verify(userRepository).save(argThat(user ->
                user.getId().equals(validUser.getId()) &&
                user.getName().equals("Alejandro") &&
                user.getEmail().equals(validUser.getEmail()) &&
                user.getPassword().equals(validUser.getPassword())
        ));
    }

    @Test
    public void shouldUpdatePassword() {
        when(userRepository.findById(validUser.getId())).thenReturn(Optional.of(validUser));
        when(userRepository.save(any(User.class))).thenReturn(updatePassword);

        String result = userService.updateUser(validUser.getId(), new User(null, null, null, "NewPassword#123"));

        assertEquals("User updated successfully!", result);
        verify(userRepository).save(argThat(user ->
                user.getId().equals(validUser.getId()) &&
                user.getName().equals(validUser.getName()) &&
                user.getEmail().equals(validUser.getEmail()) &&
                user.getPassword().equals("NewPassword#123")
        ));
    }

    @Test
    public void shouldNotUpdateInvalidPassword() {
        when(userRepository.findById(validUser.getId())).thenReturn(Optional.of(validUser));

        Exception exception = assertThrows(IllegalArgumentException.class, () -> userService.updateUser(validUser.getId(), new User(null, null, null, "newpassword#123")));

        assertEquals("Invalid password", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    public void shouldNotUpdateEmail() {
        when(userRepository.findById(validUser.getId())).thenReturn(Optional.of(validUser));

        Exception exception = assertThrows(IllegalArgumentException.class, () -> userService.updateUser(validUser.getId(), new User(null, null, "newEmail@gmail.com", null)));

        assertEquals("The email cannot be updated", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    public void shouldNotUpdateNonExistentUser() {
        when(userRepository.findById(validUser.getId())).thenReturn(Optional.empty());

        Exception exception = assertThrows(IllegalArgumentException.class, () -> userService.updateUser(validUser.getId(), new User(null, "Alejandro", null, null)));

        assertEquals("User not found", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }
}