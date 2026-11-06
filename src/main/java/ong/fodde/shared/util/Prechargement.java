package ong.fodde.shared.util;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.repository.CrudRepository;

/**
 * Évite le problème « une requête par ligne » lors de la conversion d'une liste en réponses.
 *
 * <p>Les méthodes {@code toResponse} font un {@code findById} par élément (nom de l'auteur, code projet…).
 * En appelant {@link #precharger} avant la conversion, toutes les entités référencées sont chargées
 * en UNE requête ({@code WHERE id IN (...)}). Dans la même transaction, Hibernate les garde en mémoire
 * (cache de premier niveau) : les {@code findById} suivants ne touchent plus la base.
 *
 * <p>À appeler dans une méthode {@code @Transactional}.
 */
public final class Prechargement {

    private Prechargement() {
    }

    public static <E, ID> void precharger(CrudRepository<?, ID> repository,
                                          Collection<E> elements,
                                          Function<E, ID> identifiant) {
        Set<ID> ids = elements.stream()
                .map(identifiant)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (!ids.isEmpty()) {
            repository.findAllById(ids);
        }
    }
}
