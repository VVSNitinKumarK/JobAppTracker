package com.jobapptracker.backend.tag.web;

import com.jobapptracker.backend.tag.dto.TagDto;
import com.jobapptracker.backend.tag.service.TagService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
public class TagController {

    private final TagService tagService;

    public TagController(TagService service) {
        this.tagService = service;
    }

    @GetMapping
    public ResponseEntity<List<TagDto>> listTags() {
        return ResponseEntity.ok(tagService.listAll());
    }
}
