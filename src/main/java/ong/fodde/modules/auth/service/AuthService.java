package ong.fodde.modules.auth.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ong.fodde.config.JwtService;
import ong.fodde.modules.auth.dto.LoginRequest;
import ong.fodde.modules.auth.dto.LoginResponse;
import ong.fodde.modules.auth.entity.Role;
import ong.fodde.modules.auth.entity.StatutUser;
import ong.fodde.modules.auth.entity.User;
import ong.fodde.modules.auth.repository.UserRepository;
import ong.fodde.shared.exception.AuthentificationException;
import ong.fodde.shared.exception.CompteVerrouilleException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new AuthentificationException("Email ou mot de passe incorrect"));
        

        // Vérifier si le compte est verrouillé
        if (user.getTentativesEchouees() != null && user.getTentativesEchouees() >= 5) {
            throw new CompteVerrouilleException("Compte verrouillé suite à trop de tentatives échouées. Contactez un administrateur.");
        }

        // Vérifier si le compte est actif
        if (user.getStatut() != StatutUser.ACTIF) {
            throw new AuthentificationException("Votre compte n'est pas actif. Statut actuel : " + user.getStatut());
        }

        // Vérifier le mot de passe
        if (!passwordEncoder.matches(request.motDePasse(), user.getMotDePasseHash())) {
            // Incrémenter les tentatives échouées
            user.setTentativesEchouees(
                    (user.getTentativesEchouees() != null ? user.getTentativesEchouees() : 0) + 1
            );
            userRepository.save(user);
            throw new AuthentificationException("Email ou mot de passe incorrect");
        }

        // Succès : reset tentatives et update dernière connexion
user.setTentativesEchouees(0);
user.setDerniereConnexion(LocalDateTime.now());
userRepository.save(user);

// ↓↓↓ REMPLACEZ TOUT CE QUI SUIT PAR CECI ↓↓↓

List<String> roles = user.getRoles().stream()
        .map(Role::getNom)
        .toList();

// DEBUG TEMPORAIRE
try {
    String token = jwtService.generateToken(user);
    System.out.println("=== TOKEN GÉNÉRÉ : " + token.substring(0, 30) + "...");
    return new LoginResponse(
            token,
            user.getId(),
            user.getEmail(),
            user.getNom(),
            user.getPrenom(),
            roles
    );
} catch (Exception e) {
    System.out.println("=== ERREUR GÉNÉRATION TOKEN : " + e.getMessage());
    throw e;
}
    }

}
