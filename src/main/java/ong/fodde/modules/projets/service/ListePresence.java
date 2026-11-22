package ong.fodde.modules.projets.service;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import ong.fodde.shared.exception.BusinessException;

/**
 * Comptage d'une liste de présence au format CSV, dont l'animateur choisit lui-même les colonnes.
 * La première ligne remplie est l'en-tête ; chaque ligne remplie ensuite est un participant.
 * Les femmes sont comptées si une colonne s'intitule « Sexe » ou « Genre ».
 */
public final class ListePresence {

    /** Nombre de participants ; femmes à null quand la liste n'a pas de colonne Sexe ou Genre. */
    public record Comptage(int participants, Integer femmes) {
    }

    private static final Set<String> FEMMES = Set.of("f", "femme", "feminin", "fille");

    private ListePresence() {
    }

    public static Comptage compter(byte[] contenu) {
        List<String> lignes = lignes(texte(contenu));
        List<List<String>> remplies = new ArrayList<>();
        char separateur = 0;
        for (String ligne : lignes) {
            if (separateur == 0 && !ligne.isBlank()) {
                separateur = separateur(ligne);
            }
            if (separateur != 0) {
                List<String> cellules = cellules(ligne, separateur);
                if (cellules.stream().anyMatch(c -> !c.isBlank())) {
                    remplies.add(cellules);
                }
            }
        }
        if (remplies.size() < 2) {
            throw new BusinessException("La liste de présence ne contient aucun participant");
        }
        List<String> entete = remplies.get(0);
        int colonneSexe = -1;
        for (int i = 0; i < entete.size(); i++) {
            String titre = normaliser(entete.get(i));
            if (titre.contains("sexe") || titre.contains("genre")) {
                colonneSexe = i;
                break;
            }
        }
        int participants = remplies.size() - 1;
        if (colonneSexe < 0) {
            return new Comptage(participants, null);
        }
        int femmes = 0;
        for (List<String> ligne : remplies.subList(1, remplies.size())) {
            if (colonneSexe < ligne.size() && FEMMES.contains(normaliser(ligne.get(colonneSexe)))) {
                femmes++;
            }
        }
        return new Comptage(participants, femmes);
    }

    /** UTF-8 (avec ou sans BOM), sinon Windows-1252, l'encodage d'Excel en français. */
    private static String texte(byte[] contenu) {
        try {
            String s = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(contenu)).toString();
            return s.startsWith("﻿") ? s.substring(1) : s;
        } catch (CharacterCodingException e) {
            return new String(contenu, Charset.forName("windows-1252"));
        }
    }

    private static List<String> lignes(String texte) {
        return List.of(texte.split("\\r\\n|\\r|\\n", -1));
    }

    private static char separateur(String entete) {
        char meilleur = ',';
        long max = -1;
        for (char c : new char[] {';', ',', '\t'}) {
            long n = entete.chars().filter(x -> x == c).count();
            if (n > max) {
                max = n;
                meilleur = c;
            }
        }
        return meilleur;
    }

    /** Découpe une ligne en respectant les guillemets. */
    private static List<String> cellules(String ligne, char separateur) {
        List<String> cellules = new ArrayList<>();
        StringBuilder courante = new StringBuilder();
        boolean entreGuillemets = false;
        for (int i = 0; i < ligne.length(); i++) {
            char c = ligne.charAt(i);
            if (c == '"') {
                if (entreGuillemets && i + 1 < ligne.length() && ligne.charAt(i + 1) == '"') {
                    courante.append('"');
                    i++;
                } else {
                    entreGuillemets = !entreGuillemets;
                }
            } else if (c == separateur && !entreGuillemets) {
                cellules.add(courante.toString().trim());
                courante.setLength(0);
            } else {
                courante.append(c);
            }
        }
        cellules.add(courante.toString().trim());
        return cellules;
    }

    private static String normaliser(String s) {
        return Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}
