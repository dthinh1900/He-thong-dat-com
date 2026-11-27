package com.mealbooking.backend.domain.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter @NoArgsConstructor
public class Department {
    private Long id;
    private String name;
    private String description;
    private Boolean active = true;
    private List<User> users = new ArrayList<>();
}
