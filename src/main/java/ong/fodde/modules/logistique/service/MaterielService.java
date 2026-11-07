package ong.fodde.modules.logistique.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ong.fodde.modules.acquisitions.entity.Acquisition;
import ong.fodde.modules.acquisitions.entity.StatutAcquisition;
import ong.fodde.modules.acquisitions.repository.AcquisitionRepository;
import ong.fodde.modules.auth.repository.UserRepository;
import ong.fodde.modules.logistique.dto.LogistiqueDtos.MaterielRequest;
import ong.fodde.modules.logistique.dto.LogistiqueDtos.MaterielResponse;
import ong.fodde.modules.logistique.dto.LogistiqueDtos.MouvementRequest;
import ong.fodde.modules.logistique.dto.LogistiqueDtos.MouvementResponse;
import ong.fodde.modules.logistique.entity.EtatMateriel;
import ong.fodde.modules.logistique.entity.Materiel;
import ong.fodde.modules.logistique.entity.MouvementMateriel;
import ong.fodde.modules.logistique.entity.TypeMouvement;
import ong.fodde.modules.logistique.repository.MaterielRepository;
import ong.fodde.modules.logistique.repository.MouvementMaterielRepository;
import ong.fodde.modules.projets.repository.ProjetRepository;
import ong.fodde.shared.exception.BusinessException;
import ong.fodde.shared.exception.ResourceNotFoundException;

import static ong.fodde.shared.util.Prechargement.precharger;

/**
 * Inventaire du matériel. La fiche se modifie librement (désignation, valeur…) ;
 * l'état, la personne et le projet d'affectation ne changent que par un
 * mouvement, pour que l'historique reste complet.
 */
@Service
@Transactional
public class MaterielService {

    private final MaterielRepository materielRepository;
    private final MouvementMaterielRepository mouvementRepository;
    private final AcquisitionRepository acquisitionRepository;
    private final ProjetRepository projetRepository;
    private final UserRepository userRepository;

    public MaterielService(MaterielRepository materielRepository,
                           MouvementMaterielRepository mouvementRepository,
                           AcquisitionRepository acquisitionRepository,
                           ProjetRepository projetRepository,
                           UserRepository userRepository) {
        this.materielRepository = materielRepository;
        this.mouvementRepository = mouvementRepository;
        this.acquisitionRepository = acquisitionRepository;
        this.projetRepository = projetRepository;
        this.userRepository = userRepository;
    }

    // ── Lecture ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<MaterielResponse> lister() {
        List<Materiel> materiels = materielRepository.findAllByOrderByCreatedAtDesc();
        precharger(projetRepository, materiels, Materiel::getProjetId);
        precharger(userRepository, materiels, Materiel::getResponsableId);
        return materiels.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public MaterielResponse obtenir(UUID id) {
        return toResponse(trouver(id));
    }

    @Transactional(readOnly = true)
    public List<MouvementResponse> mouvements(UUID materielId) {
        trouver(materielId);
        List<MouvementMateriel> liste = mouvementRepository.findByMaterielIdOrderByDateMouvementDescCreatedAtDesc(materielId);
        precharger(projetRepository, liste, MouvementMateriel::getProjetId);
        precharger(userRepository, liste, MouvementMateriel::getResponsableId);
        precharger(userRepository, liste, MouvementMateriel::getCreatedBy);
        return liste.stream().map(this::toResponse).toList();
    }

    // ── Création et modification ────────────────────────────────────────────

