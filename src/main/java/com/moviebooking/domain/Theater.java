package com.moviebooking.domain;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A theater in a city. A theater has one or more screens, and each screen owns its own
 * seat layout, so screens are embedded here rather than modeled as a separate entity.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "theaters")
public class Theater {

    @Id
    private String id;

    private String name;

    private String city;

    @Builder.Default
    private List<Screen> screens = new ArrayList<>();
}
