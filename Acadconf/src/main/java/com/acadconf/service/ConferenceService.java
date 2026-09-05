package com.acadconf.service;

import com.acadconf.model.Conference;
import com.acadconf.model.Utilisateur;
import com.acadconf.repository.ConferenceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConferenceService {

    private final ConferenceRepository conferenceRepository;

    public ConferenceService(ConferenceRepository conferenceRepository) {
        this.conferenceRepository = conferenceRepository;
    }

    public List<Conference> findAll() {
        return conferenceRepository.findAll();
    }

    public Page<Conference> findFiltered(String search, Conference.Statut statut, int page, int size) {
        return conferenceRepository.findFiltered(
                search == null || search.isBlank() ? null : search,
                statut,
                PageRequest.of(page, size, Sort.by("id").descending())
        );
    }

    public List<Conference> findByPresident(Utilisateur president) {
        return conferenceRepository.findByPresident(president);
    }

    public List<Conference> findOuvertes() {
        return conferenceRepository.findByStatut(Conference.Statut.OUVERTE)
                .stream()
                .filter(Conference::isSubmissionOpen)
                .toList();
    }

    public Conference findById(Long id) {
        return conferenceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Conférence introuvable"));
    }

    public Conference save(Conference conference) {
        return conferenceRepository.save(conference);
    }

    public void delete(Long id) {
        conferenceRepository.deleteById(id);
    }
}