    /**
     * Entrée d'un bien dans l'inventaire, éventuellement depuis une acquisition validée
     * (une seule fois). Un premier mouvement « inventaire » ou « affectation » est enregistré.
     */
    public MaterielResponse creer(MaterielRequest req, UUID auteurId) {
        Materiel m = new Materiel();
        appliquerFiche(m, req);

        if (req.acquisitionId() != null) {
            Acquisition a = acquisitionRepository.findById(req.acquisitionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Acquisition introuvable : " + req.acquisitionId()));
            if (a.getStatut() != StatutAcquisition.VALIDEE) {
                throw new BusinessException("Seule une acquisition validée peut entrer dans l'inventaire");
            }
            materielRepository.findByAcquisitionId(a.getId()).ifPresent(deja -> {
                throw new BusinessException("Cette acquisition est déjà dans l'inventaire (" + deja.getCodeActif() + ")");
            });
            m.setAcquisitionId(a.getId());
            if (m.getProjetId() == null) {
                m.setProjetId(a.getProjetId());
            }
        }

        String code = req.codeActif() != null && !req.codeActif().isBlank() ? req.codeActif().trim() : null;
        if (code != null && materielRepository.existsByCodeActif(code)) {
            throw new BusinessException("Le code actif " + code + " est déjà utilisé");
        }
        m.setCodeActif(code != null ? code : materielRepository.prochainCodeActif());

        EtatMateriel etat = req.etat() != null ? req.etat() : EtatMateriel.BON_ETAT;
        if (etat == EtatMateriel.HORS_SERVICE) {
            throw new BusinessException("Un bien ne peut pas entrer dans l'inventaire hors service");
        }
        verifierProjet(req.projetId());
        verifierPersonne(req.responsableId());
        m.setEtat(etat);
        if (req.projetId() != null) {
            m.setProjetId(req.projetId());
        }
        m.setResponsableId(req.responsableId());
        m.setLocalisation(texte(req.localisation()));
        m.setCreatedBy(auteurId);
        Materiel saved = materielRepository.save(m);

        boolean affecte = saved.getResponsableId() != null;
        journaliser(saved, affecte ? TypeMouvement.AFFECTATION : TypeMouvement.INVENTAIRE, LocalDate.now(),
                null, etat, saved.getProjetId(), saved.getResponsableId(),
                req.acquisitionId() != null ? "Entrée dans l'inventaire depuis une acquisition validée" : "Entrée dans l'inventaire",
                auteurId);
        return toResponse(saved);
    }

    /** Modifie la fiche descriptive (pas l'état ni l'affectation : voir les mouvements). */
    public MaterielResponse modifier(UUID id, MaterielRequest req) {
        Materiel m = trouver(id);
        appliquerFiche(m, req);
        if (req.codeActif() != null && !req.codeActif().isBlank() && !req.codeActif().trim().equals(m.getCodeActif())) {
            String code = req.codeActif().trim();
            if (materielRepository.existsByCodeActif(code)) {
                throw new BusinessException("Le code actif " + code + " est déjà utilisé");
            }
            m.setCodeActif(code);
        }
        return toResponse(materielRepository.save(m));
    }

    private void appliquerFiche(Materiel m, MaterielRequest req) {
        m.setDesignation(req.designation().trim());
        m.setCategorie(req.categorie());
        m.setMarqueModele(texte(req.marqueModele()));
        m.setNumeroSerie(texte(req.numeroSerie()));
        m.setValeurAcquisition(req.valeurAcquisition());
        m.setDateAcquisition(req.dateAcquisition());
        m.setNotes(texte(req.notes()));
    }

    // ── Mouvements ──────────────────────────────────────────────────────────

    /**
     * Affectation, retour, réparation, mise hors service ou constat d'inventaire.
     * Met à jour le bien et garde la trace (état avant et après, auteur).
     */
    public MouvementResponse enregistrerMouvement(UUID materielId, MouvementRequest req, UUID auteurId) {
        Materiel m = trouver(materielId);
        EtatMateriel avant = m.getEtat();
        if (avant == EtatMateriel.HORS_SERVICE) {
            throw new BusinessException("Ce bien est hors service : aucun mouvement n'est possible");
        }
        LocalDate date = req.date() != null ? req.date() : LocalDate.now();
        if (date.isAfter(LocalDate.now())) {
            throw new BusinessException("La date du mouvement ne peut pas être dans le futur");
        }
        String notes = texte(req.notes());
        EtatMateriel apres = avant;
        UUID responsableMvt = null;
        UUID projetMvt = m.getProjetId();

        switch (req.type()) {
            case AFFECTATION -> {
                if (req.responsableId() == null && req.projetId() == null) {
                    throw new BusinessException("Indiquez la personne ou le projet à qui le bien est affecté");
                }
                if (avant == EtatMateriel.EN_REPARATION) {
                    throw new BusinessException("Ce bien est en réparation : enregistrez d'abord son retour");
                }
                verifierPersonne(req.responsableId());
                verifierProjet(req.projetId());
                m.setResponsableId(req.responsableId());
                if (req.projetId() != null) {
                    m.setProjetId(req.projetId());
                }
                if (texte(req.localisation()) != null) {
                    m.setLocalisation(texte(req.localisation()));
                }
                responsableMvt = req.responsableId();
                projetMvt = m.getProjetId();
            }
            case RETOUR -> {
                if (m.getResponsableId() == null && avant != EtatMateriel.EN_REPARATION) {
                    throw new BusinessException("Ce bien n'est ni affecté ni en réparation : rien à rendre");
                }
                apres = req.etatApres() != null ? req.etatApres() : (avant == EtatMateriel.EN_REPARATION ? null : avant);
                if (apres == null || apres == EtatMateriel.EN_REPARATION) {
                    throw new BusinessException("Indiquez l'état du bien à son retour de réparation");
                }
                if (apres == EtatMateriel.HORS_SERVICE) {
                    throw new BusinessException("Pour un bien inutilisable, enregistrez une mise hors service");
                }
                m.setResponsableId(null);
                m.setLocalisation(texte(req.localisation()) != null ? texte(req.localisation()) : "Magasin");
            }
            case REPARATION -> {
                if (avant == EtatMateriel.EN_REPARATION) {
                    throw new BusinessException("Ce bien est déjà en réparation");
                }
                apres = EtatMateriel.EN_REPARATION;
                if (texte(req.localisation()) != null) {
                    m.setLocalisation(texte(req.localisation()));
                }
            }
            case MISE_HORS_SERVICE -> {
                if (notes == null) {
                    throw new BusinessException("Indiquez la raison de la mise hors service");
                }
                apres = EtatMateriel.HORS_SERVICE;
                m.setResponsableId(null);
            }
            case INVENTAIRE -> {
                if (req.etatApres() == null) {
                    throw new BusinessException("Indiquez l'état constaté");
                }
                if (req.etatApres() == EtatMateriel.HORS_SERVICE) {
                    throw new BusinessException("Pour un bien inutilisable, enregistrez une mise hors service");
                }
                apres = req.etatApres();
                if (texte(req.localisation()) != null) {
                    m.setLocalisation(texte(req.localisation()));
                }
                responsableMvt = m.getResponsableId();
            }
        }

        m.setEtat(apres);
        materielRepository.save(m);
        MouvementMateriel mvt = journaliser(m, req.type(), date, avant, apres, projetMvt, responsableMvt, notes, auteurId);
        return toResponse(mvt);
    }

    private MouvementMateriel journaliser(Materiel m, TypeMouvement type, LocalDate date, EtatMateriel avant,
                                          EtatMateriel apres, UUID projetId, UUID responsableId, String notes,
                                          UUID auteurId) {
        MouvementMateriel mvt = new MouvementMateriel();
        mvt.setMaterielId(m.getId());
        mvt.setType(type);
        mvt.setDateMouvement(date);
        mvt.setEtatAvant(avant != null ? avant.name() : null);
        mvt.setEtatApres(apres != null ? apres.name() : null);
        mvt.setProjetId(projetId);
        mvt.setResponsableId(responsableId);
        mvt.setNotes(notes);
        mvt.setCreatedBy(auteurId);
        return mouvementRepository.save(mvt);
    }

    // ── Outils ──────────────────────────────────────────────────────────────

    private Materiel trouver(UUID id) {
        return materielRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bien introuvable : " + id));
    }

    private void verifierProjet(UUID projetId) {
        if (projetId != null && !projetRepository.existsById(projetId)) {
            throw new ResourceNotFoundException("Projet introuvable : " + projetId);
        }
    }

    private void verifierPersonne(UUID userId) {
        if (userId != null && !userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("Personne introuvable : " + userId);
        }
    }

    private static String texte(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private String nom(UUID userId) {
        return userId == null ? null
                : userRepository.findById(userId).map(u -> u.getPrenom() + " " + u.getNom()).orElse(null);
    }

    private String projetCode(UUID projetId) {
        return projetId == null ? null : projetRepository.findById(projetId).map(p -> p.getCode()).orElse(null);
    }

    private MaterielResponse toResponse(Materiel m) {
        return new MaterielResponse(m.getId(), m.getCodeActif(), m.getDesignation(), m.getCategorie(),
                m.getMarqueModele(), m.getNumeroSerie(), m.getEtat(), m.getValeurAcquisition(),
                m.getDateAcquisition(), m.getLocalisation(), m.getProjetId(), projetCode(m.getProjetId()),
                m.getResponsableId(), nom(m.getResponsableId()), m.getNotes(), m.getAcquisitionId(), m.getCreatedAt());
    }

    private MouvementResponse toResponse(MouvementMateriel x) {
        return new MouvementResponse(x.getId(), x.getType(), x.getDateMouvement(), x.getResponsableId(),
                nom(x.getResponsableId()), x.getProjetId(), projetCode(x.getProjetId()), x.getEtatAvant(),
                x.getEtatApres(), x.getNotes(), nom(x.getCreatedBy()), x.getCreatedAt());
    }
}
