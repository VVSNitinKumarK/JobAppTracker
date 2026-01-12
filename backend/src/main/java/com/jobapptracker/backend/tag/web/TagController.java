package com.jobapptracker.backend.tag.web;

import com.jobapptracker.backend.tag.dto.TagDto;
import com.jobapptracker.backend.tag.service.TagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
public class TagController {

    private static final Logger log = LoggerFactory.getLogger(TagController.class);

    private final TagService tagService;

    public TagController(TagService service) {
        this.tagService = service;
    }

    @GetMapping
    public ResponseEntity<List<TagDto>> listTags() {
        log.info("GET /api/tags - listing all tags");
        return ResponseEntity.ok(tagService.listAll());
    }
}
