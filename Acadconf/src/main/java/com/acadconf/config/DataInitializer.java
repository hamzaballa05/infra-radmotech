package com.acadconf.config;

import com.acadconf.model.*;
import com.acadconf.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Component
@Profile("dev")
public class DataInitializer implements CommandLineRunner {

    @PersistenceContext
    private EntityManager em;

    private final UtilisateurRepository utilisateurRepo;
    private final ConferenceRepository conferenceRepo;
    private final ArticleRepository articleRepo;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UtilisateurRepository utilisateurRepo,
                           ConferenceRepository conferenceRepo,
                           ArticleRepository articleRepo,
                           PasswordEncoder passwordEncoder) {
        this.utilisateurRepo = utilisateurRepo;
        this.conferenceRepo = conferenceRepo;
        this.articleRepo = articleRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (utilisateurRepo.count() > 0) {
            return;
        }
        clearAll();
        seed();
    }

    private void clearAll() {
        em.createNativeQuery("DROP TABLE IF EXISTS notification").executeUpdate();
        em.createNativeQuery("DROP TABLE IF EXISTS decision").executeUpdate();
        em.createNativeQuery("DROP TABLE IF EXISTS evaluation").executeUpdate();
        em.createNativeQuery("DROP TABLE IF EXISTS affectation").executeUpdate();
        em.createQuery("DELETE FROM Article a").executeUpdate();
        em.createQuery("DELETE FROM Conference c").executeUpdate();
        em.createQuery("DELETE FROM Utilisateur u").executeUpdate();
    }

    private void seed() {

        user("admin@acadconf.ma",     "Système",   "Admin",      Utilisateur.Role.ADMIN,     "INSEA Rabat");
        Utilisateur guerraoui = user("president@acadconf.ma", "Rachid",    "Guerraoui",  Utilisateur.Role.PRESIDENT, "EPFL Lausanne");
        Utilisateur vaswani   = user("auteur1@acadconf.ma",   "Ashish",    "Vaswani",    Utilisateur.Role.AUTEUR,    "Google Brain");
        Utilisateur devlin    = user("auteur2@acadconf.ma",   "Jacob",     "Devlin",     Utilisateur.Role.AUTEUR,    "Google AI Language");
        Utilisateur radford   = user("auteur3@acadconf.ma",   "Alec",      "Radford",    Utilisateur.Role.AUTEUR,    "OpenAI");
        Utilisateur hu        = user("auteur4@acadconf.ma",   "Edward",    "Hu",         Utilisateur.Role.AUTEUR,    "Microsoft Research");

        Conference neurips = conf(guerraoui,
            "Neural Information Processing Systems 2025",
            "NeurIPS-2025",
            "Premier forum mondial sur l'apprentissage automatique, les réseaux de neurones et l'intelligence artificielle profonde.",
            "Machine Learning, Deep Learning, Réseaux de neurones, Optimisation, IA",
            LocalDate.of(2025, 7, 1),
            LocalDate.of(2025, 9, 15),
            LocalDate.of(2025, 11, 10),
            LocalDate.of(2025, 11, 14),
            Conference.Statut.FERMEE);

        Conference emnlp = conf(guerraoui,
            "Conference on Empirical Methods in Natural Language Processing 2026",
            "EMNLP-2026",
            "Conférence de référence sur le traitement automatique du langage naturel et les grands modèles de langage.",
            "NLP, Grands Modèles de Langage, Transformers, Alignement, Fine-tuning",
            LocalDate.of(2025, 12, 1),
            LocalDate.of(2026, 6, 30),
            LocalDate.of(2026, 8, 5),
            LocalDate.of(2026, 8, 9),
            Conference.Statut.FERMEE);

        Conference cvpr = conf(guerraoui,
            "Computer Vision and Pattern Recognition Conference 2026",
            "CVPR-2026",
            "Conférence internationale de référence en vision par ordinateur et reconnaissance de formes.",
            "Vision par ordinateur, Segmentation, Détection, Modèles fondationnels",
            LocalDate.of(2026, 7, 1),
            LocalDate.of(2026, 8, 30),
            LocalDate.of(2026, 10, 15),
            LocalDate.of(2026, 10, 19),
            Conference.Statut.OUVERTE);

        article(neurips, vaswani,
            "Attention Is All You Need",
            "We propose the Transformer, a network architecture based solely on attention mechanisms, dispensing with recurrence and convolutions entirely.",
            "Transformers, Attention, Sequence-to-Sequence, NLP",
            Article.Statut.SOUMIS);

        article(neurips, devlin,
            "BERT: Pre-training of Deep Bidirectional Transformers for Language Understanding",
            "We introduce BERT, designed to pre-train deep bidirectional representations from unlabeled text, fine-tunable for a wide range of NLP tasks.",
            "BERT, Pré-entraînement, Transformers Bidirectionnels, Fine-tuning, NLP",
            Article.Statut.SOUMIS);

        article(emnlp, radford,
            "Language Models are Unsupervised Multitask Learners",
            "Language models begin to learn NLP tasks without explicit supervision when trained on a large dataset of webpages (WebText).",
            "GPT-2, Modèles de Langage, Apprentissage Non Supervisé, Zéro-Shot",
            Article.Statut.SOUMIS);

        article(emnlp, hu,
            "LoRA: Low-Rank Adaptation of Large Language Models",
            "LoRA freezes pre-trained model weights and injects trainable rank decomposition matrices, greatly reducing trainable parameters.",
            "LoRA, Fine-tuning Efficace, LLM, Efficacité, Paramètres",
            Article.Statut.SOUMIS);

        article(cvpr, vaswani,
            "DINOv2: Learning Robust Visual Features without Supervision",
            "A new method for training high-performance visual features without supervision, providing universal features for image-level and pixel-level tasks.",
            "DINO, Vision Autosupervisée, Segmentation, Classification",
            Article.Statut.SOUMIS);

        article(cvpr, hu,
            "Segment Anything: Foundation Model for Image Segmentation",
            "We introduce SAM, trained on over 11 million images and 1.1 billion masks, with zero-shot generalization to unfamiliar objects.",
            "Segmentation, Modèle Fondationnel, SAM, Zero-Shot, Vision",
            Article.Statut.SOUMIS);
    }

    private Utilisateur user(String email, String prenom, String nom,
                              Utilisateur.Role role, String affiliation) {
        Utilisateur u = new Utilisateur();
        u.setEmail(email);
        u.setPrenom(prenom);
        u.setNom(nom);
        u.setRole(role);
        u.setAffiliation(affiliation);
        u.setMotDePasse(passwordEncoder.encode("1234"));
        return utilisateurRepo.save(u);
    }

    private Conference conf(Utilisateur president, String titre, String sigle,
                             String description, String themes,
                             LocalDate dateLimiteSoumission, LocalDate dateLimiteEvaluation,
                             LocalDate dateDebut, LocalDate dateFin,
                             Conference.Statut statut) {
        Conference c = new Conference();
        c.setTitre(titre);
        c.setSigle(sigle);
        c.setDescription(description);
        c.setThemes(themes);
        c.setDateLimiteSoumission(dateLimiteSoumission);
        c.setDateLimiteEvaluation(dateLimiteEvaluation);
        c.setDateDebut(dateDebut);
        c.setDateFin(dateFin);
        c.setStatut(statut);
        c.setPresident(president);
        return conferenceRepo.save(c);
    }

    private void article(Conference conference, Utilisateur auteur, String titre,
                          String resume, String motsCles, Article.Statut statut) {
        Article a = new Article();
        a.setTitre(titre);
        a.setResume(resume);
        a.setMotsCles(motsCles);
        a.setConference(conference);
        a.setAuteurPrincipal(auteur);
        a.setStatutArticle(statut);
        articleRepo.save(a);
    }
}
