package com.jobapptracker.backend.tag.service;

import com.jobapptracker.backend.tag.dto.TagDto;
import com.jobapptracker.backend.tag.repository.TagRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TagService {

    private final TagRepository tagRepository;

    public TagService(TagRepository repository) {
        this.tagRepository = repository;
    }

    public List<TagDto> listAll() {
        return tagRepository.listAll();
    }
}
