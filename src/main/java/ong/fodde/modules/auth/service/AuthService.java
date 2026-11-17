package ong.fodde.modules.auth.service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    /** Nombre de mots de passe incorrects successifs qui verrouillent le compte. */
    public static final int TENTATIVES_MAX = 5;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JournalConnexions journal;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
                       JournalConnexions journal) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.journal = journal;
    }

    /**
     * Connexion ; chaque tentative, réussie ou refusée, est inscrite au journal des connexions.
     * Les refus ne doivent pas annuler la transaction : sans cela, le compteur de mots de passe
     * incorrects n'était jamais enregistré et le compte ne se verrouillait jamais.
     */
    @Transactional(noRollbackFor = { AuthentificationException.class, CompteVerrouilleException.class })
    public LoginResponse login(LoginRequest request, String adresseIp, String navigateur) {
        User user = userRepository.findByEmail(request.email()).orElse(null);
        if (user == null) {
            journaliser(request.email(), null, false, JournalConnexions.COMPTE_INCONNU, adresseIp, navigateur);
            throw new AuthentificationException("Email ou mot de passe incorrect");
        }

        // Vérifier si le compte est verrouillé
        if (user.getTentativesEchouees() != null && user.getTentativesEchouees() >= TENTATIVES_MAX) {
            journaliser(request.email(), user, false, JournalConnexions.COMPTE_VERROUILLE, adresseIp, navigateur);
            throw new CompteVerrouilleException("Compte verrouillé suite à trop de tentatives échouées. Contactez un administrateur.");
        }

        // Vérifier si le compte est actif
        if (user.getStatut() != StatutUser.ACTIF) {
            journaliser(request.email(), user, false, JournalConnexions.COMPTE_INACTIF, adresseIp, navigateur);
            throw new AuthentificationException("Votre compte n'est pas actif. Statut actuel : " + user.getStatut());
        }

        // Vérifier le mot de passe
        if (!passwordEncoder.matches(request.motDePasse(), user.getMotDePasseHash())) {
            // Incrémenter les tentatives échouées
            user.setTentativesEchouees(
                    (user.getTentativesEchouees() != null ? user.getTentativesEchouees() : 0) + 1
            );
            userRepository.save(user);
            journaliser(request.email(), user, false, JournalConnexions.MOT_DE_PASSE, adresseIp, navigateur);
            throw new AuthentificationException("Email ou mot de passe incorrect");
        }

        // Succès : reset tentatives et update dernière connexion
        user.setTentativesEchouees(0);
        user.setDerniereConnexion(LocalDateTime.now());
        userRepository.save(user);
        journaliser(request.email(), user, true, null, adresseIp, navigateur);

        List<String> roles = user.getRoles().stream()
                .map(Role::getNom)
                .toList();

        String token = jwtService.generateToken(user);
        return new LoginResponse(
                token,
                user.getId(),
                user.getEmail(),
                user.getNom(),
                user.getPrenom(),
                roles
        );
    }

    /** Une panne du journal ne doit jamais bloquer ni changer le résultat d'une connexion. */
    private void journaliser(String email, User user, boolean succes, String motif, String ip, String navigateur) {
        try {
            journal.enregistrer(email, user, succes, motif, ip, navigateur);
        } catch (RuntimeException e) {
            log.warn("Journal des connexions indisponible : {}", e.getMessage());
        }
    }
}
