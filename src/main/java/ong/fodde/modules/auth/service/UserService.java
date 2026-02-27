package ong.fodde.modules.auth.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ong.fodde.modules.auth.dto.CreateUserRequest;
import ong.fodde.modules.auth.dto.UserListResponse;
import ong.fodde.modules.auth.entity.Role;
import ong.fodde.modules.auth.entity.StatutUser;
import ong.fodde.modules.auth.entity.User;
import ong.fodde.modules.auth.repository.RoleRepository;
import ong.fodde.modules.auth.repository.UserRepository;
import ong.fodde.shared.exception.BusinessException;
import ong.fodde.shared.exception.ResourceNotFoundException;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserListResponse> listerUtilisateurs() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public UserListResponse creerUtilisateur(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("Un utilisateur avec cet email existe déjà : " + request.email());
        }

        Set<Role> roles = new HashSet<>();
        for (String nomRole : request.roles()) {
            Role role = roleRepository.findByNom(nomRole)
                    .orElseThrow(() -> new ResourceNotFoundException("Rôle introuvable : " + nomRole));
            roles.add(role);
        }

        User user = new User();
        user.setNom(request.nom());
        user.setPrenom(request.prenom());
        user.setEmail(request.email());
        user.setMotDePasseHash(passwordEncoder.encode(request.motDePasse()));
        user.setTelephone(request.telephone());
        user.setService(request.service());
        user.setStatut(StatutUser.ACTIF);
        user.setTentativesEchouees(0);
        user.setRoles(roles);

        return toResponse(userRepository.save(user));
    }

    public UserListResponse changerStatut(UUID id, StatutUser statut) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + id));
        user.setStatut(statut);
        return toResponse(userRepository.save(user));
    }

    public UserListResponse mettreAJourRoles(UUID id, List<String> roleNoms) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + id));

        Set<Role> roles = new HashSet<>();
        for (String nomRole : roleNoms) {
            Role role = roleRepository.findByNom(nomRole)
                    .orElseThrow(() -> new ResourceNotFoundException("Rôle introuvable : " + nomRole));
            roles.add(role);
        }

        user.setRoles(roles);
        return toResponse(userRepository.save(user));
    }

    private UserListResponse toResponse(User user) {
        List<String> roles = user.getRoles().stream()
                .map(Role::getNom)
                .toList();
        return new UserListResponse(
                user.getId(),
                user.getNom(),
                user.getPrenom(),
                user.getEmail(),
                user.getTelephone(),
                user.getService(),
                user.getStatut(),
                roles,
                user.getDerniereConnexion(),
                user.getCreatedAt()
        );
    }
}
