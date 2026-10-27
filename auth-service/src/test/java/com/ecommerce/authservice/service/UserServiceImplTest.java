package com.ecommerce.authservice.service;

import com.ecommerce.authservice.constant.RoleConstant;
import com.ecommerce.authservice.dto.request.RegisterRequest;
import com.ecommerce.authservice.dto.request.UpdateUserRequest;
import com.ecommerce.authservice.dto.response.UserResponse;
import com.ecommerce.authservice.entity.Role;
import com.ecommerce.authservice.entity.RoleName;
import com.ecommerce.authservice.entity.User;
import com.ecommerce.authservice.mapper.UserMapper;
import com.ecommerce.authservice.repository.UserRepository;
import com.ecommerce.commonlib.exception.BusinessException;
import com.ecommerce.commonlib.keycloak.KeycloakAuthClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleService roleService;

    @Mock
    private KeycloakAuthClient keycloakAuthClient;

    private UserMapper userMapper;
    private UserServiceImpl userService;
    private Validator validator;

    @BeforeEach
    void setUp() {
        userMapper = new UserMapper(new ModelMapper());
        userService = new UserServiceImpl(userRepository, userMapper, roleService, keycloakAuthClient);
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void updateShouldReturnUserResponse() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFullName("Updated Name");

        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setUsername("testuser");

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        UserResponse result = userService.update(1L, request);

        assertEquals("testuser", result.getUsername());
        verify(userRepository).save(existingUser);
    }

    @Test
    void updateShouldThrowWhenUserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> userService.update(999L, new UpdateUserRequest()));
    }

    @Test
    void findByIdShouldReturnUser() {
        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        User result = userService.findById(1L);

        assertEquals(1L, result.getId());
    }

    @Test
    void findByIdShouldThrowWhenNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> userService.findById(999L));
    }

    @Test
    void principalCannotReadAnotherUsersProfile() {
        when(userRepository.findByIdAndKeycloakUserId(22L, "kc-user-a")).thenReturn(Optional.empty());
        BusinessException error = assertThrows(BusinessException.class,
                () -> userService.findByIdForPrincipal(22L, "kc-user-a", false));
        assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, error.getStatus());
        verify(userRepository).findByIdAndKeycloakUserId(22L, "kc-user-a");
        verify(userRepository, never()).findById(22L);
    }

    @Test
    void principalCannotReadAnotherUsersProfileByUsername() {
        when(userRepository.findByUsernameAndKeycloakUserId("user-b", "kc-user-a")).thenReturn(Optional.empty());
        assertThrows(BusinessException.class,
                () -> userService.findByUsernameForPrincipal("user-b", "kc-user-a", false));
        verify(userRepository).findByUsernameAndKeycloakUserId("user-b", "kc-user-a");
        verify(userRepository, never()).findByUsername("user-b");
    }

    @Test
    void principalCannotUpdateOrDeleteAnotherUsersProfile() {
        when(userRepository.findByIdAndKeycloakUserId(22L, "kc-user-a")).thenReturn(Optional.empty());
        assertThrows(BusinessException.class,
                () -> userService.updateForPrincipal(22L, new UpdateUserRequest(), "kc-user-a", false));
        assertThrows(BusinessException.class,
                () -> userService.deleteForPrincipal(22L, "kc-user-a", false));
        verify(userRepository, never()).save(any(User.class));
        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    void administratorCanReadUserByDatabaseId() {
        User user = new User();
        user.setId(22L);
        when(userRepository.findById(22L)).thenReturn(Optional.of(user));
        assertSame(user, userService.findByIdForPrincipal(22L, "kc-admin", true));
        verify(userRepository).findById(22L);
    }

    @Test
    void principalCanUpdateOwnProfileUsingKeycloakIdentity() {
        User user = new User();
        user.setId(10L);
        user.setKeycloakUserId("kc-user-a");
        user.setUsername("user-a");
        when(userRepository.findByIdAndKeycloakUserId(10L, "kc-user-a")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFullName("Updated Name");
        assertEquals("Updated Name", userService.updateForPrincipal(10L, request, "kc-user-a", false).getFullname());
        verify(userRepository).save(user);
    }

    @Test
    void findAllUsersShouldReturnPagedResults() {
        User user = new User();
        user.setId(1L);

        when(userRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(user)));

        Page<UserResponse> result = userService.findAllUsers(0, 10, "id", "ASC");

        assertEquals(1, result.getContent().size());
        assertEquals(1L, result.getContent().get(0).getId());
    }

    @Test
    void registerShouldThrowWhenUsernameExists() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("existing");
        request.setEmail("test@test.com");
        request.setPhone("0123456789");

        when(userRepository.existsByUsername("existing")).thenReturn(true);

        assertThrows(BusinessException.class, () -> userService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerShouldAssignOnlyDefaultRoleToKeycloakAndDatabase() {
        RegisterRequest request = validRegistration();
        Role customerRole = new Role(1L, RoleName.USER);
        when(roleService.findByName(RoleName.USER)).thenReturn(Optional.of(customerRole));
        when(keycloakAuthClient.createUser("newuser", "newuser@example.com", "New Customer",
                "Password1", List.of(RoleConstant.DEFAULT_ROLE))).thenReturn("keycloak-user-id");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.register(request);

        assertEquals(Set.of(customerRole), result.getRoles());
        assertEquals("keycloak-user-id", result.getKeycloakUserId());
        verify(keycloakAuthClient).createUser("newuser", "newuser@example.com", "New Customer",
                "Password1", List.of(RoleConstant.DEFAULT_ROLE));
    }

    @Test
    void submittedAdminRoleIsIgnoredAndCannotBeGranted() throws Exception {
        String payload = """
                {"fullName":"New Customer","username":"newuser","password":"Password1",
                 "email":"newuser@example.com","gender":"unspecified","phone":"0123456789",
                 "roles":["ADMIN"]}
                """;
        RegisterRequest request = new ObjectMapper().readValue(payload, RegisterRequest.class);
        Role customerRole = new Role(1L, RoleName.USER);
        when(roleService.findByName(RoleName.USER)).thenReturn(Optional.of(customerRole));
        when(keycloakAuthClient.createUser(anyString(), anyString(), anyString(), anyString(), anyList()))
                .thenReturn("keycloak-user-id");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.register(request);

        assertEquals(Set.of(customerRole), result.getRoles());
        verify(keycloakAuthClient).createUser("newuser", "newuser@example.com", "New Customer",
                "Password1", List.of(RoleConstant.DEFAULT_ROLE));
    }

    @Test
    void registerShouldPreserveExistingFieldValidation() {
        RegisterRequest request = validRegistration();
        request.setUsername("short");
        request.setPassword("weak");
        request.setEmail("not-an-email");
        request.setPhone("123");

        Set<jakarta.validation.ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("username")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("password")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("phone")));
    }

    @Test
    void registerShouldCompensateKeycloakWhenLocalPersistenceFails() {
        RegisterRequest request = validRegistration();
        Role customerRole = new Role(1L, RoleName.USER);
        when(roleService.findByName(RoleName.USER)).thenReturn(Optional.of(customerRole));
        when(keycloakAuthClient.createUser(anyString(), anyString(), anyString(), anyString(), anyList()))
                .thenReturn("keycloak-user-id");
        when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("database unavailable"));

        assertThrows(RuntimeException.class, () -> userService.register(request));

        verify(keycloakAuthClient).deleteUser("keycloak-user-id");
    }

    @Test
    void registerShouldKeepOriginalFailureWhenKeycloakCompensationFails() {
        RegisterRequest request = validRegistration();
        Role customerRole = new Role(1L, RoleName.USER);
        when(roleService.findByName(RoleName.USER)).thenReturn(Optional.of(customerRole));
        when(keycloakAuthClient.createUser(anyString(), anyString(), anyString(), anyString(), anyList()))
                .thenReturn("keycloak-user-id");
        RuntimeException persistenceFailure = new RuntimeException("database unavailable");
        when(userRepository.save(any(User.class))).thenThrow(persistenceFailure);
        doThrow(new RuntimeException("Keycloak unavailable")).when(keycloakAuthClient).deleteUser("keycloak-user-id");

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> userService.register(request));

        assertSame(persistenceFailure, thrown);
        assertEquals(1, thrown.getSuppressed().length);
    }

    @Test
    void registerShouldPreserveEmailAndPhoneDuplicateChecks() {
        RegisterRequest request = validRegistration();
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThrows(BusinessException.class, () -> userService.register(request));
        verify(keycloakAuthClient, never()).createUser(anyString(), anyString(), anyString(), anyString(), anyList());

        reset(userRepository, keycloakAuthClient);
        when(userRepository.existsByPhone(request.getPhone())).thenReturn(true);
        assertThrows(BusinessException.class, () -> userService.register(request));
        verify(keycloakAuthClient, never()).createUser(anyString(), anyString(), anyString(), anyString(), anyList());
    }

    private static RegisterRequest validRegistration() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("New Customer");
        request.setUsername("newuser");
        request.setPassword("Password1");
        request.setEmail("newuser@example.com");
        request.setGender("unspecified");
        request.setPhone("0123456789");
        return request;
    }
}
